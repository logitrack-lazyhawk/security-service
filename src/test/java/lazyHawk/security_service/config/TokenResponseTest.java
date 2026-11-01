package lazyHawk.security_service.config;

import lazyHawk.security_service.dominio.entidad.Usuario;
import lazyHawk.security_service.dominio.enumerador.RolUsuario;
import lazyHawk.security_service.repositorio.UsuarioRepository;
import lazyHawk.security_service.servicio.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@WebAppConfiguration
class TokenResponseTest {
    @Autowired WebApplicationContext context;
    @Autowired UsuarioRepository usuarios;
    @Autowired JwtService jwt;
    MockMvc mvc;

    @BeforeEach
    void configurar() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void sinTokenDevuelveMensaje() throws Exception {
        mvc.perform(get("/api/auth/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Debe ingresar un token de autenticación."))
                .andExpect(jsonPath("$.path").value("/api/auth/me"));
    }

    @Test
    void tokenIncorrectoDevuelveMensaje() throws Exception {
        for (String header : new String[]{"Bearer incorrecto", "Basic incorrecto", "Bearer "}) {
            mvc.perform(get("/api/auth/me").header("Authorization", header))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.message").isNotEmpty());
        }
    }

    @Test
    void tokenCorrectoDevuelveMensajeYPerfil() throws Exception {
        Usuario usuario = usuarios.save(Usuario.builder().email("token-test@example.com")
                .password("unused").rol(RolUsuario.CLIENTE).activo(true).build());
        try {
            mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + jwt.generateToken(usuario)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.message").value("Token válido. Usuario autenticado correctamente."))
                    .andExpect(jsonPath("$.usuario.email").value(usuario.getEmail()))
                    .andExpect(jsonPath("$.usuario.password").doesNotExist());
        } finally {
            usuarios.delete(usuario);
        }
    }
}
