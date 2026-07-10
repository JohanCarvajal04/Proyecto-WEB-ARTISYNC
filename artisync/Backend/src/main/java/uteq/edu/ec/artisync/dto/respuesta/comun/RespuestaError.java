package uteq.edu.ec.artisync.dto.respuesta.comun;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.Map;

@Builder
public record RespuestaError(
        LocalDateTime timestamp,
        int status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {
    public int getStatus() { return status; }
    public String getError() { return error; }
    public String getMessage() { return message; }
    public String getPath() { return path; }
    public Map<String, String> getFieldErrors() { return fieldErrors; }
    public LocalDateTime getTimestamp() { return timestamp; }
}
