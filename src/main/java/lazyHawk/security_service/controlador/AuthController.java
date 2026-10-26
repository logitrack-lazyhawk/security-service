package lazyHawk.security_service.controlador;

import lazyHawk.security_service.dto.AuthResponseDTO;
import lazyHawk.security_service.dto.LoginRequestDTO;
import lazyHawk.security_service.dto.RegistroRequestDTO;
import lazyHawk.security_service.dto.UsuarioPerfilDTO;
import lazyHawk.security_service.servicio.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST para exponer los endpoints de registro, autenticación y consulta de perfil.
 *
 * @author leoM2022
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Endpoint público para registrar un nuevo usuario en el sistema.
     *
     * @param request Datos del usuario a registrar, validados mediante anotaciones.
     * @return ResponseEntity con código HTTP 201 (Created) si el registro fue exitoso.
     */
    @PostMapping("/registro")
    public ResponseEntity<Void> registrar(@Valid @RequestBody RegistroRequestDTO request) {
        authService.registrar(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    /**
     * Endpoint público para iniciar sesión en el sistema.
     *
     * @param request Datos de credenciales del usuario, validados mediante anotaciones.
     * @return ResponseEntity con código HTTP 200 (OK) y el token JWT en el cuerpo de la respuesta.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO request) {
        AuthResponseDTO response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint privado para obtener los datos del usuario autenticado.
     *
     * @param authentication Contexto de seguridad inyectado automáticamente por Spring Security.
     * @return ResponseEntity con código HTTP 200 (OK) y el perfil del usuario.
     */
    @GetMapping("/me")
    public ResponseEntity<UsuarioPerfilDTO> obtenerPerfil(Authentication authentication) {
        UsuarioPerfilDTO perfil = authService.obtenerPerfil(authentication.getName());
        return ResponseEntity.ok(perfil);
    }
}