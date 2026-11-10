package lazyHawk.security_service.controlador;

import jakarta.validation.Valid;
import lazyHawk.security_service.dto.*;
import lazyHawk.security_service.mensaje.Mensajes;
import lazyHawk.security_service.servicio.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controlador REST para exponer los endpoints de registro, autenticación y consulta de perfil.
 * Implementa validaciones RBAC y Resource Ownership para seguridad de acceso por rol y usuario.
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

    /**
     * Endpoint privado para obtener el perfil de un usuario por su ID o email.
     * Valida pertenencia de recurso (Resource Ownership):
     * - ROLE_ADMIN puede consultar cualquier perfil.
     * - Otros roles (CLIENTE, VENDEDOR, REPARTIDOR) solo pueden consultar si son los dueños del recurso.
     *
     * @param id Identificador UUID o email del usuario solicitado.
     * @param authentication Contexto de seguridad.
     * @param xAuthUser Cabecera downstream opcional inyectada por API Gateway.
     * @param xAuthRoles Cabecera downstream opcional inyectada por API Gateway.
     * @return ResponseEntity con código HTTP 200 (OK) y el perfil, o 403 Forbidden si no posee pertenencia.
     */
    @GetMapping("/usuarios/{id}")
    public ResponseEntity<PerfilResponseDto> obtenerUsuarioPorId(
            @PathVariable String id,
            Authentication authentication,
            @RequestHeader(value = "X-Auth-User", required = false) String xAuthUser,
            @RequestHeader(value = "X-Auth-Roles", required = false) String xAuthRoles) {
        UsuarioPerfilDto perfil = authService.obtenerPerfilPorId(id, authentication, xAuthUser, xAuthRoles);
        return ResponseEntity.ok(new PerfilResponseDto(Mensajes.obtener("auth.token.success"), perfil));
    }
}
