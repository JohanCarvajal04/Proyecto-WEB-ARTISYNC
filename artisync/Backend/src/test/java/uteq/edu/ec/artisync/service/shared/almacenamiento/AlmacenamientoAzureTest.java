package uteq.edu.ec.artisync.service.shared.almacenamiento;

import org.junit.jupiter.api.Test;
import uteq.edu.ec.artisync.config.AlmacenamientoProperties;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Pruebas que no requieren una cuenta de Azure. Cubren lo que se puede
 * verificar sin red: el fallo temprano de configuración y el rechazo de
 * referencias inválidas antes de intentar cualquier llamada al servicio.
 * La subida y descarga reales se prueban con el emulador Azurite.
 */
class AlmacenamientoAzureTest {

    /** Cuenta ficticia con formato válido: construir el cliente no abre conexión. */
    private static final String CONEXION_FICTICIA =
            "DefaultEndpointsProtocol=https;AccountName=cuentaficticia;"
                    + "AccountKey=bGxhdmVGaWN0aWNpYVBhcmFQcnVlYmFzMTIzNDU2Nzg5MA==;"
                    + "EndpointSuffix=core.windows.net";

    private AlmacenamientoAzure almacenamientoConCuentaFicticia() {
        AlmacenamientoProperties propiedades = new AlmacenamientoProperties();
        propiedades.setProveedor("azure");
        propiedades.getAzure().setConnectionString(CONEXION_FICTICIA);
        propiedades.getAzure().setContenedor("documentos-prueba");
        return new AlmacenamientoAzure(propiedades);
    }

    @Test
    void constructor_sinCadenaDeConexion_fallaAlArrancar() {
        AlmacenamientoProperties propiedades = new AlmacenamientoProperties();
        propiedades.setProveedor("azure");

        IllegalStateException error = assertThrows(IllegalStateException.class,
                () -> new AlmacenamientoAzure(propiedades));

        assertThat(error).hasMessageContaining("connection-string");
    }

    @Test
    void constructor_conCadenaValida_noAbreConexionDeRed() {
        assertThat(almacenamientoConCuentaFicticia()).isNotNull();
    }

    @Test
    void leer_referenciaQueIntentaEscapar_esRechazadaAntesDeLlamarAAzure() {
        AlmacenamientoAzure almacenamiento = almacenamientoConCuentaFicticia();

        assertThrows(ExcepcionReglaNegocio.class, () -> almacenamiento.leer("../otro-contenedor/secreto.jpg"));
        assertThrows(ExcepcionReglaNegocio.class, () -> almacenamiento.leer("/absoluto.jpg"));
        assertThrows(ExcepcionReglaNegocio.class, () -> almacenamiento.leer("  "));
        assertThrows(ExcepcionReglaNegocio.class, () -> almacenamiento.leer(null));
    }

    @Test
    void eliminar_referenciaInvalida_esRechazadaAntesDeLlamarAAzure() {
        AlmacenamientoAzure almacenamiento = almacenamientoConCuentaFicticia();

        assertThrows(ExcepcionReglaNegocio.class, () -> almacenamiento.eliminar("../passwd"));
    }
}
