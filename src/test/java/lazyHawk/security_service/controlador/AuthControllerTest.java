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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void registroConRolVendedorRetorna201Created() throws Exception {
        when(authService.registrar(any())).thenReturn(new RegistroResponseDto(
                Mensajes.obtener("auth.registration.success"),
                new UsuarioPerfilDto("vendedor-id", "vendedor@example.com", RolUsuario.VENDEDOR, true)));

        mvc.perform(post("/api/auth/registro").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"vendedor@example.com","password":"password123","rol":"VENDEDOR"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value(Mensajes.obtener("auth.registration.success")))
                .andExpect(jsonPath("$.usuario.id").value("vendedor-id"))
                .andExpect(jsonPath("$.usuario.rol").value("VENDEDOR"));
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

    @Test
    void obtenerUsuarioPorId_AdminRetorna200Ok() throws Exception {
        when(authService.obtenerPerfilPorId(eq("user-123"), any(), eq("admin@logitrack.com"), eq("ROLE_ADMIN")))
                .thenReturn(new UsuarioPerfilDto("user-123", "cliente@logitrack.com", RolUsuario.CLIENTE, true));

        mvc.perform(get("/api/auth/usuarios/user-123")
                        .header("X-Auth-User", "admin@logitrack.com")
                        .header("X-Auth-Roles", "ROLE_ADMIN"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.id").value("user-123"))
                .andExpect(jsonPath("$.usuario.email").value("cliente@logitrack.com"));
    }

    @Test
    void obtenerUsuarioPorId_ClientePropioRetorna200Ok() throws Exception {
        when(authService.obtenerPerfilPorId(eq("user-123"), any(), eq("user-123"), eq("ROLE_CLIENTE")))
                .thenReturn(new UsuarioPerfilDto("user-123", "cliente@logitrack.com", RolUsuario.CLIENTE, true));

        mvc.perform(get("/api/auth/usuarios/user-123")
                        .header("X-Auth-User", "user-123")
                        .header("X-Auth-Roles", "ROLE_CLIENTE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.usuario.id").value("user-123"));
    }

    @Test
    void obtenerUsuarioPorId_ClienteAjenoRetorna403Forbidden() throws Exception {
        when(authService.obtenerPerfilPorId(eq("user-otro"), any(), eq("user-123"), eq("ROLE_CLIENTE")))
                .thenThrow(new AccessDeniedException("Acceso denegado: No posee pertenencia sobre este recurso"));

        mvc.perform(get("/api/auth/usuarios/user-otro")
                        .header("X-Auth-User", "user-123")
                        .header("X-Auth-Roles", "ROLE_CLIENTE"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.error").value("Forbidden"))
                .andExpect(jsonPath("$.message").value("Acceso denegado: No posee pertenencia sobre este recurso"));
    }
}
