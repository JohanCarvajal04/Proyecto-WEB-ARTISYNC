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
 * Componente transversal de infraestructura: Controlador de asesoramiento (ControllerAdvice).
 * 
 * Propósito: Centralizar y capturar las excepciones lanzadas desde cualquier capa de la aplicacion para estandarizarlas bajo el formato ProblemDetails (RFC 7807).
 * 
 * Flujo interno: Intercepta excepciones (via @ExceptionHandler) y devuelve un payload JSON coherente con el codigo HTTP apropiado (400, 401, 404, 500), evitando la filtracion de stacktraces.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String BASE_TIPO = "https://artisync.dev/errors/";

    /**
     * Errores de validación de {@code @Valid} en el cuerpo de la petición: agrega
     * el detalle por campo (nombre → mensaje) en {@code fieldErrors}.
     *
     * @param ex excepción con los errores de binding/validación
     * @param peticion petición que falló la validación
     * @return 400 con el detalle de validación y los campos afectados en {@code fieldErrors}
     */
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

    /**
     * @param ex excepción de dominio; su mensaje pasa tal cual como detalle
     * @param peticion petición cuyo recurso solicitado no existe
     * @return 404 con el mensaje de la excepción como detalle
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> manejarExcepcionRecursoNoEncontrado(
            ResourceNotFoundException ex, HttpServletRequest peticion) {

        ProblemDetail pd = construirProblemDetail(
                HttpStatus.NOT_FOUND, "recurso-no-encontrado", ex.getMessage(), peticion.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    /**
     * Ruta sin controlador ni recurso estático asociado (lanzada por Spring MVC,
     * no por código de dominio).
     *
     * @param ex excepción de Spring MVC ante una ruta inexistente
     * @param peticion petición a una ruta que no resuelve a ningún handler
     * @return 404 genérico, sin exponer detalle interno de enrutamiento
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ProblemDetail> manejarNoResourceFoundException(
            NoResourceFoundException ex, HttpServletRequest peticion) {

        ProblemDetail pd = construirProblemDetail(
                HttpStatus.NOT_FOUND, "ruta-no-encontrada", "Recurso no encontrado o ruta inexistente", peticion.getRequestURI());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(pd);
    }

    /**
     * @param ex excepción de dominio ante una violación de unicidad (correo, nombre, etc.)
     * @param peticion petición que intentó crear el recurso duplicado
     * @return 409 con el mensaje de la excepción como detalle
     */
    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ProblemDetail> manejarExcepcionRecursoDuplicado(
            DuplicateResourceException ex, HttpServletRequest peticion) {

        ProblemDetail pd = construirProblemDetail(
                HttpStatus.CONFLICT, "recurso-duplicado", ex.getMessage(), peticion.getRequestURI());
        return ResponseEntity.status(HttpStatus.CONFLICT).body(pd);
    }

    /**
     * @param ex excepción de dominio ante el incumplimiento de una regla de negocio
     * @param peticion petición que incumplió la regla
     * @return 422 con el mensaje de la excepción como detalle
     */
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ProblemDetail> manejarExcepcionReglaNegocio(
            BusinessRuleException ex, HttpServletRequest peticion) {

        ProblemDetail pd = construirProblemDetail(
                HttpStatus.UNPROCESSABLE_ENTITY, "regla-de-negocio", ex.getMessage(), peticion.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY).body(pd);
    }

    /**
     * @param ex excepción ante un fallo o indisponibilidad del proveedor de IA externo
     * @param peticion petición que dependía del servicio de IA
     * @return 503 con el mensaje de la excepción como detalle; el fallo se registra
     *         además como warning (no error), por ser un fallo esperado de un tercero
     */
    @ExceptionHandler(AiServiceUnavailableException.class)
    public ResponseEntity<ProblemDetail> manejarExcepcionServicioIaNoDisponible(
            AiServiceUnavailableException ex, HttpServletRequest peticion) {

        log.warn("Offering de IA no disponible en {}: {}", peticion.getRequestURI(), ex.getMessage());
        ProblemDetail pd = construirProblemDetail(
                HttpStatus.SERVICE_UNAVAILABLE, "ia-no-disponible", ex.getMessage(), peticion.getRequestURI());
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(pd);
    }

    /**
     * Captura genérica para código que lanza {@link ResponseStatusException}
     * directamente (en vez de una excepción de dominio propia), preservando el
     * código de estado y motivo que ese código ya decidió.
     *
     * @param ex excepción con el estado HTTP y motivo ya resueltos
     * @param peticion petición rechazada
     * @return el mismo estado HTTP de {@code ex}, con {@code ex.getReason()} como
     *         detalle (o {@code ex.getMessage()} si no hay motivo explícito)
     */
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

    /**
     * @param ex excepción ante el agotamiento de una cuota operativa (p. ej. IA);
     *           su {@code retryAfterSegundos} se refleja en la cabecera {@code Retry-After}
     * @param peticion petición que superó la cuota
     * @return 429 con el mensaje de la excepción como detalle y cabecera {@code Retry-After}
     */
    @ExceptionHandler(QuotaExceededException.class)
    public ResponseEntity<ProblemDetail> manejarExcepcionCuotaExcedida(
            QuotaExceededException ex, HttpServletRequest peticion) {

        log.warn("Cuota de intentos por cuenta excedida en {}: {}", peticion.getRequestURI(), ex.getMessage());
        ProblemDetail pd = construirProblemDetail(
                HttpStatus.TOO_MANY_REQUESTS, "cuota-excedida", ex.getMessage(), peticion.getRequestURI());
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header("Retry-After", String.valueOf(ex.getRetryAfterSegundos()))
                .body(pd);
    }

    /**
     * Lanzada por Spring Security cuando {@code @PreAuthorize} rechaza al usuario
     * ya autenticado (a diferencia de {@link #manejarExcepcionAutenticacion}, que
     * cubre al usuario sin autenticar).
     *
     * @param ex excepción de autorización de Spring Security
     * @param peticion petición rechazada por falta de permisos
     * @return 403 con un mensaje genérico (no se expone qué permiso exacto faltaba)
     */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> manejarAccessDeniedException(
            AccessDeniedException ex, HttpServletRequest peticion) {

        ProblemDetail pd = construirProblemDetail(
                HttpStatus.FORBIDDEN, "acceso-denegado",
                "No tienes permisos suficientes para realizar esta acción", peticion.getRequestURI());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(pd);
    }

    /**
     * Cubre tanto fallos de autenticación de Spring Security como excepciones
     * crudas de la librería JWT que se escapen fuera de {@code JwtAuthenticationFilter}
     * (que normalmente ya las captura y responde antes de llegar aquí).
     *
     * @param ex la excepción de autenticación o JWT capturada
     * @param peticion petición sin credenciales válidas
     * @return 401 con un mensaje genérico (nunca el detalle interno de {@code ex})
     */
    @ExceptionHandler({AuthenticationException.class, JwtException.class})
    public ResponseEntity<ProblemDetail> manejarExcepcionAutenticacion(
            Exception ex, HttpServletRequest peticion) {
        log.warn("Error de autenticación/JWT en {}: {}", peticion.getRequestURI(), ex.getMessage());
        ProblemDetail pd = construirProblemDetail(
                HttpStatus.UNAUTHORIZED, "autenticacion",
                "Credenciales inválidas o token expirado/malformado", peticion.getRequestURI());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(pd);
    }

    /**
     * Red de seguridad para código que valida con excepciones estándar de Java
     * en vez de una excepción de dominio propia.
     *
     * @param ex la excepción de argumento o estado inválido
     * @param peticion petición con datos de entrada inválidos
     * @return 400 con el mensaje de la excepción como detalle
     */
    @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
    public ResponseEntity<ProblemDetail> manejarExcepcionesPeticionIncorrecta(
            RuntimeException ex, HttpServletRequest peticion) {

        ProblemDetail pd = construirProblemDetail(
                HttpStatus.BAD_REQUEST, "peticion-invalida", ex.getMessage(), peticion.getRequestURI());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(pd);
    }

    /**
     * Último recurso: cualquier excepción no cubierta por los handlers
     * anteriores. Registra el stacktrace completo como error (a diferencia del
     * resto, que solo hacen warning o nada) y nunca expone {@code ex.getMessage()}
     * al cliente, para no filtrar detalles internos.
     *
     * @param ex la excepción no controlada
     * @param peticion petición durante la que ocurrió el error
     * @return 500 con un mensaje genérico
     */
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

