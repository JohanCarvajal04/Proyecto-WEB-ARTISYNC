package uteq.edu.ec.artisync.service.shared.ia;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AbstractIaServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    static class ServicioDePrueba extends AbstractIaService {
        String cargar(String archivo, Object... args) { return cargarPrompt(archivo, args); }
        String extraer(String respuesta) { return extraerJson(respuesta); }
        String texto(JsonNode nodo, String campo) { return textoONull(nodo, campo); }
        BigDecimal decimal(Object valor) { return toBigDecimal(valor); }
        BigDecimal acotar(BigDecimal valor) { return acotarConfianza(valor); }
    }

    private final ServicioDePrueba servicio = new ServicioDePrueba();

    @Test
    void cargarPrompt_sustituyePlaceholders() {
        String resultado = servicio.cargar("prompt_moderacion_mensaje.md", "hola mundo");
        assertThat(resultado).contains("hola mundo");
    }

    @Test
    void cargarPrompt_archivoInexistente_lanzaExcepcion() {
        assertThrows(IllegalStateException.class, () -> servicio.cargar("no_existe.md"));
    }

    @Test
    void extraerJson_bloqueMarkdown() {
        String respuesta = "Aquí tienes:\n```json\n{\"a\":1}\n```\ngracias";
        assertThat(servicio.extraer(respuesta)).isEqualTo("{\"a\":1}");
    }

    @Test
    void extraerJson_soloLlaves() {
        assertThat(servicio.extraer("{\"a\":1}")).isEqualTo("{\"a\":1}");
    }

    @Test
    void extraerJson_textoAlrededor() {
        assertThat(servicio.extraer("resultado: {\"a\":1} fin")).isEqualTo("{\"a\":1}");
    }

    @Test
    void extraerJson_sinJson_devuelveObjetoVacio() {
        assertThat(servicio.extraer("no hay json aquí")).isEqualTo("{}");
    }

    @Test
    void textoONull_campoNuloDeJson_devuelveNullDeJava_noLaCadenaNull() {
        JsonNode nodo = mapper.readTree("{\"nombre\": null}");
        assertThat(servicio.texto(nodo, "nombre")).isNull();
    }

    @Test
    void textoONull_campoAusente_devuelveNull() {
        JsonNode nodo = mapper.readTree("{}");
        assertThat(servicio.texto(nodo, "nombre")).isNull();
    }

    @Test
    void textoONull_campoConValor_loDevuelve() {
        JsonNode nodo = mapper.readTree("{\"nombre\": \"Ana\"}");
        assertThat(servicio.texto(nodo, "nombre")).isEqualTo("Ana");
    }

    @Test
    void toBigDecimal_valorNulo_devuelveCero() {
        assertThat(servicio.decimal(null)).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void acotarConfianza_fueraDeRango_seRecorta() {
        assertThat(servicio.acotar(new BigDecimal("1.5"))).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(servicio.acotar(new BigDecimal("-0.2"))).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(servicio.acotar(new BigDecimal("0.6"))).isEqualByComparingTo(new BigDecimal("0.6"));
    }
}
