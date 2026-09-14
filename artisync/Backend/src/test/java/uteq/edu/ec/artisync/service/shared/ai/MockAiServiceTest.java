package uteq.edu.ec.artisync.service.shared.ai;

import org.junit.jupiter.api.Test;
import uteq.edu.ec.artisync.dto.ai.AiVerificationResponse;

import static org.assertj.core.api.Assertions.assertThat;

class MockAiServiceTest {

    private final MockAiService servicio = new MockAiService();

    @Test
    void verificarIdentidad_devuelveDictamenSimuladoAprobado() {
        AiVerificationResponse respuesta = servicio.verifyIdentity("bytes".getBytes(), "image/jpeg");

        assertThat(respuesta.isAprobado()).isTrue();
        assertThat(respuesta.getConfianza()).isEqualByComparingTo("0.92");
        assertThat(respuesta.getTipoDocumento()).isEqualTo("cedula");
    }

    @Test
    void analizarCertificado_devuelveDictamenSimuladoAprobado() {
        AiVerificationResponse respuesta = servicio.analyzeCertificate("bytes".getBytes(), "image/jpeg");

        assertThat(respuesta.isAprobado()).isTrue();
        assertThat(respuesta.getInstitucionEmisora()).isNotBlank();
    }
}
