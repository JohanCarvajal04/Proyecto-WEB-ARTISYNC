package uteq.edu.ec.artisync.service.shared.almacenamiento;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import uteq.edu.ec.artisync.config.AlmacenamientoProperties;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Cómo queda cableado el almacenamiento según haya o no credenciales de Azure.
 *
 * <p>Son las dos situaciones que de verdad importan y que ninguna prueba
 * cubría: que el backend arranque sin Azure —CI y desarrollo no tienen
 * credenciales ni emulador— y que con credenciales los dos backends convivan
 * en lugar de excluirse.
 */
class AlmacenamientoCableadoTest {

    /** Cuenta ficticia con formato válido: construir el cliente no abre conexión. */
    private static final String CONEXION_FICTICIA =
            "DefaultEndpointsProtocol=https;AccountName=cuentaficticia;"
                    + "AccountKey=bGxhdmVGaWN0aWNpYVBhcmFQcnVlYmFzMTIzNDU2Nzg5MA==;"
                    + "EndpointSuffix=core.windows.net";

    @TempDir
    Path directorioTemporal;

    @Configuration
    @EnableConfigurationProperties(AlmacenamientoProperties.class)
    @Import({AlmacenamientoLocal.class, AlmacenamientoAzure.class, AlmacenamientoRouter.class})
    static class Almacenamiento {
    }

    private ApplicationContextRunner runner() {
        return new ApplicationContextRunner()
                .withUserConfiguration(Almacenamiento.class)
                .withPropertyValues("documentos.ruta-base=" + directorioTemporal);
    }

    @Test
    void sinCadenaDeConexion_elContextoArrancaYAzureNoSeRegistra() {
        runner().run(contexto -> {
            assertThat(contexto).hasNotFailed();
            assertThat(contexto).doesNotHaveBean(AlmacenamientoAzure.class);
            assertThat(contexto).hasSingleBean(AlmacenamientoLocal.class);
        });
    }

    /** Una cadena en blanco es lo que produce la variable de entorno sin definir. */
    @Test
    void cadenaEnBlanco_seTrataComoAusente() {
        runner().withPropertyValues("documentos.azure.connection-string=   ").run(contexto -> {
            assertThat(contexto).hasNotFailed();
            assertThat(contexto).doesNotHaveBean(AlmacenamientoAzure.class);
        });
    }

    @Test
    void conCadenaDeConexion_ambosBackendsConviven() {
        runner().withPropertyValues("documentos.azure.connection-string=" + CONEXION_FICTICIA)
                .run(contexto -> {
                    assertThat(contexto).hasNotFailed();
                    assertThat(contexto).hasSingleBean(AlmacenamientoAzure.class);
                    assertThat(contexto).hasSingleBean(AlmacenamientoLocal.class);
                });
    }

    /** Los servicios inyectan la interfaz: debe llegarles el router, no un backend suelto. */
    @Test
    void elBeanPrimarioEsSiempreElRouter() {
        runner().withPropertyValues("documentos.azure.connection-string=" + CONEXION_FICTICIA)
                .run(contexto -> assertThat(contexto.getBean(AlmacenamientoDocumentos.class))
                        .isInstanceOf(AlmacenamientoRouter.class));

        runner().run(contexto -> assertThat(contexto.getBean(AlmacenamientoDocumentos.class))
                .isInstanceOf(AlmacenamientoRouter.class));
    }

    @Test
    void losPrefijosLocalesSonConfigurables() {
        runner().withPropertyValues("documentos.prefijos-locales=verificacion,entregables")
                .run(contexto -> assertThat(contexto.getBean(AlmacenamientoProperties.class)
                        .getPrefijosLocales()).containsExactlyInAnyOrder("verificacion", "entregables"));
    }
}
