package uteq.edu.ec.artisync.service.shared.storage;

import org.junit.jupiter.api.Test;
import uteq.edu.ec.artisync.exception.BusinessRuleException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StoragePrefixTest {

    @Test
    void componer_sinPrefijo_devuelveElNombreSinCambios() {
        assertThat(StoragePrefix.componer(null, "archivo.png")).isEqualTo("archivo.png");
        assertThat(StoragePrefix.componer("  ", "archivo.png")).isEqualTo("archivo.png");
    }

    @Test
    void componer_conPrefijoValido_uneAmbosConBarra() {
        assertThat(StoragePrefix.componer(StoragePrefix.PORTAFOLIO, "obra.jpg"))
                .isEqualTo("portafolio/obra.jpg");
    }

    @Test
    void componer_conPrefijoInvalido_lanzaExcepcion() {
        assertThatThrownBy(() -> StoragePrefix.componer("../otro-directorio", "archivo.png"))
                .isInstanceOf(BusinessRuleException.class);
    }
}
