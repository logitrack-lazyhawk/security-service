package lazyHawk.security_service.dto;

import lazyHawk.security_service.dominio.enumerador.RolUsuario;

/**
 * DTO para devolver los datos públicos del usuario autenticado (/me).*
 * @author leoM2022
 * @version 1.0.0
 */
public record UsuarioPerfilDTO(
        String id,
        String email,
        RolUsuario rol,
        boolean activo
) {}