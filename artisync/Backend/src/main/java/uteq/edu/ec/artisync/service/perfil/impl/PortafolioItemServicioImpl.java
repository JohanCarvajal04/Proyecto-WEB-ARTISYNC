package uteq.edu.ec.artisync.service.perfil.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionCrearPortafolioItem;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaPortafolioItem;
import uteq.edu.ec.artisync.entity.perfil.Portafolio;
import uteq.edu.ec.artisync.entity.perfil.PortafolioItem;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.perfil.PortafolioItemRepository;
import uteq.edu.ec.artisync.repository.perfil.PortafolioRepository;
import uteq.edu.ec.artisync.service.perfil.IPortafolioItemServicio;
import uteq.edu.ec.artisync.service.shared.almacenamiento.AlmacenamientoDocumentos;
import uteq.edu.ec.artisync.service.shared.almacenamiento.ExtensionesArchivo;
import uteq.edu.ec.artisync.service.shared.almacenamiento.PoliticaArchivo;
import uteq.edu.ec.artisync.service.shared.almacenamiento.PrefijoAlmacenamiento;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortafolioItemServicioImpl implements IPortafolioItemServicio {

    /**
     * Tope de obras por portafolio. El almacenamiento se factura por GB, y sin
     * un límite una sola cuenta puede subir video hasta agotar el presupuesto.
     */
    private static final long MAX_ITEMS_POR_PORTAFOLIO = 50;

    private final PortafolioItemRepository itemRepository;
    private final PortafolioRepository portafolioRepository;
    private final AlmacenamientoDocumentos almacenamiento;

    @Override
    @Transactional
    public RespuestaPortafolioItem subirItem(Long idPortafolio, Long idUsuario,
                                              PeticionCrearPortafolioItem peticion, MultipartFile archivo) {
        PoliticaArchivo.PORTAFOLIO.validar(archivo);

        Portafolio portafolio = portafolioRepository.findById(idPortafolio)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "Portafolio no encontrado con ID: " + idPortafolio));

        exigirPropietario(portafolio, idUsuario);

        if (itemRepository.countByPortafolioIdPortafolio(idPortafolio) >= MAX_ITEMS_POR_PORTAFOLIO) {
            throw new ExcepcionReglaNegocio(
                    "El portafolio alcanzó el máximo de " + MAX_ITEMS_POR_PORTAFOLIO + " obras.");
        }

        String referencia = almacenamiento.guardar(archivo, PrefijoAlmacenamiento.PORTAFOLIO);

        PortafolioItem item = PortafolioItem.builder()
                .portafolio(portafolio)
                .tituloObra(peticion.tituloObra())
                .descripcionObra(peticion.descripcionObra())
                .urlArchivoMultimedia(referencia)
                .build();

        try {
            item = itemRepository.save(item);
        } catch (RuntimeException e) {
            // Sin esto el archivo queda subido y facturándose sin fila que lo apunte.
            eliminarSilencioso(referencia);
            throw e;
        }

        log.info("Obra {} subida al portafolio {}", item.getIdItemPortafolio(), idPortafolio);
        return mapear(item);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaPortafolioItem> listarItems(Long idPortafolio, Long idUsuario) {
        Portafolio portafolio = portafolioRepository.findById(idPortafolio)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "Portafolio no encontrado con ID: " + idPortafolio));

        exigirVisibilidad(portafolio, idUsuario);

        return itemRepository.findByPortafolioIdPortafolioOrderByFechaSubidaDesc(idPortafolio)
                .stream()
                .map(this::mapear)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaPortafolioItem obtenerItem(Long idItem, Long idUsuario) {
        PortafolioItem item = buscarItem(idItem);
        exigirVisibilidad(item.getPortafolio(), idUsuario);
        return mapear(item);
    }

    @Override
    @Transactional(readOnly = true)
    public ArchivoItem descargarArchivo(Long idItem, Long idUsuario) {
        PortafolioItem item = buscarItem(idItem);
        exigirVisibilidad(item.getPortafolio(), idUsuario);

        String referencia = item.getUrlArchivoMultimedia();
        return new ArchivoItem(
                almacenamiento.leer(referencia),
                "obra-" + idItem + extensionDe(referencia),
                ExtensionesArchivo.contentTypeDe(referencia));
    }

    @Override
    @Transactional
    public RespuestaPortafolioItem actualizarItem(Long idItem, Long idUsuario, PeticionCrearPortafolioItem peticion) {
        PortafolioItem item = buscarItem(idItem);
        exigirPropietario(item.getPortafolio(), idUsuario);

        item.setTituloObra(peticion.tituloObra());
        item.setDescripcionObra(peticion.descripcionObra());
        item = itemRepository.save(item);

        log.info("Obra {} actualizada en el portafolio {}", idItem, item.getPortafolio().getIdPortafolio());
        return mapear(item);
    }

    @Override
    @Transactional
    public void eliminarItem(Long idItem, Long idUsuario) {
        PortafolioItem item = buscarItem(idItem);
        exigirPropietario(item.getPortafolio(), idUsuario);

        String referencia = item.getUrlArchivoMultimedia();
        itemRepository.delete(item);
        // Se borra después de la fila: si falla, queda un huérfano en el
        // almacenamiento, que es preferible a una fila apuntando a la nada.
        eliminarSilencioso(referencia);

        log.info("Obra {} eliminada del portafolio {}", idItem, item.getPortafolio().getIdPortafolio());
    }

    // ── Autorización ─────────────────────────────────────────────────────────

    /** Solo el creador dueño del portafolio puede modificarlo. */
    private void exigirPropietario(Portafolio portafolio, Long idUsuario) {
        Long idDuenio = portafolio.getPerfil().getUsuario().getIdUsuario();
        if (!idDuenio.equals(idUsuario)) {
            throw new ExcepcionReglaNegocio("Solo el creador dueño del portafolio puede modificarlo.");
        }
    }

    /** Un portafolio privado solo lo ve su dueño; uno público, cualquiera. */
    private void exigirVisibilidad(Portafolio portafolio, Long idUsuario) {
        if (Boolean.TRUE.equals(portafolio.getEsPublico())) {
            return;
        }
        Long idDuenio = portafolio.getPerfil().getUsuario().getIdUsuario();
        if (idUsuario == null || !idDuenio.equals(idUsuario)) {
            throw new ExcepcionReglaNegocio("Este portafolio no es público.");
        }
    }

    // ── Auxiliares ───────────────────────────────────────────────────────────

    private PortafolioItem buscarItem(Long idItem) {
        return itemRepository.findById(idItem)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Obra no encontrada con ID: " + idItem));
    }

    private void eliminarSilencioso(String referencia) {
        if (referencia == null || referencia.isBlank()) {
            return;
        }
        try {
            almacenamiento.eliminar(referencia);
        } catch (RuntimeException e) {
            log.warn("No se pudo eliminar el archivo {}: {}", referencia, e.getMessage());
        }
    }

    private String extensionDe(String referencia) {
        int punto = referencia == null ? -1 : referencia.lastIndexOf('.');
        return punto < 0 ? "" : referencia.substring(punto);
    }

    private RespuestaPortafolioItem mapear(PortafolioItem item) {
        String referencia = item.getUrlArchivoMultimedia();
        // Con Azure el video viaja directo desde el blob; sin SAS, por el backend.
        String url = almacenamiento.urlTemporal(referencia)
                .orElse("/api/v1/portafolios/items/" + item.getIdItemPortafolio() + "/archivo");

        return RespuestaPortafolioItem.builder()
                .idItemPortafolio(item.getIdItemPortafolio())
                .idPortafolio(item.getPortafolio().getIdPortafolio())
                .tituloObra(item.getTituloObra())
                .descripcionObra(item.getDescripcionObra())
                .urlArchivo(url)
                .fechaSubida(item.getFechaSubida())
                .build();
    }
}
