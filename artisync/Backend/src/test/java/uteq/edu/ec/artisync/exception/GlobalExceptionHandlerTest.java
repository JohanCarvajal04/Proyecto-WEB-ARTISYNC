package uteq.edu.ec.artisync.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler manejador = new GlobalExceptionHandler();
    private final HttpServletRequest peticion = mock(HttpServletRequest.class);

    {
        when(peticion.getRequestURI()).thenReturn("/api/v1/recurso");
    }

    @Test
    void excepcionServicioIaNoDisponible_devuelve503ComoProblemDetail() {
        AiServiceUnavailableException ex =
                new AiServiceUnavailableException("NVIDIA no respondió", new RuntimeException("timeout"));

        ResponseEntity<ProblemDetail> respuesta = manejador.handleAiServiceUnavailableException(ex, peticion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(respuesta.getBody().getDetail()).isEqualTo("NVIDIA no respondió");
    }

    @Test
    void excepcionesValidacion_devuelve400ConDetallePorCampo() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = mock(BindingResult.class);
        FieldError error = new FieldError("objeto", "correo", "debe ser un correo válido");
        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(List.of(error));

        ResponseEntity<ProblemDetail> respuesta = manejador.manejarExcepcionesValidacion(ex, peticion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().getProperties()).containsKey("fieldErrors");
    }

    @Test
    void recursoNoEncontrado_devuelve404ConMensajeDeLaExcepcion() {
        ResourceNotFoundException ex = new ResourceNotFoundException("Usuario no encontrado");

        ResponseEntity<ProblemDetail> respuesta = manejador.manejarExcepcionRecursoNoEncontrado(ex, peticion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody().getDetail()).isEqualTo("Usuario no encontrado");
    }

    @Test
    void rutaSinControlador_devuelve404Generico() {
        NoResourceFoundException ex = mock(NoResourceFoundException.class);

        ResponseEntity<ProblemDetail> respuesta = manejador.manejarNoResourceFoundException(ex, peticion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(respuesta.getBody().getDetail()).isEqualTo("Recurso no encontrado o ruta inexistente");
    }

    @Test
    void recursoDuplicado_devuelve409ConMensajeDeLaExcepcion() {
        DuplicateResourceException ex = new DuplicateResourceException("Ya existe un usuario con ese correo");

        ResponseEntity<ProblemDetail> respuesta = manejador.manejarExcepcionRecursoDuplicado(ex, peticion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(respuesta.getBody().getDetail()).isEqualTo("Ya existe un usuario con ese correo");
    }

    @Test
    void reglaDeNegocio_devuelve422ConMensajeDeLaExcepcion() {
        BusinessRuleException ex = new BusinessRuleException("El pedido ya fue entregado");

        ResponseEntity<ProblemDetail> respuesta = manejador.manejarExcepcionReglaNegocio(ex, peticion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(respuesta.getBody().getDetail()).isEqualTo("El pedido ya fue entregado");
    }

    @Test
    void responseStatusException_conMotivoExplicito_usaElMotivoComoDetalle() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.PAYMENT_REQUIRED, "Saldo insuficiente");

        ResponseEntity<ProblemDetail> respuesta = manejador.manejarResponseStatusException(ex, peticion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.PAYMENT_REQUIRED);
        assertThat(respuesta.getBody().getDetail()).isEqualTo("Saldo insuficiente");
    }

    @Test
    void responseStatusException_sinMotivo_usaElMensajeComoDetalle() {
        ResponseStatusException ex = mock(ResponseStatusException.class);
        when(ex.getStatusCode()).thenReturn((HttpStatusCode) HttpStatus.BAD_GATEWAY);
        when(ex.getReason()).thenReturn(null);
        when(ex.getMessage()).thenReturn("502 BAD_GATEWAY");

        ResponseEntity<ProblemDetail> respuesta = manejador.manejarResponseStatusException(ex, peticion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_GATEWAY);
        assertThat(respuesta.getBody().getDetail()).isEqualTo("502 BAD_GATEWAY");
    }

    @Test
    void cuotaExcedida_devuelve429ConCabeceraRetryAfter() {
        QuotaExceededException ex = new QuotaExceededException("Límite de intentos alcanzado", 30L);

        ResponseEntity<ProblemDetail> respuesta = manejador.manejarExcepcionCuotaExcedida(ex, peticion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(respuesta.getHeaders().getFirst("Retry-After")).isEqualTo("30");
    }

    @Test
    void accesoDenegado_devuelve403ConMensajeGenerico() {
        AccessDeniedException ex = new AccessDeniedException("denegado");

        ResponseEntity<ProblemDetail> respuesta = manejador.manejarAccessDeniedException(ex, peticion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(respuesta.getBody().getDetail()).isEqualTo("No tienes permisos suficientes para realizar esta acción");
    }

    @Test
    void excepcionAutenticacion_devuelve401ConMensajeGenerico() {
        BadCredentialsException ex = new BadCredentialsException("credenciales inválidas");

        ResponseEntity<ProblemDetail> respuesta = manejador.manejarExcepcionAutenticacion(ex, peticion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(respuesta.getBody().getDetail()).isEqualTo("Credenciales inválidas o token expirado/malformado");
    }

    @Test
    void peticionIncorrecta_devuelve400ConMensajeDeLaExcepcion() {
        IllegalArgumentException ex = new IllegalArgumentException("el id no puede ser negativo");

        ResponseEntity<ProblemDetail> respuesta = manejador.manejarExcepcionesPeticionIncorrecta(ex, peticion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(respuesta.getBody().getDetail()).isEqualTo("el id no puede ser negativo");
    }

    @Test
    void excepcionGeneral_devuelve500SinExponerElMensajeInterno() {
        RuntimeException ex = new RuntimeException("detalle interno sensible");

        ResponseEntity<ProblemDetail> respuesta = manejador.manejarExcepcionGeneral(ex, peticion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(respuesta.getBody().getDetail()).isEqualTo("Ha ocurrido un error interno en el servidor");
    }
}
