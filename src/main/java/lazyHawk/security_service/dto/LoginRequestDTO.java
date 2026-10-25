package lazyHawk.security_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO para la petición de inicio de sesión (Login).
 * @author leoM2022
 * @version 1.0.0
 */
public record LoginRequestDTO(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El formato del email es inválido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        String password
) {}