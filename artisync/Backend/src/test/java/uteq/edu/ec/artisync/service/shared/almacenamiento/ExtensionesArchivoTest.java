package uteq.edu.ec.artisync.service.shared.almacenamiento;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExtensionesArchivoTest {

    @Test
    void desde_tiposConocidos_devuelveLaExtensionCorrespondiente() {
        assertThat(ExtensionesArchivo.desde("image/jpeg")).isEqualTo(".jpg");
        assertThat(ExtensionesArchivo.desde("image/png")).isEqualTo(".png");
        assertThat(ExtensionesArchivo.desde("application/pdf")).isEqualTo(".pdf");
        assertThat(ExtensionesArchivo.desde("video/mp4")).isEqualTo(".mp4");
    }

    @Test
    void desde_ignoraParametrosYMayusculas() {
        assertThat(ExtensionesArchivo.desde("IMAGE/JPEG; charset=UTF-8")).isEqualTo(".jpg");
    }

    @Test
    void desde_tipoDesconocidoONulo_noSeDisfrazaDeImagen() {
        assertThat(ExtensionesArchivo.desde("application/x-desconocido")).isEqualTo(".bin");
        assertThat(ExtensionesArchivo.desde(null)).isEqualTo(".bin");
        assertThat(ExtensionesArchivo.desde("   ")).isEqualTo(".bin");
    }
}
