package uteq.edu.ec.artisync.exception;

import io.jsonwebtoken.JwtException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Errores en formato ProblemDetails (RFC 7807), Bloque A.1 de la guia de la Tercera Entrega.
 */
@Slf4j
@RestControllerAdvice
public class ManejadorGlobalExcepciones {

    private static final String BASE_TIPO = "https://artisync.dev/errors/";

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> manejarExcepcionesValidacion(
            MethodArgumentNotValidException ex, HttpServletRequest peticion) {

        Map<String, String> erroresCampos = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            erroresCampos.put(error.getField(), error.getDefaultMessage());
        }

        ProblemDetail pd = construirProblemDetail(
                HttpStatus.BAD_REQUEST,
                "validacion",
                "Error de validación en los datos de entrada",
                peticion.getRequestURI()
        );
        pd.setProperty("fieldErrors", erroresCampos);

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(ExcepcionRecursoNoEncontrado.class)
    public ResponseEntity<ProblemDetail> manejarExcepcionRecursoNoEncontrado(
            ExcepcionRecursoNoEncontrado ex, HttpServletRequest peticion) {

        ProblemDetail pd = construirProblemDetail(
                HttpStatus.NOT_FOUND, "recurso-no-encontrado", ex.getMessage(), peticion.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> manejarNoResourceFoundException(
            NoResourceFoundException ex, HttpServletRequest peticion) {

        ProblemDetail pd = construirProblemDetail(
                HttpStatus.NOT_FOUND, "ruta-no-encontrada", "Recurso no encontrado o ruta inexistente", peticion.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    @ExceptionHandler(ExcepcionRecursoDuplicado.class)
    public ResponseEntity<ProblemDetail> manejarExcepcionRecursoDuplicado(
            ExcepcionRecursoDuplicado ex, HttpServletRequest peticion) {

        ProblemDetail pd = construirProblemDetail(
                HttpStatus.CONFLICT, "recurso-duplicado", ex.getMessage(), peticion.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    @ExceptionHandler(ExcepcionReglaNegocio.class)
    public ResponseEntity<ProblemDetail> manejarExcepcionReglaNegocio(
            ExcepcionReglaNegocio ex, HttpServletRequest peticion) {

        ProblemDetail pd = construirProblemDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, "regla-de-negocio", ex.getMessage(), peticion.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(pd);
    }

    @ExceptionHandler(ExcepcionServicioIaNoDisponible.class)
    public ResponseEntity<ProblemDetail> manejarExcepcionServicioIaNoDisponible(
            ExcepcionServicioIaNoDisponible ex, HttpServletRequest peticion) {

        log.warn("Servicio de IA no disponible en {}: {}", peticion.getRequestURI(), ex.getMessage());
        ProblemDetail pd = construirProblemDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "ia-no-disponible", ex.getMessage(), peticion.getRequestURI());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(pd);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> manejarResponseStatusException(
            ResponseStatusException ex, HttpServletRequest peticion) {

        HttpStatus estado = HttpStatus.valueOf(ex.getStatusCode().value());
        ProblemDetail pd = construirProblemDetail(
                estado, "peticion-rechazada",
                ex.getReason() != null ? ex.getReason() : ex.getMessage(),
                peticion.getRequestURI());
        return ResponseEntity.status(estado).body(pd);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> manejarAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest peticion) {

        ProblemDetail pd = construirProblemDetail(
                HttpStatus.FORBIDDEN, "acceso-denegado",
                "No tienes permisos suficientes para realizar esta acción", peticion.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
    }

    @ExceptionHandler({AuthenticationException.class, JwtException.class})
    public ResponseEntity<ProblemDetail> manejarExcepcionAutenticacion(
            Exception ex, HttpServletRequest peticion) {
        log.warn("Error de autenticación/JWT en {}: {}", peticion.getRequestURI(), ex.getMessage());
        ProblemDetail pd = construirProblemDetail(
                HttpStatus.UNAUTHORIZED, "autenticacion",
                "Credenciales inválidas o token expirado/malformado", peticion.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
    }

    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ProblemDetail> manejarExcepcionesPeticionIncorrecta(
            RuntimeException ex, HttpServletRequest peticion) {

        ProblemDetail pd = construirProblemDetail(
                HttpStatus.BAD_REQUEST, "peticion-invalida", ex.getMessage(), peticion.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> manejarExcepcionGeneral(
            Exception ex, HttpServletRequest peticion) {
        log.error("Error interno no controlado en {}: ", peticion.getRequestURI(), ex);
        ProblemDetail pd = construirProblemDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "error-interno",
                "Ha ocurrido un error interno en el servidor", peticion.getRequestURI());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(pd);
    }

    private ProblemDetail construirProblemDetail(HttpStatus estado, String tipoSlug, String detalle, String instancia) {
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(estado, detalle);
        pd.setType(URI.create(BASE_TIPO + tipoSlug));
        pd.setTitle(estado.getReasonPhrase());
        pd.setInstance(URI.create(instancia));
        return pd;
    }
}
