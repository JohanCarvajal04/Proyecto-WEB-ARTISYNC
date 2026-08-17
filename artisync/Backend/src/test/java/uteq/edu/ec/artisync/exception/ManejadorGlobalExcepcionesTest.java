package uteq.edu.ec.artisync.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ManejadorGlobalExcepcionesTest {

    @Test
    void excepcionServicioIaNoDisponible_devuelve503ComoProblemDetail() {
        ManejadorGlobalExcepciones manejador = new ManejadorGlobalExcepciones();
        HttpServletRequest peticion = mock(HttpServletRequest.class);
        when(peticion.getRequestURI()).thenReturn("/api/v1/verificaciones/1/analisis-ia");

        ExcepcionServicioIaNoDisponible ex =
                new ExcepcionServicioIaNoDisponible("NVIDIA no respondió", new RuntimeException("timeout"));

        ResponseEntity<ProblemDetail> respuesta = manejador.manejarExcepcionServicioIaNoDisponible(ex, peticion);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(respuesta.getBody().getDetail()).isEqualTo("NVIDIA no respondió");
    }
}
