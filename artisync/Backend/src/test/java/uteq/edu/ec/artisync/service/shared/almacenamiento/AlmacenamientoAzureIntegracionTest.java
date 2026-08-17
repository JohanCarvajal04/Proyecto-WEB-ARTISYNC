package uteq.edu.ec.artisync.service.shared.almacenamiento;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import uteq.edu.ec.artisync.config.AlmacenamientoProperties;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Roundtrip real contra Azurite, el emulador oficial de Azure Storage. Las
 * pruebas unitarias solo cubren validación y configuración: nada de eso prueba
 * que el SDK hable de verdad con el servicio, que es justo donde aparecen los
 * desajustes de versión del transporte HTTP.
 *
 * <p>Se salta si Azurite no está escuchando, para no romper CI ni la máquina de
 * quien no lo tenga levantado:
 * <pre>docker run -p 10000:10000 mcr.microsoft.com/azure-storage/azurite \
 *   azurite-blob --blobHost 0.0.0.0</pre>
 */
class AlmacenamientoAzureIntegracionTest {

    /** Cuenta y llave públicas y fijas del emulador; no son un secreto. */
    private static final String CONEXION_AZURITE =
            "DefaultEndpointsProtocol=http;AccountName=devstoreaccount1;"
                    + "AccountKey=Eby8vdM02xNOcqFlqUwJPLlmEtlCDXJ1OUzFT50uSRZ6IFsuFq2UVErCz4I6tq/K1SZFPTOtr/KBHBeksoGMGw==;"
                    + "BlobEndpoint=http://127.0.0.1:10000/devstoreaccount1;";

    @BeforeAll
    static void requiereAzurite() {
        assumeTrue(azuriteDisponible(), "Azurite no está escuchando en 127.0.0.1:10000; prueba omitida.");
    }

    private static boolean azuriteDisponible() {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress("127.0.0.1", 10000), 1000);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private AlmacenamientoAzure almacenamiento() {
        AlmacenamientoProperties propiedades = new AlmacenamientoProperties();
        propiedades.getAzure().setConnectionString(CONEXION_AZURITE);
        // Un contenedor por ejecución evita que dos corridas se pisen.
        propiedades.getAzure().setContenedor("prueba-" + UUID.randomUUID());
        return new AlmacenamientoAzure(propiedades);
    }

    @Test
    void guardar_leer_eliminar_completaElCicloContraElServicio() {
        AlmacenamientoAzure azure = almacenamiento();
        byte[] contenido = "contenido de una cedula".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile archivo = new MockMultipartFile(
                "documento", "cedula.jpg", "image/jpeg", contenido);

        String referencia = azure.guardar(archivo);

        assertThat(referencia).endsWith(".jpg");
        assertThat(azure.leer(referencia)).isEqualTo(contenido);

        azure.eliminar(referencia);

        assertThrows(ExcepcionRecursoNoEncontrado.class, () -> azure.leer(referencia));
    }

    @Test
    void guardar_asignaLaExtensionSegunElTipoReal() {
        AlmacenamientoAzure azure = almacenamiento();

        String pdf = azure.guardar(new MockMultipartFile(
                "documento", "contrato.pdf", "application/pdf", "%PDF-1.4".getBytes(StandardCharsets.UTF_8)));
        String video = azure.guardar(new MockMultipartFile(
                "documento", "demo.mp4", "video/mp4", new byte[] {0, 0, 0, 24}));

        assertThat(pdf).endsWith(".pdf");
        assertThat(video).endsWith(".mp4");
    }

    @Test
    void urlTemporal_devuelveUnaUrlFirmadaHaciaElBlob() {
        AlmacenamientoAzure azure = almacenamiento();
        String referencia = azure.guardar(new MockMultipartFile(
                "documento", "titulo.png", "image/png", "png".getBytes(StandardCharsets.UTF_8)));

        String url = azure.urlTemporal(referencia).orElseThrow();

        assertThat(url).contains(referencia).contains("sig=").contains("se=");
    }

    @Test
    void guardar_conPrefijo_agrupaElBlobYSigueSiendoLegible() {
        AlmacenamientoAzure azure = almacenamiento();
        byte[] contenido = "obra de portafolio".getBytes(StandardCharsets.UTF_8);

        String referencia = azure.guardar(new MockMultipartFile(
                "archivo", "obra.mp4", "video/mp4", contenido), PrefijoAlmacenamiento.PORTAFOLIO);

        assertThat(referencia).startsWith("portafolio/").endsWith(".mp4");
        assertThat(azure.leer(referencia)).isEqualTo(contenido);
    }

    @Test
    void leer_referenciaInexistente_reportaRecursoNoEncontrado() {
        AlmacenamientoAzure azure = almacenamiento();

        assertThrows(ExcepcionRecursoNoEncontrado.class, () -> azure.leer("no-existe.jpg"));
    }
}
