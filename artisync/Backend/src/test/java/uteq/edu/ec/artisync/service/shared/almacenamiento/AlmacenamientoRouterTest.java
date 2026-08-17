package uteq.edu.ec.artisync.service.shared.almacenamiento;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mock.web.MockMultipartFile;
import uteq.edu.ec.artisync.config.AlmacenamientoProperties;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * El reparto entre volumen local y Azure. Lo que se prueba aquí no es que cada
 * backend funcione —eso lo cubren sus propias pruebas— sino que cada archivo
 * acaba en el lado correcto, incluidas las referencias heredadas.
 */
class AlmacenamientoRouterTest {

    private final AlmacenamientoLocal local = mock(AlmacenamientoLocal.class);
    private final AlmacenamientoAzure azure = mock(AlmacenamientoAzure.class);

    @SuppressWarnings("unchecked")
    private AlmacenamientoRouter router(boolean conAzure) {
        ObjectProvider<AlmacenamientoAzure> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(conAzure ? azure : null);

        AlmacenamientoProperties propiedades = new AlmacenamientoProperties();
        propiedades.setPrefijosLocales(List.of("verificacion"));
        return new AlmacenamientoRouter(local, provider, propiedades);
    }

    private MockMultipartFile archivo() {
        return new MockMultipartFile("archivo", "x.png", "image/png", "bytes".getBytes());
    }

    // ── Escritura ────────────────────────────────────────────────────────────

    @Test
    void guardar_prefijoDeVerificacion_vaAlVolumenLocal() {
        router(true).guardar(archivo(), PrefijoAlmacenamiento.VERIFICACION);

        verify(local).guardar(any(), eq("verificacion"));
        verifyNoInteractions(azure);
    }

    @Test
    void guardar_prefijosNoLocales_vanAAzure() {
        AlmacenamientoRouter router = router(true);

        router.guardar(archivo(), PrefijoAlmacenamiento.PORTAFOLIO);
        router.guardar(archivo(), PrefijoAlmacenamiento.ENTREGABLES);

        verify(azure).guardar(any(), eq("portafolio"));
        verify(azure).guardar(any(), eq("entregables"));
        verify(local, never()).guardar(any(), anyString());
    }

    /** Sin credenciales de Azure el backend debe seguir aceptando subidas. */
    @Test
    void guardar_sinAzureConfigurado_todoCaeAlVolumenLocal() {
        router(false).guardar(archivo(), PrefijoAlmacenamiento.PORTAFOLIO);

        verify(local).guardar(any(), eq("portafolio"));
    }

    /** El overload deprecado no puede decidir, así que se queda en local. */
    @Test
    void guardarSinPrefijo_vaAlVolumenLocalYNoAAzure() {
        router(true).guardar(archivo());

        verify(local).guardar(any());
        verifyNoInteractions(azure);
    }

    // ── Lectura, borrado y URL ───────────────────────────────────────────────

    @Test
    void leer_referenciaConPrefijoLocal_consultaElVolumen() {
        router(true).leer("verificacion/abc.jpg");

        verify(local).leer("verificacion/abc.jpg");
        verifyNoInteractions(azure);
    }

    @Test
    void leer_referenciaConPrefijoRemoto_consultaAzure() {
        router(true).leer("portafolio/abc.mp4");

        verify(azure).leer("portafolio/abc.mp4");
        verify(local, never()).leer(anyString());
    }

    /**
     * Verificación guardó sin prefijo antes de que existiera el enrutado, y esos
     * archivos están en el volumen. Mandarlos a Azure devolvería 404 a cada
     * cédula ya subida.
     */
    @Test
    void leer_referenciaHeredadaSinPrefijo_consultaElVolumenYNoAzure() {
        router(true).leer("11111111-2222-3333-4444-555555555555.jpg");

        verify(local).leer("11111111-2222-3333-4444-555555555555.jpg");
        verifyNoInteractions(azure);
    }

    /** El scheduler purga a los 30 días: debe borrar donde el archivo está. */
    @Test
    void eliminar_referenciaHeredadaSinPrefijo_borraDelVolumen() {
        router(true).eliminar("abc.jpg");

        verify(local).eliminar("abc.jpg");
        verifyNoInteractions(azure);
    }

    @Test
    void eliminar_referenciaDeEntregable_borraDeAzure() {
        router(true).eliminar("entregables/abc.pdf");

        verify(azure).eliminar("entregables/abc.pdf");
        verify(local, never()).eliminar(anyString());
    }

    @Test
    void urlTemporal_referenciaDeAzure_delegaLaFirma() {
        when(azure.urlTemporal("portafolio/a.mp4")).thenReturn(Optional.of("https://blob/a?sig=x"));

        assertThat(router(true).urlTemporal("portafolio/a.mp4")).contains("https://blob/a?sig=x");
    }

    /** El volumen no firma URLs: el consumidor debe caer a servir los bytes. */
    @Test
    void urlTemporal_referenciaLocal_devuelveVacio() {
        when(local.urlTemporal("verificacion/a.jpg")).thenReturn(Optional.empty());

        assertThat(router(true).urlTemporal("verificacion/a.jpg")).isEmpty();
    }

    // ── Extracción de prefijo ────────────────────────────────────────────────

    @Test
    void extraer_devuelveElPrefijoOVacioCuandoNoLoHay() {
        assertThat(PrefijoAlmacenamiento.extraer("portafolio/a.mp4")).isEqualTo("portafolio");
        assertThat(PrefijoAlmacenamiento.extraer("a.jpg")).isEmpty();
        assertThat(PrefijoAlmacenamiento.extraer(null)).isEmpty();
        // Una barra inicial no es un prefijo: la referencia es inválida y el
        // backend la rechaza, pero extraer() no debe inventarse uno vacío válido.
        assertThat(PrefijoAlmacenamiento.extraer("/a.jpg")).isEmpty();
    }
}
