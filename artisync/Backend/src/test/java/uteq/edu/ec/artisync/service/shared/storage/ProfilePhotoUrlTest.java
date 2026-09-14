package uteq.edu.ec.artisync.service.shared.storage;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProfilePhotoUrlTest {

    @Test
    void construir_referenciaNull_devuelveNull() {
        assertThat(ProfilePhotoUrl.construir(null)).isNull();
    }

    @Test
    void construir_referenciaEnBlanco_devuelveNull() {
        assertThat(ProfilePhotoUrl.construir("   ")).isNull();
    }

    @Test
    void construir_referenciaValida_devuelveLaUrlPublica() {
        assertThat(ProfilePhotoUrl.construir("perfiles/uuid.jpg"))
                .isEqualTo("/api/v1/usuarios/foto/perfiles/uuid.jpg");
    }
}
