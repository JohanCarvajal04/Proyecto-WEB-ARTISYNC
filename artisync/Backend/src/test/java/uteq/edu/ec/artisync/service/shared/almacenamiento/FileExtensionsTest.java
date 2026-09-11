package uteq.edu.ec.artisync.service.shared.almacenamiento;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class FileExtensionsTest {

    @Test
    void desde_tiposConocidos_devuelveLaExtensionCorrespondiente() {
        assertThat(FileExtensions.desde("image/jpeg")).isEqualTo(".jpg");
        assertThat(FileExtensions.desde("image/png")).isEqualTo(".png");
        assertThat(FileExtensions.desde("application/pdf")).isEqualTo(".pdf");
        assertThat(FileExtensions.desde("video/mp4")).isEqualTo(".mp4");
    }

    @Test
    void desde_ignoraParametrosYMayusculas() {
        assertThat(FileExtensions.desde("IMAGE/JPEG; charset=UTF-8")).isEqualTo(".jpg");
    }

    @Test
    void desde_tipoDesconocidoONulo_noSeDisfrazaDeImagen() {
        assertThat(FileExtensions.desde("application/x-desconocido")).isEqualTo(".bin");
        assertThat(FileExtensions.desde(null)).isEqualTo(".bin");
        assertThat(FileExtensions.desde("   ")).isEqualTo(".bin");
    }
}
