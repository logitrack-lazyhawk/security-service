package lazyHawk.security_service.servicio;

import lazyHawk.security_service.dominio.entidad.Usuario;
import lazyHawk.security_service.dto.AuthResponseDto;
import lazyHawk.security_service.dto.RegistroResponseDto;
import lazyHawk.security_service.mensaje.Mensajes;
import lazyHawk.security_service.dto.LoginRequestDto;
import lazyHawk.security_service.dto.RegistroRequestDto;
import lazyHawk.security_service.dto.UsuarioPerfilDto;
import lazyHawk.security_service.repositorio.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

/**
 * Servicio encargado de la lógica de negocio para la autenticación, registro y control de acceso de usuarios.
 *
 * @author leoM2022
 * @version 1.0.0
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    /**
     * Registra un nuevo usuario en la base de datos validando que el email no exista previamente.
     *
     * @param request DTO con los datos requeridos para el registro.
     * @throws IllegalArgumentException si el correo electrónico ya se encuentra registrado.
     */
    @Transactional
    public RegistroResponseDto registrar(RegistroRequestDto request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException(Mensajes.obtener("auth.email.duplicate"));
        }

        Usuario nuevoUsuario = Usuario.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .rol(request.rol())
                .activo(true)
                .build();

        Usuario usuario = usuarioRepository.save(nuevoUsuario);
        return new RegistroResponseDto(Mensajes.obtener("auth.registration.success"),
                new UsuarioPerfilDto(usuario.getId(), usuario.getEmail(), usuario.getRol(), usuario.isActivo()));
    }

    /**
     * Autentica las credenciales de un usuario y genera su token JWT de acceso.
     *
     * @param request DTO con las credenciales de inicio de sesión.
     * @return AuthResponseDto que contiene el token JWT generado y los datos básicos.
     */
    @Transactional(readOnly = true)
    public AuthResponseDto login(LoginRequestDto request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException(Mensajes.obtener("auth.user.not-found")));

        String token = jwtService.generateToken(usuario);

        return new AuthResponseDto(token, usuario.getEmail(), usuario.getRol());
    }

    /**
     * Obtiene el perfil público del usuario que se encuentra actualmente autenticado en el contexto.
     *
     * @param email Correo electrónico extraído del token JWT o cabecera de autenticación.
     * @return UsuarioPerfilDto con los datos de solo lectura del usuario.
     */
    @Transactional(readOnly = true)
    public UsuarioPerfilDto obtenerPerfil(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(Mensajes.obtener("auth.user.not-found")));

        return new UsuarioPerfilDto(usuario.getId(), usuario.getEmail(), usuario.getRol(), usuario.isActivo());
    }

    /**
     * Obtiene el perfil de un usuario aplicando control de acceso y verificación de pertenencia (Resource Ownership).
     * - Si el solicitante posee rol ADMIN, puede consultar cualquier perfil.
     * - Si posee cualquier otro rol (CLIENTE, VENDEDOR, REPARTIDOR), únicamente puede consultar si el recurso
     *   solicitado coincide con su propia identidad.
     *
     * @param id Identificador UUID o email del usuario solicitado.
     * @param authentication Contexto de autenticación de Spring Security.
     * @param xAuthUser Identificador propagado por el API Gateway (opcional).
     * @param xAuthRoles Roles propagados por el API Gateway (opcional).
     * @return UsuarioPerfilDto si el acceso es autorizado.
     * @throws AccessDeniedException si el solicitante no es ADMIN ni dueño del recurso.
     */
    @Transactional(readOnly = true)
    public UsuarioPerfilDto obtenerPerfilPorId(String id, Authentication authentication, String xAuthUser, String xAuthRoles) {
        Usuario usuario = usuarioRepository.findById(id)
                .or(() -> usuarioRepository.findByEmail(id))
                .orElseThrow(() -> new IllegalArgumentException(Mensajes.obtener("auth.user.not-found")));

        boolean isAdmin = false;
        if (authentication != null && authentication.getAuthorities() != null) {
            isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equalsIgnoreCase("ROLE_ADMIN") || a.getAuthority().equalsIgnoreCase("ADMIN"));
        }
        if (!isAdmin && xAuthRoles != null) {
            isAdmin = Arrays.stream(xAuthRoles.split(","))
                    .anyMatch(r -> r.trim().equalsIgnoreCase("ROLE_ADMIN") || r.trim().equalsIgnoreCase("ADMIN"));
        }

        if (isAdmin) {
            return new UsuarioPerfilDto(usuario.getId(), usuario.getEmail(), usuario.getRol(), usuario.isActivo());
        }

        String callerIdentity = null;
        if (xAuthUser != null && !xAuthUser.isBlank()) {
            callerIdentity = xAuthUser.trim();
        } else if (authentication != null && authentication.getName() != null) {
            callerIdentity = authentication.getName().trim();
        }

        boolean isOwner = callerIdentity != null && (
                callerIdentity.equalsIgnoreCase(usuario.getId()) ||
                callerIdentity.equalsIgnoreCase(usuario.getEmail())
        );

        if (!isOwner) {
            throw new AccessDeniedException(Mensajes.obtener("auth.resource.forbidden"));
        }

        return new UsuarioPerfilDto(usuario.getId(), usuario.getEmail(), usuario.getRol(), usuario.isActivo());
    }
}
