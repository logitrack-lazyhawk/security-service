package lazyHawk.security_service.servicio;

import lazyHawk.security_service.dominio.entidad.Usuario;
import lazyHawk.security_service.dto.AuthResponseDTO;
import lazyHawk.security_service.dto.LoginRequestDTO;
import lazyHawk.security_service.dto.RegistroRequestDTO;
import lazyHawk.security_service.dto.UsuarioPerfilDTO;
import lazyHawk.security_service.repositorio.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio encargado de la lógica de negocio para la autenticación y registro de usuarios.
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
    public void registrar(RegistroRequestDTO request) {
        if (usuarioRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("El email ya se encuentra registrado en el sistema.");
        }

        Usuario nuevoUsuario = Usuario.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .rol(request.rol())
                .activo(true)
                .build();

        usuarioRepository.save(nuevoUsuario);
    }

    /**
     * Autentica las credenciales de un usuario y genera su token JWT de acceso.
     *
     * @param request DTO con las credenciales de inicio de sesión.
     * @return AuthResponseDTO que contiene el token JWT generado y los datos básicos.
     */
    @Transactional(readOnly = true)
    public AuthResponseDTO login(LoginRequestDTO request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.email())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado en los registros."));

        String token = jwtService.generateToken(usuario);

        return new AuthResponseDTO(token, usuario.getEmail(), usuario.getRol());
    }

    /**
     * Obtiene el perfil público del usuario que se encuentra actualmente autenticado en el contexto.
     *
     * @param email Correo electrónico extraído del token JWT.
     * @return UsuarioPerfilDTO con los datos de solo lectura del usuario.
     */
    @Transactional(readOnly = true)
    public UsuarioPerfilDTO obtenerPerfil(String email) {
        Usuario usuario = usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado en los registros."));

        return new UsuarioPerfilDTO(usuario.getId(), usuario.getEmail(), usuario.getRol(), usuario.isActivo());
    }
}