package lazyHawk.security_service.servicio;

import lazyHawk.security_service.dominio.entidad.Usuario;
import lazyHawk.security_service.dominio.enumerador.RolUsuario;
import lazyHawk.security_service.dto.AuthResponseDto;
import lazyHawk.security_service.dto.LoginRequestDto;
import lazyHawk.security_service.dto.RegistroRequestDto;
import lazyHawk.security_service.repositorio.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para la lógica de negocio de AuthService.
 *
 * @author leoM2022
 * @version 1.0.0
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @InjectMocks
    private AuthService authService;

    private Usuario usuarioPrueba;
    private RegistroRequestDto registroDTO;
    private LoginRequestDto loginDTO;

    @BeforeEach
    void setUp() {
        usuarioPrueba = Usuario.builder()
                .email("test@logitrack.com")
                .password("encoded_password")
                .rol(RolUsuario.CLIENTE)
                .activo(true)
                .build();
        usuarioPrueba.setId("uuid-test-123");

        registroDTO = new RegistroRequestDto("test@logitrack.com", "password123", RolUsuario.CLIENTE);
        loginDTO = new LoginRequestDto("test@logitrack.com", "password123");
    }

    @Test
    @DisplayName("Debe registrar exitosamente un usuario si el email no existe")
    void registrar_Exitoso() {
        when(usuarioRepository.existsByEmail(registroDTO.email())).thenReturn(false);
        when(passwordEncoder.encode(registroDTO.password())).thenReturn("encoded_password");

        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioPrueba);

        var respuesta = authService.registrar(registroDTO);

        assertNotNull(respuesta.message());
        assertEquals(usuarioPrueba.getId(), respuesta.usuario().id());
        assertEquals(usuarioPrueba.getEmail(), respuesta.usuario().email());
        assertEquals(usuarioPrueba.getRol(), respuesta.usuario().rol());
        assertEquals(usuarioPrueba.isActivo(), respuesta.usuario().activo());

        verify(usuarioRepository, times(1)).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Debe permitir registrar usuarios con el rol VENDEDOR")
    void registrar_Exitoso_RolVendedor() {
        RegistroRequestDto dtoVendedor = new RegistroRequestDto("vendedor@logitrack.com", "password123", RolUsuario.VENDEDOR);
        Usuario usuarioVendedor = Usuario.builder()
                .email("vendedor@logitrack.com")
                .password("encoded_password")
                .rol(RolUsuario.VENDEDOR)
                .activo(true)
                .build();
        usuarioVendedor.setId("uuid-vendedor-456");

        when(usuarioRepository.existsByEmail("vendedor@logitrack.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("encoded_password");
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(usuarioVendedor);

        var respuesta = authService.registrar(dtoVendedor);

        assertNotNull(respuesta);
        assertEquals(RolUsuario.VENDEDOR, respuesta.usuario().rol());
        assertEquals("vendedor@logitrack.com", respuesta.usuario().email());
    }

    @Test
    @DisplayName("Debe lanzar excepción al registrar si el email ya existe")
    void registrar_Falla_EmailDuplicado() {
        when(usuarioRepository.existsByEmail(registroDTO.email())).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> authService.registrar(registroDTO));
        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    @DisplayName("Debe generar JWT y devolver perfil tras login exitoso")
    void login_Exitoso() {
        when(usuarioRepository.findByEmail(loginDTO.email())).thenReturn(Optional.of(usuarioPrueba));
        when(jwtService.generateToken(usuarioPrueba)).thenReturn("jwt-token-generado");

        AuthResponseDto respuesta = authService.login(loginDTO);

        assertNotNull(respuesta);
        assertEquals("jwt-token-generado", respuesta.token());
        assertEquals("test@logitrack.com", respuesta.email());
        assertEquals(RolUsuario.CLIENTE, respuesta.rol());

        verify(authenticationManager, times(1))
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    @DisplayName("Resource Ownership: Mismo usuario puede consultar su propio perfil")
    void obtenerPerfilPorId_Exitoso_MismoUsuario() {
        when(usuarioRepository.findById("uuid-test-123")).thenReturn(Optional.of(usuarioPrueba));

        var perfil = authService.obtenerPerfilPorId("uuid-test-123", null, "uuid-test-123", "CLIENTE");

        assertNotNull(perfil);
        assertEquals("uuid-test-123", perfil.id());
        assertEquals("test@logitrack.com", perfil.email());
    }

    @Test
    @DisplayName("Resource Ownership: ADMIN puede consultar el perfil de cualquier usuario")
    void obtenerPerfilPorId_Exitoso_AdminCualquierUsuario() {
        when(usuarioRepository.findById("uuid-test-123")).thenReturn(Optional.of(usuarioPrueba));

        var perfil = authService.obtenerPerfilPorId("uuid-test-123", null, "admin@logitrack.com", "ROLE_ADMIN");

        assertNotNull(perfil);
        assertEquals("uuid-test-123", perfil.id());
    }

    @Test
    @DisplayName("Resource Ownership: CLIENTE consultando perfil ajeno debe ser rechazado con AccessDeniedException (403)")
    void obtenerPerfilPorId_Falla_ForbiddenUsuarioAjeno() {
        when(usuarioRepository.findById("uuid-test-123")).thenReturn(Optional.of(usuarioPrueba));

        AccessDeniedException ex = assertThrows(AccessDeniedException.class, () ->
                authService.obtenerPerfilPorId("uuid-test-123", null, "otro-usuario-uuid", "CLIENTE")
        );

        assertEquals("Acceso denegado: No posee pertenencia sobre este recurso", ex.getMessage());
    }
}
