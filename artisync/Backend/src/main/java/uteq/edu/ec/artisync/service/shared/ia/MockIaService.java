package uteq.edu.ec.artisync.service.shared.ia;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import uteq.edu.ec.artisync.dto.ia.*;

import java.math.BigDecimal;
import java.util.List;

/** Simula respuestas de IA sin llamadas de red. Activo por defecto (ia.provider=mock). */
@Service
@ConditionalOnProperty(name = "ia.provider", havingValue = "mock", matchIfMissing = true)
@Slf4j
public class MockIaService extends AbstractIaService implements IaService {

    public MockIaService() {
        log.info("Servicio de IA MOCK inicializado — no se realizarán llamadas reales a APIs de IA");
    }

    @Override
    public IaVerificacionResponse verificarIdentidad(byte[] imagenBytes, String mimeType) {
        return IaVerificacionResponse.builder()
                .aprobado(true)
                .confianza(new BigDecimal("0.92"))
                .tipoDocumento("cedula")
                .nombreDetectado("Usuario de Prueba")
                .mayorEdad(true)
                .fechaNacimiento("1995-05-20")
                .paisEmision("Ecuador")
                .build();
    }

    @Override
    public IaVerificacionResponse analizarCertificado(byte[] imagenBytes, String mimeType) {
        return IaVerificacionResponse.builder()
                .aprobado(true)
                .confianza(new BigDecimal("0.88"))
                .tipoDocumento("titulo_universitario")
                .nombreDetectado("Usuario de Prueba")
                .institucionEmisora("Universidad de Prueba")
                .campoEstudio("Diseño Gráfico")
                .fechaEmision("2020-07-15")
                .build();
    }

    @Override
    public IaModeracionResponse moderarContenido(String textoMensaje) {
        return IaModeracionResponse.builder()
                .esApropiado(true).categoriaInfraccion("ninguno")
                .confianza(new BigDecimal("0.95")).build();
    }

    @Override
    public IaClasificacionResponse clasificarServicio(String titulo, String descripcion, List<String> categoriasDisponibles) {
        String categoria = categoriasDisponibles.isEmpty() ? "General" : categoriasDisponibles.get(0);
        return IaClasificacionResponse.builder()
                .categoriaSugerida(categoria).subcategoriaSugerida("General")
                .etiquetasSugeridas(List.of("diseño", "creativo"))
                .confianza(new BigDecimal("0.80")).build();
    }

    @Override
    public List<String> sugerirPreguntasBriefing(String categoria, String titulo, String descripcion) {
        return List.of("¿Cuál es el objetivo del proyecto?", "¿Tienes referencias visuales?");
    }

    @Override
    public IaResenaResponse analizarResena(String textoResena, int estrellas) {
        return IaResenaResponse.builder()
                .sentimiento(estrellas >= 4 ? "positivo" : "neutro")
                .esCoherenteConEstrellas(true).esSpam(false).esInapropiado(false)
                .confianza(new BigDecimal("0.90")).build();
    }
}
