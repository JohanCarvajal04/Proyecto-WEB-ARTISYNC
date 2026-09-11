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
     * Procesa y persiste la creacion de un nuevo recurso en el contexto de negocio aplicable.
     * @param idPortafolio id del portafolio
     * @param idUsuario id del usuario
     * @param peticion peticion
     * @param archivo archivo
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
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
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @param idPortafolio identificador unico que referencia de manera univoca al registro
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
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
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param idItem identificador unico que referencia de manera univoca al registro
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public PortfolioItemResponse obtenerItem(Long idItem, Long idUsuario) {
        PortfolioItem item = buscarItem(idItem);
        exigirVisibilidad(item.getPortafolio(), idUsuario);
        return mapear(item);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Prepara y ensambla un documento o archivo fisico de salida con los datos requeridos.
     *
     * @param idItem identificador unico que referencia de manera univoca al registro
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
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
     * Aplica modificaciones y validaciones de negocio sobre los datos de un registro existente.
     *
     * @param idItem identificador unico que referencia de manera univoca al registro
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @param peticion estructura de transferencia de datos con la informacion estructurada de entrada
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
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
     * Ejecuta la eliminacion logica o fisica del registro indicado, comprobando dependencias previas.
     *
     * @param idItem identificador unico que referencia de manera univoca al registro
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
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
