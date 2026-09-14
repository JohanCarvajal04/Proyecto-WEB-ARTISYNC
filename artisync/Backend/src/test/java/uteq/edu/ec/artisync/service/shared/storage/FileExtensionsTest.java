package uteq.edu.ec.artisync.service.shared.storage;

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

    @Test
    void contentTypeDe_extensionConocida_devuelveElTipoCanonico() {
        assertThat(FileExtensions.contentTypeDe("foto.jpg")).isEqualTo("image/jpeg");
        assertThat(FileExtensions.contentTypeDe("documento.pdf")).isEqualTo("application/pdf");
    }

    @Test
    void contentTypeDe_referenciaNula_devuelveOctetStream() {
        assertThat(FileExtensions.contentTypeDe(null)).isEqualTo("application/octet-stream");
    }

    @Test
    void contentTypeDe_sinExtension_devuelveOctetStream() {
        assertThat(FileExtensions.contentTypeDe("archivo-sin-extension")).isEqualTo("application/octet-stream");
    }

    @Test
    void contentTypeDe_extensionDesconocida_devuelveOctetStream() {
        assertThat(FileExtensions.contentTypeDe("archivo.xyz")).isEqualTo("application/octet-stream");
    }
}
