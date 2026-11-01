package lazyHawk.security_service.controlador;

import lazyHawk.security_service.dto.AuthResponseDto;
import lazyHawk.security_service.dto.PerfilResponseDto;
import lazyHawk.security_service.mensaje.Mensajes;
import lazyHawk.security_service.dto.RegistroResponseDto;
import lazyHawk.security_service.dto.LoginRequestDto;
import lazyHawk.security_service.dto.RegistroRequestDto;
import lazyHawk.security_service.dto.UsuarioPerfilDto;
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
    public ResponseEntity<RegistroResponseDto> registrar(@Valid @RequestBody RegistroRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request));
    }

    /**
     * Endpoint público para iniciar sesión en el sistema.
     *
     * @param request Datos de credenciales del usuario, validados mediante anotaciones.
     * @return ResponseEntity con código HTTP 200 (OK) y el token JWT en el cuerpo de la respuesta.
     */
    @PostMapping("/login")
    public ResponseEntity<AuthResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        AuthResponseDto response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint privado para obtener los datos del usuario autenticado.
     *
     * @param authentication Contexto de seguridad inyectado automáticamente por Spring Security.
     * @return ResponseEntity con código HTTP 200 (OK) y el perfil del usuario.
     */
    @GetMapping("/me")
    public ResponseEntity<PerfilResponseDto> obtenerPerfil(Authentication authentication) {
        UsuarioPerfilDto perfil = authService.obtenerPerfil(authentication.getName());
        return ResponseEntity.ok(new PerfilResponseDto(Mensajes.obtener("auth.token.success"), perfil));
    }
}