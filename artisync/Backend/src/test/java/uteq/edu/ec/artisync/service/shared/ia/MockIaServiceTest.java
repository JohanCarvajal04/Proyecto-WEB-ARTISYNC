package uteq.edu.ec.artisync.service.shared.ia;

import org.junit.jupiter.api.Test;
import uteq.edu.ec.artisync.dto.ia.IaVerificacionResponse;

import static org.assertj.core.api.Assertions.assertThat;

class MockIaServiceTest {

    private final MockIaService servicio = new MockIaService();

    @Test
    void verificarIdentidad_devuelveDictamenSimuladoAprobado() {
        IaVerificacionResponse respuesta = servicio.verificarIdentidad("bytes".getBytes(), "image/jpeg");

        assertThat(respuesta.isAprobado()).isTrue();
        assertThat(respuesta.getConfianza()).isEqualByComparingTo("0.92");
        assertThat(respuesta.getTipoDocumento()).isEqualTo("cedula");
    }

    @Test
    void analizarCertificado_devuelveDictamenSimuladoAprobado() {
        IaVerificacionResponse respuesta = servicio.analizarCertificado("bytes".getBytes(), "image/jpeg");

        assertThat(respuesta.isAprobado()).isTrue();
        assertThat(respuesta.getInstitucionEmisora()).isNotBlank();
    }
}
