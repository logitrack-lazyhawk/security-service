package lazyHawk.security_service.dto;

import lazyHawk.security_service.dominio.enumerador.RolUsuario;

/**
 * DTO para la respuesta de autenticación exitosa. Devuelve el JWT.
 * @author leoM2022
 * @version 1.0.0
 */
public record AuthResponseDTO(
        String token,
        String email,
        RolUsuario rol
) {}