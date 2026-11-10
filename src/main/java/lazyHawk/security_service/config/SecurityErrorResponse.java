package lazyHawk.security_service.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lazyHawk.security_service.dto.ErrorResponseDto;
import lazyHawk.security_service.mensaje.Mensajes;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Map;

final class SecurityErrorResponse {
    private static final JsonMapper JSON = JsonMapper.builder().findAndAddModules().build();

    private SecurityErrorResponse() {}

    static void escribir(HttpServletRequest request, HttpServletResponse response, String clave) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(JSON.writeValueAsString(new ErrorResponseDto(
                LocalDateTime.now(), 401, "Unauthorized", Mensajes.obtener(clave),
                request.getRequestURI(), Map.of())));
    }

    static void escribirForbidden(HttpServletRequest request, HttpServletResponse response, String clave) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(JSON.writeValueAsString(new ErrorResponseDto(
                LocalDateTime.now(), 403, "Forbidden", Mensajes.obtener(clave),
                request.getRequestURI(), Map.of())));
    }
}
