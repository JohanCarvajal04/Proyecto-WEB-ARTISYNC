package uteq.edu.ec.artisync.service.shared.almacenamiento;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import uteq.edu.ec.artisync.config.AlmacenamientoProperties;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AlmacenamientoLocalTest {

    @TempDir
    Path directorioTemporal;

    private AlmacenamientoLocal almacenamiento;

    @BeforeEach
    void setUp() {
        AlmacenamientoProperties propiedades = new AlmacenamientoProperties();
        propiedades.setRutaBase(directorioTemporal.toString());
        almacenamiento = new AlmacenamientoLocal(propiedades);
    }

    @Test
    void guardarYLeer_devuelveLosMismosBytes() {
        MockMultipartFile archivo = new MockMultipartFile(
                "documento", "cedula.jpg", "image/jpeg", "contenido-de-prueba".getBytes());

        String referencia = almacenamiento.guardar(archivo);
        byte[] leido = almacenamiento.leer(referencia);

        assertThat(referencia).endsWith(".jpg");
        assertThat(new String(leido)).isEqualTo("contenido-de-prueba");
    }

    @Test
    void leer_referenciaInexistente_lanzaExcepcionRecursoNoEncontrado() {
        assertThrows(ExcepcionRecursoNoEncontrado.class, () -> almacenamiento.leer("no-existe.jpg"));
    }

    @Test
    void leer_intentoDeEscapeFueraDeLaRutaBase_esRechazado() {
        assertThrows(ExcepcionReglaNegocio.class, () -> almacenamiento.leer("../../etc/passwd"));
    }

    @Test
    void eliminar_referenciaExistente_laBorra() {
        MockMultipartFile archivo = new MockMultipartFile(
                "documento", "titulo.png", "image/png", "otro-contenido".getBytes());
        String referencia = almacenamiento.guardar(archivo);

        almacenamiento.eliminar(referencia);

        assertThrows(ExcepcionRecursoNoEncontrado.class, () -> almacenamiento.leer(referencia));
    }

    @Test
    void guardar_conPrefijo_creaElSubdirectorioYConservaElContenido() {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "obra.mp4", "video/mp4", "video-de-prueba".getBytes());

        String referencia = almacenamiento.guardar(archivo, PrefijoAlmacenamiento.PORTAFOLIO);

        assertThat(referencia).startsWith("portafolio/").endsWith(".mp4");
        assertThat(new String(almacenamiento.leer(referencia))).isEqualTo("video-de-prueba");
    }

    @Test
    void guardar_prefijosDistintos_noSePisanEntreCasosDeUso() {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "doc.pdf", "application/pdf", "contenido".getBytes());

        String enPortafolio = almacenamiento.guardar(archivo, PrefijoAlmacenamiento.PORTAFOLIO);
        String enEntregables = almacenamiento.guardar(archivo, PrefijoAlmacenamiento.ENTREGABLES);

        assertThat(enPortafolio).isNotEqualTo(enEntregables);
        assertThat(almacenamiento.leer(enPortafolio)).isNotEmpty();
        assertThat(almacenamiento.leer(enEntregables)).isNotEmpty();
    }

    @Test
    void guardar_prefijoConCaracteresNoPermitidos_esRechazado() {
        MockMultipartFile archivo = new MockMultipartFile(
                "archivo", "doc.pdf", "application/pdf", "contenido".getBytes());

        assertThrows(ExcepcionReglaNegocio.class, () -> almacenamiento.guardar(archivo, "../escape"));
    }

    /** El almacenamiento local no firma URLs; el consumidor debe servir los bytes. */
    @Test
    void urlTemporal_noEstaDisponibleEnLocal() {
        String referencia = almacenamiento.guardar(new MockMultipartFile(
                "documento", "cedula.jpg", "image/jpeg", "contenido".getBytes()));

        assertThat(almacenamiento.urlTemporal(referencia)).isEmpty();
    }
}
