package uteq.edu.ec.artisync.exception;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaError;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> manejarExcepcionesValidacion(
            MethodArgumentNotValidException ex, HttpServletRequest peticion) {

        Map<String, String> erroresCampos = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            erroresCampos.put(error.getField(), error.getDefaultMessage());
        }

        RespuestaError respuesta = construirRespuestaError(
                HttpStatus.BAD_REQUEST,
                "Error de validación en los datos de entrada",
                peticion.getRequestURI(),
                erroresCampos
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);
    }

    @ExceptionHandler(ExcepcionRecursoNoEncontrado.class)
    public ResponseEntity<RespuestaError> manejarExcepcionRecursoNoEncontrado(
            ExcepcionRecursoNoEncontrado ex, HttpServletRequest peticion) {

        RespuestaError respuesta = construirRespuestaError(
                HttpStatus.NOT_FOUND,
                ex.getMessage(),
                peticion.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<RespuestaError> manejarNoResourceFoundException(
            NoResourceFoundException ex, HttpServletRequest peticion) {

        RespuestaError respuesta = construirRespuestaError(
                HttpStatus.NOT_FOUND,
                "Recurso no encontrado o ruta inexistente",
                peticion.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(respuesta);
    }

    @ExceptionHandler(ExcepcionRecursoDuplicado.class)
    public ResponseEntity<RespuestaError> manejarExcepcionRecursoDuplicado(
            ExcepcionRecursoDuplicado ex, HttpServletRequest peticion) {

        RespuestaError respuesta = construirRespuestaError(
                HttpStatus.CONFLICT,
                ex.getMessage(),
                peticion.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(respuesta);
    }

    @ExceptionHandler(ExcepcionReglaNegocio.class)
    public ResponseEntity<RespuestaError> manejarExcepcionReglaNegocio(
            ExcepcionReglaNegocio ex, HttpServletRequest peticion) {

        RespuestaError respuesta = construirRespuestaError(
                HttpStatus.UNPROCESSABLE_ENTITY,
                ex.getMessage(),
                peticion.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(respuesta);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<RespuestaError> manejarResponseStatusException(
            ResponseStatusException ex, HttpServletRequest peticion) {

        HttpStatus estado = HttpStatus.valueOf(ex.getStatusCode().value());
        RespuestaError respuesta = construirRespuestaError(
                estado,
                ex.getReason() != null ? ex.getReason() : ex.getMessage(),
                peticion.getRequestURI(),
                null
        );
        return ResponseEntity.status(estado).body(respuesta);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RespuestaError> manejarAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest peticion) {

        RespuestaError respuesta = construirRespuestaError(
                HttpStatus.FORBIDDEN,
                "No tienes permisos suficientes para realizar esta acción",
                peticion.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(respuesta);
    }

    @ExceptionHandler({AuthenticationException.class, JwtException.class})
    public ResponseEntity<RespuestaError> manejarExcepcionAutenticacion(
            Exception ex, HttpServletRequest peticion) {
        log.warn("Error de autenticación/JWT en {}: {}", peticion.getRequestURI(), ex.getMessage());
        RespuestaError respuesta = construirRespuestaError(
                HttpStatus.UNAUTHORIZED,
                "Credenciales inválidas o token expirado/malformado",
                peticion.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(respuesta);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<RespuestaError> manejarExcepcionesPeticionIncorrecta(
            RuntimeException ex, HttpServletRequest peticion) {

        RespuestaError respuesta = construirRespuestaError(
                HttpStatus.BAD_REQUEST,
                ex.getMessage(),
                peticion.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaError> manejarExcepcionGeneral(
            Exception ex, HttpServletRequest peticion) {
        log.error("Error interno no controlado en {}: ", peticion.getRequestURI(), ex);
        RespuestaError respuesta = construirRespuestaError(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Ha ocurrido un error interno en el servidor",
                peticion.getRequestURI(),
                null
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(respuesta);
    }

    private RespuestaError construirRespuestaError(HttpStatus estado, String mensaje, String ruta, Map<String, String> erroresCampos) {
        return RespuestaError.builder()
                .timestamp(LocalDateTime.now())
                .status(estado.value())
                .error(estado.getReasonPhrase())
                .message(mensaje)
                .path(ruta)
                .fieldErrors(erroresCampos)
                .build();
    }
}
