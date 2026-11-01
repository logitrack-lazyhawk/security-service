package lazyHawk.security_service.controlador;

import lazyHawk.security_service.dominio.enumerador.RolUsuario;
import lazyHawk.security_service.dto.RegistroResponseDto;
import lazyHawk.security_service.dto.UsuarioPerfilDto;
import lazyHawk.security_service.exception.GlobalExceptionHandler;
import lazyHawk.security_service.mensaje.Mensajes;
import lazyHawk.security_service.servicio.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest {

    private AuthService authService;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        authService = mock(AuthService.class);
        mvc = MockMvcBuilders.standaloneSetup(new AuthController(authService))
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void registroDevuelveConfirmacionYPerfilPublico() throws Exception {
        when(authService.registrar(any())).thenReturn(new RegistroResponseDto(
                Mensajes.obtener("auth.registration.success"),
                new UsuarioPerfilDto("usuario-id", "test@example.com", RolUsuario.CLIENTE, true)));

        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"test@example.com","password":"password123","rol":"CLIENTE"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value(Mensajes.obtener("auth.registration.success")))
                .andExpect(jsonPath("$.usuario.id").value("usuario-id"))
                .andExpect(jsonPath("$.usuario.email").value("test@example.com"))
                .andExpect(jsonPath("$.usuario.password").doesNotExist());
    }

    @Test
    void validacionDevuelveTodosLosCamposYContexto() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.timesTamp").doesNotExist())
                .andExpect(jsonPath("$.path").value("/api/auth/registro"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors.email").value(Mensajes.obtener("validation.email.required")))
                .andExpect(jsonPath("$.fieldErrors.password").value(Mensajes.obtener("validation.password.required")))
                .andExpect(jsonPath("$.fieldErrors.rol").value(Mensajes.obtener("validation.role.required")));
        verifyNoInteractions(authService);
    }

    @Test
    void credencialesInvalidasDevuelvenMensajeYContexto() throws Exception {
        when(authService.login(any())).thenThrow(new BadCredentialsException("internal"));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"test@example.com","password":"incorrecta"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value(Mensajes.obtener("auth.credentials.invalid")))
                .andExpect(jsonPath("$.path").value("/api/auth/login"))
                .andExpect(jsonPath("$.fieldErrors").isMap());
    }

    @Test
    void rolInvalidoDevuelveBadRequest() throws Exception {
        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"test@example.com","password":"password123","rol":"INVALIDO"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(Mensajes.obtener("error.request.invalid")))
                .andExpect(jsonPath("$.path").value("/api/auth/registro"));
        verifyNoInteractions(authService);
    }
}
