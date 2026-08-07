package uteq.edu.ec.artisync.config;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IaPropertiesTest {

    @Test
    void valoresPorDefecto_sonSegurosParaArrancarSinConfiguracion() {
        IaProperties propiedades = new IaProperties();

        assertThat(propiedades.getProvider()).isEqualTo("mock");
        assertThat(propiedades.getConfidenceThreshold()).isEqualTo(0.75);
        assertThat(propiedades.getTimeoutSeconds()).isEqualTo(30);
        assertThat(propiedades.getNvidia().getModel()).isEqualTo("nvidia/llama-3.2-nv-vision-instruct");
        assertThat(propiedades.getNvidia().getApiKey()).isEmpty();
    }
}
