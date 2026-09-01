package uteq.edu.ec.artisync.service.perfil.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionCrearPortafolioItem;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaPortafolioItem;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.perfil.Portafolio;
import uteq.edu.ec.artisync.entity.perfil.PortafolioItem;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.perfil.PortafolioItemRepository;
import uteq.edu.ec.artisync.repository.perfil.PortafolioRepository;
import uteq.edu.ec.artisync.service.perfil.IPortafolioItemServicio;
import uteq.edu.ec.artisync.service.shared.almacenamiento.AlmacenamientoDocumentos;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PortafolioItemServicioImplTest {

    private static final Long ID_PORTAFOLIO = 3L;
    private static final Long ID_ITEM = 11L;
    private static final Long ID_DUENIO = 1L;
    private static final Long ID_OTRO = 42L;

    @Mock private PortafolioItemRepository itemRepository;
    @Mock private PortafolioRepository portafolioRepository;
    @Mock private AlmacenamientoDocumentos almacenamiento;

    @InjectMocks private PortafolioItemServicioImpl servicio;

    private Portafolio portafolio;

    @BeforeEach
    void setUp() {
        Usuario duenio = new Usuario();
        duenio.setIdUsuario(ID_DUENIO);
        PerfilCreador perfil = new PerfilCreador();
        perfil.setUsuario(duenio);

        portafolio = Portafolio.builder()
                .idPortafolio(ID_PORTAFOLIO)
                .perfil(perfil)
                .esPublico(true)
                .build();
    }

    private PeticionCrearPortafolioItem datos() {
        return new PeticionCrearPortafolioItem("Mi obra", "Una descripcion");
    }

    private MockMultipartFile imagen() {
        return new MockMultipartFile("archivo", "obra.png", "image/png", "bytes".getBytes());
    }

    private PortafolioItem item(String referencia) {
        return PortafolioItem.builder()
                .idItemPortafolio(ID_ITEM)
                .portafolio(portafolio)
                .tituloObra("Mi obra")
                .urlArchivoMultimedia(referencia)
                .build();
    }

    // ── Subida ───────────────────────────────────────────────────────────────

    @Test
    void subirItem_guardaBajoElPrefijoDePortafolioYPersisteLaReferencia() {
        when(portafolioRepository.findById(ID_PORTAFOLIO)).thenReturn(Optional.of(portafolio));
        when(itemRepository.countByPortafolioIdPortafolio(ID_PORTAFOLIO)).thenReturn(0L);
        when(almacenamiento.guardar(any(), eq("portafolio"))).thenReturn("portafolio/abc.png");
        when(itemRepository.save(any())).thenAnswer(inv -> {
            PortafolioItem guardado = inv.getArgument(0);
            guardado.setIdItemPortafolio(ID_ITEM);
            return guardado;
        });
        when(almacenamiento.urlTemporal("portafolio/abc.png")).thenReturn(Optional.empty());

        RespuestaPortafolioItem respuesta = servicio.subirItem(
                ID_PORTAFOLIO, ID_DUENIO, datos(), imagen());

        verify(almacenamiento).guardar(any(), eq("portafolio"));
        assertThat(respuesta.tituloObra()).isEqualTo("Mi obra");
        assertThat(respuesta.urlArchivo()).isEqualTo("/api/v1/portafolios/items/11/archivo");
    }

    @Test
    void subirItem_usuarioQueNoEsElDuenio_esRechazadoSinSubirNada() {
        when(portafolioRepository.findById(ID_PORTAFOLIO)).thenReturn(Optional.of(portafolio));

        assertThrows(ExcepcionReglaNegocio.class,
                () -> servicio.subirItem(ID_PORTAFOLIO, ID_OTRO, datos(), imagen()));

        verify(almacenamiento, never()).guardar(any(), anyString());
    }

    @Test
    void subirItem_formatoNoPermitido_seRechazaAntesDeConsultarLaBase() {
        MockMultipartFile pdf = new MockMultipartFile(
                "archivo", "doc.pdf", "application/pdf", "%PDF".getBytes());

        assertThrows(ExcepcionReglaNegocio.class,
                () -> servicio.subirItem(ID_PORTAFOLIO, ID_DUENIO, datos(), pdf));

        verifyNoInteractions(portafolioRepository, itemRepository, almacenamiento);
    }

    @Test
    void subirItem_alAlcanzarElTope_esRechazado() {
        when(portafolioRepository.findById(ID_PORTAFOLIO)).thenReturn(Optional.of(portafolio));
        when(itemRepository.countByPortafolioIdPortafolio(ID_PORTAFOLIO)).thenReturn(50L);

        assertThrows(ExcepcionReglaNegocio.class,
                () -> servicio.subirItem(ID_PORTAFOLIO, ID_DUENIO, datos(), imagen()));

        verify(almacenamiento, never()).guardar(any(), anyString());
    }

    /** Sin esto el archivo queda subido y facturándose sin fila que lo apunte. */
    @Test
    void subirItem_siFallaGuardarLaFila_borraElArchivoYaSubido() {
        when(portafolioRepository.findById(ID_PORTAFOLIO)).thenReturn(Optional.of(portafolio));
        when(itemRepository.countByPortafolioIdPortafolio(ID_PORTAFOLIO)).thenReturn(0L);
        when(almacenamiento.guardar(any(), eq("portafolio"))).thenReturn("portafolio/huerfano.png");
        when(itemRepository.save(any())).thenThrow(new RuntimeException("fallo de base"));

        assertThrows(RuntimeException.class,
                () -> servicio.subirItem(ID_PORTAFOLIO, ID_DUENIO, datos(), imagen()));

        verify(almacenamiento).eliminar("portafolio/huerfano.png");
    }

    @Test
    void subirItem_portafolioInexistente_reportaRecursoNoEncontrado() {
        when(portafolioRepository.findById(ID_PORTAFOLIO)).thenReturn(Optional.empty());

        assertThrows(ExcepcionRecursoNoEncontrado.class,
                () -> servicio.subirItem(ID_PORTAFOLIO, ID_DUENIO, datos(), imagen()));
    }

    // ── Visibilidad ──────────────────────────────────────────────────────────

    @Test
    void listarItems_portafolioPublico_esVisibleParaAnonimos() {
        when(portafolioRepository.findById(ID_PORTAFOLIO)).thenReturn(Optional.of(portafolio));
        when(itemRepository.findByPortafolioIdPortafolioOrderByFechaSubidaDesc(ID_PORTAFOLIO))
                .thenReturn(List.of(item("portafolio/a.png")));
        when(almacenamiento.urlTemporal(anyString())).thenReturn(Optional.empty());

        assertThat(servicio.listarItems(ID_PORTAFOLIO, null)).hasSize(1);
    }

    @Test
    void listarItems_portafolioPrivadoYAnonimo_esRechazado() {
        portafolio.setEsPublico(false);
        when(portafolioRepository.findById(ID_PORTAFOLIO)).thenReturn(Optional.of(portafolio));

        assertThrows(ExcepcionReglaNegocio.class, () -> servicio.listarItems(ID_PORTAFOLIO, null));
    }

    @Test
    void listarItems_portafolioPrivadoYOtroUsuario_esRechazado() {
        portafolio.setEsPublico(false);
        when(portafolioRepository.findById(ID_PORTAFOLIO)).thenReturn(Optional.of(portafolio));

        assertThrows(ExcepcionReglaNegocio.class, () -> servicio.listarItems(ID_PORTAFOLIO, ID_OTRO));
    }

    @Test
    void listarItems_portafolioPrivadoYSuDuenio_siLoVe() {
        portafolio.setEsPublico(false);
        when(portafolioRepository.findById(ID_PORTAFOLIO)).thenReturn(Optional.of(portafolio));
        when(itemRepository.findByPortafolioIdPortafolioOrderByFechaSubidaDesc(ID_PORTAFOLIO))
                .thenReturn(List.of(item("portafolio/a.png")));
        when(almacenamiento.urlTemporal(anyString())).thenReturn(Optional.empty());

        assertThat(servicio.listarItems(ID_PORTAFOLIO, ID_DUENIO)).hasSize(1);
    }

    // ── Descarga ─────────────────────────────────────────────────────────────

    @Test
    void descargarArchivo_devuelveBytesYContentTypeSegunLaExtension() {
        when(itemRepository.findById(ID_ITEM)).thenReturn(Optional.of(item("portafolio/obra.mp4")));
        when(almacenamiento.leer("portafolio/obra.mp4")).thenReturn("video".getBytes());

        IPortafolioItemServicio.ArchivoItem archivo = servicio.descargarArchivo(ID_ITEM, null);

        assertThat(archivo.contenido()).isEqualTo("video".getBytes());
        assertThat(archivo.contentType()).isEqualTo("video/mp4");
        assertThat(archivo.nombreSugerido()).isEqualTo("obra-11.mp4");
    }

    @Test
    void descargarArchivo_dePortafolioPrivadoAjeno_esRechazado() {
        portafolio.setEsPublico(false);
        when(itemRepository.findById(ID_ITEM)).thenReturn(Optional.of(item("portafolio/obra.mp4")));

        assertThrows(ExcepcionReglaNegocio.class, () -> servicio.descargarArchivo(ID_ITEM, ID_OTRO));

        verify(almacenamiento, never()).leer(anyString());
    }

    // ── Edición ──────────────────────────────────────────────────────────────

    @Test
    void actualizarItem_porElDuenio_cambiaTituloYDescripcion() {
        PortafolioItem existente = item("portafolio/obra.png");
        when(itemRepository.findById(ID_ITEM)).thenReturn(Optional.of(existente));
        when(itemRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(almacenamiento.urlTemporal("portafolio/obra.png")).thenReturn(Optional.empty());

        RespuestaPortafolioItem respuesta = servicio.actualizarItem(
                ID_ITEM, ID_DUENIO, new PeticionCrearPortafolioItem("Nuevo título", "Nueva descripción"));

        assertThat(respuesta.tituloObra()).isEqualTo("Nuevo título");
        assertThat(respuesta.descripcionObra()).isEqualTo("Nueva descripción");
    }

    @Test
    void actualizarItem_porQuienNoEsElDuenio_esRechazado() {
        when(itemRepository.findById(ID_ITEM)).thenReturn(Optional.of(item("portafolio/obra.png")));

        assertThrows(ExcepcionReglaNegocio.class,
                () -> servicio.actualizarItem(ID_ITEM, ID_OTRO, datos()));

        verify(itemRepository, never()).save(any());
    }

    // ── Eliminacion ──────────────────────────────────────────────────────────

    @Test
    void eliminarItem_borraLaFilaYElArchivo() {
        PortafolioItem existente = item("portafolio/obra.png");
        when(itemRepository.findById(ID_ITEM)).thenReturn(Optional.of(existente));

        servicio.eliminarItem(ID_ITEM, ID_DUENIO);

        verify(itemRepository).delete(existente);
        verify(almacenamiento).eliminar("portafolio/obra.png");
    }

    @Test
    void eliminarItem_porQuienNoEsElDuenio_esRechazado() {
        when(itemRepository.findById(ID_ITEM)).thenReturn(Optional.of(item("portafolio/obra.png")));

        assertThrows(ExcepcionReglaNegocio.class, () -> servicio.eliminarItem(ID_ITEM, ID_OTRO));

        verify(itemRepository, never()).delete(any());
        verify(almacenamiento, never()).eliminar(anyString());
    }

    /** Un huérfano en el almacenamiento es preferible a fallar el borrado entero. */
    @Test
    void eliminarItem_siFallaBorrarElArchivo_laFilaIgualSeElimina() {
        PortafolioItem existente = item("portafolio/obra.png");
        when(itemRepository.findById(ID_ITEM)).thenReturn(Optional.of(existente));
        doThrow(new ExcepcionReglaNegocio("Azure caido")).when(almacenamiento).eliminar(anyString());

        servicio.eliminarItem(ID_ITEM, ID_DUENIO);

        verify(itemRepository).delete(existente);
    }

    // ── Respuesta ────────────────────────────────────────────────────────────

    @Test
    void mapear_conProveedorQueFirmaUrls_devuelveElSasEnLugarDeLaRutaDelBackend() {
        when(itemRepository.findById(ID_ITEM)).thenReturn(Optional.of(item("portafolio/obra.png")));
        when(almacenamiento.urlTemporal("portafolio/obra.png"))
                .thenReturn(Optional.of("https://cuenta.blob.core.windows.net/c/portafolio/obra.png?sig=z"));

        RespuestaPortafolioItem respuesta = servicio.obtenerItem(ID_ITEM, null);

        assertThat(respuesta.urlArchivo()).startsWith("https://").contains("sig=");
    }
}
