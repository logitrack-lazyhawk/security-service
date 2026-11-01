package lazyHawk.security_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * DTO para la petición de inicio de sesión (Login).
 * @author leoM2022
 * @version 1.0.0
 */
public record LoginRequestDto(
        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email.invalid}")
        String email,

        @NotBlank(message = "{validation.password.required}")
        String password
) {}