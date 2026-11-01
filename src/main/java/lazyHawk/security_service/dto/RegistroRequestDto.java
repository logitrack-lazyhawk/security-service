package lazyHawk.security_service.dto;

import lazyHawk.security_service.dominio.enumerador.RolUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO para la petición de registro de un nuevo usuario.
 * @author leoM2022
 * @version 1.0.0
 */
public record RegistroRequestDto(
        @NotBlank(message = "{validation.email.required}")
        @Email(message = "{validation.email.invalid}")
        String email,

        @NotBlank(message = "{validation.password.required}")
        String password,

        @NotNull(message = "{validation.role.required}")
        RolUsuario rol
) {}