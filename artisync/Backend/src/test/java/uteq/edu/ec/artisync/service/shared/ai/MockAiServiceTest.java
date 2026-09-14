package uteq.edu.ec.artisync.service.shared.ai;

import org.junit.jupiter.api.Test;
import uteq.edu.ec.artisync.dto.ai.AiClassificationResponse;
import uteq.edu.ec.artisync.dto.ai.AiReviewResponse;
import uteq.edu.ec.artisync.dto.ai.AiVerificationResponse;
import uteq.edu.ec.artisync.dto.ai.IaModeracionResponse;

import java.util.List;

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

    @Test
    void moderarContenido_devuelveApropiadoSimulado() {
        IaModeracionResponse respuesta = servicio.moderarContenido("hola mundo");

        assertThat(respuesta.isEsApropiado()).isTrue();
        assertThat(respuesta.getCategoriaInfraccion()).isEqualTo("ninguno");
    }

    @Test
    void clasificarServicio_conCategoriasDisponibles_usaLaPrimera() {
        AiClassificationResponse respuesta = servicio.classifyOffering(
                "Logo", "Descripción", List.of("Diseño", "Ilustración"));

        assertThat(respuesta.getCategoriaSugerida()).isEqualTo("Diseño");
        assertThat(respuesta.getEtiquetasSugeridas()).contains("diseño", "creativo");
    }

    @Test
    void clasificarServicio_sinCategoriasDisponibles_usaGeneral() {
        AiClassificationResponse respuesta = servicio.classifyOffering("Logo", "Descripción", List.of());

        assertThat(respuesta.getCategoriaSugerida()).isEqualTo("General");
    }

    @Test
    void sugerirPreguntasBriefing_devuelvePreguntasSimuladas() {
        List<String> preguntas = servicio.sugerirPreguntasBriefing("Diseño", "Logo", "Descripción");

        assertThat(preguntas).isNotEmpty();
    }

    @Test
    void analizarResena_conEstrellasAltas_sentimientoPositivo() {
        AiReviewResponse respuesta = servicio.analyzeReview("Excelente trabajo", 5);

        assertThat(respuesta.getSentimiento()).isEqualTo("positivo");
    }

    @Test
    void analizarResena_conEstrellasBajas_sentimientoNeutro() {
        AiReviewResponse respuesta = servicio.analyzeReview("Podría mejorar", 2);

        assertThat(respuesta.getSentimiento()).isEqualTo("neutro");
    }
}
