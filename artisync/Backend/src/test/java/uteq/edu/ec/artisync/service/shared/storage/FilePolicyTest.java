package uteq.edu.ec.artisync.service.shared.storage;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import uteq.edu.ec.artisync.exception.BusinessRuleException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilePolicyTest {

    @Test
    void validate_archivoVacio_lanzaExcepcion() {
        MockMultipartFile vacio = new MockMultipartFile("imagen", "vacio.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> FilePolicy.PERFIL.validate(vacio))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("vacío");
    }

    @Test
    void validate_tipoNoPermitido_lanzaExcepcion() {
        MockMultipartFile pdf = new MockMultipartFile("imagen", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> FilePolicy.PERFIL.validate(pdf))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Formato no soportado");
    }

    @Test
    void validate_excedeElTamanoMaximo_lanzaExcepcion() {
        byte[] contenido = new byte[(int) (FilePolicy.BOCETO.maxBytes() + 1)];
        MockMultipartFile grande = new MockMultipartFile("imagen", "grande.png", "image/png", contenido);

        assertThatThrownBy(() -> FilePolicy.BOCETO.validate(grande))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("supera el máximo");
    }

    @Test
    void validate_archivoValido_noLanzaExcepcion() {
        MockMultipartFile valido = new MockMultipartFile("imagen", "foto.png", "image/png", new byte[]{1, 2, 3});

        assertThatCode(() -> FilePolicy.PERFIL.validate(valido)).doesNotThrowAnyException();
    }

    @Test
    void validate_contentTypeConParametros_seNormalizaAntesDeComparar() {
        MockMultipartFile conCharset =
                new MockMultipartFile("imagen", "foto.png", "image/png; charset=UTF-8", new byte[]{1, 2, 3});

        assertThatCode(() -> FilePolicy.PERFIL.validate(conCharset)).doesNotThrowAnyException();
    }
}
