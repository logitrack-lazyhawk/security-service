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
public record RegistroRequestDTO(
        @NotBlank(message = "El email es obligatorio")
        @Email(message = "El formato del email es inválido")
        String email,

        @NotBlank(message = "La contraseña es obligatoria")
        String password,

        @NotNull(message = "El rol es obligatorio")
        RolUsuario rol
) {}