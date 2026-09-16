package uteq.edu.ec.artisync.service.profile.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.dto.request.profile.CreatePortfolioItemRequest;
import uteq.edu.ec.artisync.dto.response.profile.PortfolioItemResponse;
import uteq.edu.ec.artisync.entity.profile.Portfolio;
import uteq.edu.ec.artisync.entity.profile.PortfolioItem;
import uteq.edu.ec.artisync.entity.security.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.profile.PortfolioItemRepository;
import uteq.edu.ec.artisync.repository.profile.PortfolioRepository;
import uteq.edu.ec.artisync.service.profile.IPortfolioItemService;
import uteq.edu.ec.artisync.service.shared.storage.DocumentStorage;
import uteq.edu.ec.artisync.service.shared.storage.FileExtensions;
import uteq.edu.ec.artisync.service.shared.storage.FilePolicy;
import uteq.edu.ec.artisync.service.shared.storage.StoragePrefix;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioItemServiceImpl implements IPortfolioItemService {

    /**
     * Tope de obras por portafolio. El almacenamiento se factura por GB, y sin
     * un límite una sola cuenta puede upload video hasta agotar el presupuesto.
     */
    private static final long MAX_ITEMS_POR_PORTAFOLIO = 50;

    private final PortfolioItemRepository itemRepository;
    private final PortfolioRepository portafolioRepository;
    private final DocumentStorage almacenamiento;

    /**
     * {@inheritDoc}
     * @param idPortafolio el identificador de portafolio
     * @param idUsuario el identificador de usuario
     * @param peticion el peticion
     * @param archivo el archivo
     * @return el resultado de la operacion, de tipo {@code PortfolioItemResponse}
     */
    @Override
    @Transactional
    public PortfolioItemResponse uploadItem(Long idPortafolio, Long idUsuario,
                                              CreatePortfolioItemRequest peticion, MultipartFile archivo) {
        FilePolicy.PORTAFOLIO.validate(archivo);

        Portfolio portafolio = portafolioRepository.findById(idPortafolio)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Portfolio no encontrado con ID: " + idPortafolio));

        exigirPropietario(portafolio, idUsuario);

        if (itemRepository.countByPortafolioIdPortafolio(idPortafolio) >= MAX_ITEMS_POR_PORTAFOLIO) {
            throw new BusinessRuleException(
                    "El portafolio alcanzó el máximo de " + MAX_ITEMS_POR_PORTAFOLIO + " obras.");
        }

        String referencia = almacenamiento.save(archivo, StoragePrefix.PORTAFOLIO);

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
            deleteSilently(referencia);
            throw e;
        }

        log.info("Obra {} subida al portafolio {}", item.getIdItemPortafolio(), idPortafolio);
        return map(item);
    }

    /**
     * {@inheritDoc}
     * @param idPortafolio el identificador de portafolio
     * @param idUsuario el identificador de usuario
     * @return la lista de PortfolioItemResponse encontrados
     */
    @Override
    @Transactional(readOnly = true)
    public List<PortfolioItemResponse> listItems(Long idPortafolio, Long idUsuario) {
        Portfolio portafolio = portafolioRepository.findById(idPortafolio)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Portfolio no encontrado con ID: " + idPortafolio));

        exigirVisibilidad(portafolio, idUsuario);

        return itemRepository.findByPortafolioIdPortafolioOrderByFechaSubidaDesc(idPortafolio)
                .stream()
                .map(this::map)
                .toList();
    }

    /**
     * {@inheritDoc}
     * @param idItem el identificador de item
     * @param idUsuario el identificador de usuario
     * @return el resultado de la operacion, de tipo {@code PortfolioItemResponse}
     */
    @Override
    @Transactional(readOnly = true)
    public PortfolioItemResponse getItem(Long idItem, Long idUsuario) {
        PortfolioItem item = findItem(idItem);
        exigirVisibilidad(item.getPortafolio(), idUsuario);
        return map(item);
    }

    /**
     * {@inheritDoc}
     * @param idItem el identificador de item
     * @param idUsuario el identificador de usuario
     * @return el resultado de la operacion, de tipo {@code DownloadedFile}
     */
    @Override
    @Transactional(readOnly = true)
    public DownloadedFile downloadFile(Long idItem, Long idUsuario) {
        PortfolioItem item = findItem(idItem);
        exigirVisibilidad(item.getPortafolio(), idUsuario);

        String referencia = item.getUrlArchivoMultimedia();
        return new DownloadedFile(
                almacenamiento.read(referencia),
                "obra-" + idItem + extensionDe(referencia),
                FileExtensions.contentTypeDe(referencia));
    }

    /**
     * {@inheritDoc}
     * @param idItem el identificador de item
     * @param idUsuario el identificador de usuario
     * @param peticion el peticion
     * @return el resultado de la operacion, de tipo {@code PortfolioItemResponse}
     */
    @Override
    @Transactional
    public PortfolioItemResponse updateItem(Long idItem, Long idUsuario, CreatePortfolioItemRequest peticion) {
        PortfolioItem item = findItem(idItem);
        exigirPropietario(item.getPortafolio(), idUsuario);

        item.setTituloObra(peticion.tituloObra());
        item.setDescripcionObra(peticion.descripcionObra());
        item = itemRepository.save(item);

        log.info("Obra {} actualizada en el portafolio {}", idItem, item.getPortafolio().getIdPortafolio());
        return map(item);
    }

    /**
     * {@inheritDoc}
     * @param idItem el identificador de item
     * @param idUsuario el identificador de usuario
     */
    @Override
    @Transactional
    public void deleteItem(Long idItem, Long idUsuario) {
        PortfolioItem item = findItem(idItem);
        exigirPropietario(item.getPortafolio(), idUsuario);

        String referencia = item.getUrlArchivoMultimedia();
        itemRepository.delete(item);
        // Se borra después de la fila: si falla, queda un huérfano en el
        // almacenamiento, que es preferible a una fila apuntando a la nada.
        deleteSilently(referencia);

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
        // REQ-NF-018 (ajuste de seguimiento): una cuenta desactivada (soft-eliminar
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

    private PortfolioItem findItem(Long idItem) {
        return itemRepository.findById(idItem)
                .orElseThrow(() -> new ResourceNotFoundException("Obra no encontrada con ID: " + idItem));
    }

    private void deleteSilently(String referencia) {
        if (referencia == null || referencia.isBlank()) {
            return;
        }
        try {
            almacenamiento.delete(referencia);
        } catch (RuntimeException e) {
            log.warn("No se pudo eliminar el archivo {}: {}", referencia, e.getMessage());
        }
    }

    private String extensionDe(String referencia) {
        int punto = referencia == null ? -1 : referencia.lastIndexOf('.');
        return punto < 0 ? "" : referencia.substring(punto);
    }

    private PortfolioItemResponse map(PortfolioItem item) {
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
