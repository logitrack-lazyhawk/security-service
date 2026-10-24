package lazyHawk.security_service.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Clase que representa la respuesta de error en formato DTO (Data Transfer Object).
 * Esta clase se utiliza para encapsular la información de error que se devuelve al cliente.
 * Contiene detalles como la marca de tiempo, el estado HTTP, el tipo de error y un mensaje descriptivo.
 * @author leoM2022
 * @version 1.0.0
 */
@Getter
@Setter
@AllArgsConstructor
public class ErrorResponseDTO {
    private LocalDateTime timesTamp;
    private int status;
    private String error;
    private String message;
}
