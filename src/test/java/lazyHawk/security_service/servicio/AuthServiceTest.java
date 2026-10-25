package lazyHawk.security_service.servicio;

import lazyHawk.security_service.dominio.entidad.Usuario;
import lazyHawk.security_service.dominio.enumerador.RolUsuario;
import lazyHawk.security_service.dto.AuthResponseDTO;
import lazyHawk.security_service.dto.LoginRequestDTO;
import lazyHawk.security_service.dto.RegistroRequestDTO;
import lazyHawk.security_service.repositorio.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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
    private RegistroRequestDTO registroDTO;
    private LoginRequestDTO loginDTO;

    @BeforeEach
    void setUp() {
        usuarioPrueba = Usuario.builder()
                .email("test@logitrack.com")
                .password("encoded_password")
                .rol(RolUsuario.CLIENTE)
                .activo(true)
                .build();
        usuarioPrueba.setId("uuid-test-123");

        registroDTO = new RegistroRequestDTO("test@logitrack.com", "password123", RolUsuario.CLIENTE);
        loginDTO = new LoginRequestDTO("test@logitrack.com", "password123");
    }

    @Test
    @DisplayName("Debe registrar exitosamente un usuario si el email no existe")
    void registrar_Exitoso() {
        when(usuarioRepository.existsByEmail(registroDTO.email())).thenReturn(false);
        when(passwordEncoder.encode(registroDTO.password())).thenReturn("encoded_password");

        authService.registrar(registroDTO);

        verify(usuarioRepository, times(1)).save(any(Usuario.class));
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

        AuthResponseDTO respuesta = authService.login(loginDTO);

        assertNotNull(respuesta);
        assertEquals("jwt-token-generado", respuesta.token());
        assertEquals("test@logitrack.com", respuesta.email());
        assertEquals(RolUsuario.CLIENTE, respuesta.rol());

        verify(authenticationManager, times(1))
                .authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}