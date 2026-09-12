package uteq.edu.ec.artisync.service.perfil.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.dto.peticion.perfil.CreatePortfolioItemRequest;
import uteq.edu.ec.artisync.dto.respuesta.perfil.PortfolioItemResponse;
import uteq.edu.ec.artisync.entity.perfil.Portfolio;
import uteq.edu.ec.artisync.entity.perfil.PortfolioItem;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.perfil.PortfolioItemRepository;
import uteq.edu.ec.artisync.repository.perfil.PortfolioRepository;
import uteq.edu.ec.artisync.service.perfil.IPortfolioItemService;
import uteq.edu.ec.artisync.service.shared.almacenamiento.DocumentStorage;
import uteq.edu.ec.artisync.service.shared.almacenamiento.FileExtensions;
import uteq.edu.ec.artisync.service.shared.almacenamiento.FilePolicy;
import uteq.edu.ec.artisync.service.shared.almacenamiento.StoragePrefix;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioItemServiceImpl implements IPortfolioItemService {

    /**
     * Tope de obras por portafolio. El almacenamiento se factura por GB, y sin
     * un límite una sola cuenta puede subir video hasta agotar el presupuesto.
     */
    private static final long MAX_ITEMS_POR_PORTAFOLIO = 50;

    private final PortfolioItemRepository itemRepository;
    private final PortfolioRepository portafolioRepository;
    private final DocumentStorage almacenamiento;

    /**
     * Sube una obra al portafolio, hasta el tope de {@value #MAX_ITEMS_POR_PORTAFOLIO} por portafolio.
     *
     * @param idPortafolio identificador del portafolio
     * @param idUsuario identificador de quien sube; debe ser dueño del portafolio
     * @param peticion título y descripción de la obra
     * @param archivo archivo multimedia de la obra, validado contra {@code FilePolicy.PORTAFOLIO}
     * @return la obra creada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el portafolio no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si quien sube no es dueño del
     *         portafolio, si ya alcanzó el máximo de obras, o si el archivo no cumple la política de tipo/tamaño
     */
    @Override
    @Transactional
    public PortfolioItemResponse subirItem(Long idPortafolio, Long idUsuario,
                                              CreatePortfolioItemRequest peticion, MultipartFile archivo) {
        FilePolicy.PORTAFOLIO.validar(archivo);

        Portfolio portafolio = portafolioRepository.findById(idPortafolio)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Portfolio no encontrado con ID: " + idPortafolio));

        exigirPropietario(portafolio, idUsuario);

        if (itemRepository.countByPortafolioIdPortafolio(idPortafolio) >= MAX_ITEMS_POR_PORTAFOLIO) {
            throw new BusinessRuleException(
                    "El portafolio alcanzó el máximo de " + MAX_ITEMS_POR_PORTAFOLIO + " obras.");
        }

        String referencia = almacenamiento.guardar(archivo, StoragePrefix.PORTAFOLIO);

        PortfolioItem item = PortfolioItem.builder()
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
    /**
     * @param idPortafolio identificador del portafolio
     * @param idUsuario identificador de quien consulta; solo relevante si el portafolio es privado
     * @return las obras del portafolio, más recientes primero
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el portafolio no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el portafolio no es público
     *         y quien consulta no es su dueño
     */
    public List<PortfolioItemResponse> listarItems(Long idPortafolio, Long idUsuario) {
        Portfolio portafolio = portafolioRepository.findById(idPortafolio)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Portfolio no encontrado con ID: " + idPortafolio));

        exigirVisibilidad(portafolio, idUsuario);

        return itemRepository.findByPortafolioIdPortafolioOrderByFechaSubidaDesc(idPortafolio)
                .stream()
                .map(this::mapear)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * @param idItem identificador de la obra
     * @param idUsuario identificador de quien consulta; solo relevante si el portafolio es privado
     * @return la obra solicitada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la obra no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el portafolio no es público
     *         y quien consulta no es su dueño
     */
    public PortfolioItemResponse obtenerItem(Long idItem, Long idUsuario) {
        PortfolioItem item = buscarItem(idItem);
        exigirVisibilidad(item.getPortafolio(), idUsuario);
        return mapear(item);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * @param idItem identificador de la obra
     * @param idUsuario identificador de quien descarga; solo relevante si el portafolio es privado
     * @return el archivo de la obra, listo para transmitirse en streaming
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la obra no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el portafolio no es público
     *         y quien descarga no es su dueño
     */
    public ArchivoItem descargarArchivo(Long idItem, Long idUsuario) {
        PortfolioItem item = buscarItem(idItem);
        exigirVisibilidad(item.getPortafolio(), idUsuario);

        String referencia = item.getUrlArchivoMultimedia();
        return new ArchivoItem(
                almacenamiento.leer(referencia),
                "obra-" + idItem + extensionDe(referencia),
                FileExtensions.contentTypeDe(referencia));
    }

    @Override
    @Transactional
    /**
     * Actualiza el título y la descripción de una obra propia.
     *
     * @param idItem identificador de la obra
     * @param idUsuario identificador de quien edita; debe ser dueño del portafolio
     * @param peticion nuevo título y descripción
     * @return la obra ya actualizada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la obra no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si quien edita no es dueño del portafolio
     */
    public PortfolioItemResponse actualizarItem(Long idItem, Long idUsuario, CreatePortfolioItemRequest peticion) {
        PortfolioItem item = buscarItem(idItem);
        exigirPropietario(item.getPortafolio(), idUsuario);

        item.setTituloObra(peticion.tituloObra());
        item.setDescripcionObra(peticion.descripcionObra());
        item = itemRepository.save(item);

        log.info("Obra {} actualizada en el portafolio {}", idItem, item.getPortafolio().getIdPortafolio());
        return mapear(item);
    }

    @Override
    @Transactional
    /**
     * Elimina una obra propia, junto con su archivo en el almacenamiento.
     *
     * @param idItem identificador de la obra a eliminar
     * @param idUsuario identificador de quien elimina; debe ser dueño del portafolio
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la obra no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si quien elimina no es dueño del portafolio
     */
    public void eliminarItem(Long idItem, Long idUsuario) {
        PortfolioItem item = buscarItem(idItem);
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
    private void exigirPropietario(Portfolio portafolio, Long idUsuario) {
        Long idDuenio = portafolio.getPerfil().getUsuario().getIdUsuario();
        if (!idDuenio.equals(idUsuario)) {
            throw new BusinessRuleException("Solo el creador dueño del portafolio puede modificarlo.");
        }
    }

    /** Un portafolio privado solo lo ve su dueño; uno público, cualquiera. */
    private void exigirVisibilidad(Portfolio portafolio, Long idUsuario) {
        // REQ-NF-018 (ajuste de seguimiento): una cuenta desactivada (soft-delete
        // o supresión real) nunca es visible, sin importar esPublico ni idUsuario
        // — el propio dueño, si su cuenta está desactivada, tampoco puede haberse
        // autenticado para pedirlo con su propio id.
        User duenio = portafolio.getPerfil().getUsuario();
        if (duenio == null || !Boolean.TRUE.equals(duenio.getEstadoCuenta())) {
            throw new BusinessRuleException("Este portafolio no es público.");
        }

        if (Boolean.TRUE.equals(portafolio.getEsPublico())) {
            return;
        }
        Long idDuenio = duenio.getIdUsuario();
        if (idUsuario == null || !idDuenio.equals(idUsuario)) {
            throw new BusinessRuleException("Este portafolio no es público.");
        }
    }

    // ── Auxiliares ───────────────────────────────────────────────────────────

    private PortfolioItem buscarItem(Long idItem) {
        return itemRepository.findById(idItem)
                .orElseThrow(() -> new ResourceNotFoundException("Obra no encontrada con ID: " + idItem));
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

    private PortfolioItemResponse mapear(PortfolioItem item) {
        String referencia = item.getUrlArchivoMultimedia();
        // Con Azure el video viaja directo desde el blob; sin SAS, por el backend.
        String url = almacenamiento.urlTemporal(referencia)
                .orElse("/api/v1/portafolios/items/" + item.getIdItemPortafolio() + "/archivo");

        return PortfolioItemResponse.builder()
                .idItemPortafolio(item.getIdItemPortafolio())
                .idPortafolio(item.getPortafolio().getIdPortafolio())
                .tituloObra(item.getTituloObra())
                .descripcionObra(item.getDescripcionObra())
                .urlArchivo(url)
                .fechaSubida(item.getFechaSubida())
                .build();
    }
}
