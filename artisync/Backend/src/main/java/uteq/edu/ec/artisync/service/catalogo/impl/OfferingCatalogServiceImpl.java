package uteq.edu.ec.artisync.service.catalogo.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.peticion.catalogo.*;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.*;
import uteq.edu.ec.artisync.entity.catalogo.*;
import uteq.edu.ec.artisync.entity.comunicacion.BriefingTemplate;
import uteq.edu.ec.artisync.entity.pedido.ContractTemplate;
import uteq.edu.ec.artisync.entity.perfil.CreatorProfile;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.catalogo.*;
import uteq.edu.ec.artisync.repository.comunicacion.BriefingTemplateRepository;
import uteq.edu.ec.artisync.repository.pedido.ContractTemplateRepository;
import uteq.edu.ec.artisync.repository.perfil.CreatorProfileRepository;
import uteq.edu.ec.artisync.service.catalogo.IOfferingCatalogService;
import uteq.edu.ec.artisync.service.perfil.IVerificationService;
import uteq.edu.ec.artisync.specification.catalogo.OfferingSpecification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OfferingCatalogServiceImpl implements IOfferingCatalogService {

    private final OfferingRepository servicioRepository;
    private final CreatorProfileRepository perfilRepository;
    private final SubcategoryRepository subcategoriaRepository;
    private final DynamicAttributeRepository atributoRepository;
    private final OfferingAttributeRepository servicioAtributoRepository;
    private final TagRepository etiquetaRepository;
    private final OfferingTagRepository servicioEtiquetaRepository;
    private final OfferingSubcategoryRepository servicioSubcategoriaRepository;
    private final IVerificationService verificacionServicio;
    private final WorkflowRepository flujoTrabajoRepository;
    private final ContractTemplateRepository plantillaContratoRepository;
    private final BriefingTemplateRepository briefingPlantillaRepository;
    private final uteq.edu.ec.artisync.service.shared.almacenamiento.DocumentStorage almacenamientoDocumentos;

    /**
     * Crea un servicio para el perfil de creador indicado, con sus
     * subcategorías y etiquetas, en estado {@code ACTIVO}.
     *
     * @param idPerfilCreador identificador del perfil de creador dueño del servicio
     * @param peticion título, descripción, precio, subcategorías y demás datos del servicio
     * @return el servicio creado, con su detalle completo
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el perfil no existe,
     *         o si alguna subcategoría indicada no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el precio es menor a 0.01 USD,
     *         si quien llama no es dueño del perfil ni admin, o si la identidad del creador no está verificada
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "catalogo", allEntries = true)
    @Auditable(accion = "SERVICIO_CREAR", modulo = AuditModule.CATALOGO,
            entidad = "servicios", idEntidad = "#resultado.idServicio",
            detalle = "{tituloServicio: #peticion.tituloServicio, precioBase: #peticion.precioBase}")
    public OfferingResponse createOffering(Long idPerfilCreador, CreateOfferingRequest peticion) {
        if (peticion.getPrecioBase() == null || peticion.getPrecioBase().compareTo(new BigDecimal("0.01")) < 0) {
            throw new BusinessRuleException("El precio debe ser de al menos 0.01 USD");
        }

        CreatorProfile perfil = perfilRepository.findById(idPerfilCreador)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil creador no encontrado con ID: " + idPerfilCreador));

        validateOwnershipOrAdmin(perfil);
        validateVerifiedIdentity(perfil);

        List<Subcategory> subcategorias = resolveSubcategories(peticion.getIdsSubcategoria());

        Offering servicio = Offering.builder()
                .perfil(perfil)
                .tituloServicio(peticion.getTituloServicio().trim())
                .descripcionDetallada(peticion.getDescripcionDetallada().trim())
                .precioBase(peticion.getPrecioBase())
                .urlMiniatura(peticion.getUrlMiniatura())
                .tipoItem(peticion.getTipoItem() != null ? peticion.getTipoItem() : "SERVICIO")
                .estadoPublicacion("ACTIVO")
                .cargoRevisionAdicional(peticion.getCargoRevisionAdicional() != null ? peticion.getCargoRevisionAdicional() : BigDecimal.ZERO)
                .limiteRevisionesBase(peticion.getLimiteRevisionesBase() != null ? peticion.getLimiteRevisionesBase() : 0)
                .flujo(resolveOwnWorkflow(peticion.getIdFlujo(), perfil))
                .plantillaContrato(resolveActiveContractTemplate(peticion.getIdPlantillaContrato(), perfil))
                .briefingPlantilla(resolveOwnBriefingTemplate(peticion.getIdBriefingPlantilla(), perfil))
                .build();

        Offering guardado = servicioRepository.save(servicio);

        saveOfferingSubcategories(guardado, subcategorias);
        saveOfferingTags(guardado, peticion.getEtiquetaIds());

        return getOfferingById(guardado.getIdServicio());
    }

    /**
     * Actualiza un servicio existente: reemplaza subcategorías y etiquetas
     * solo si vienen informadas, y exige identidad verificada si el nuevo
     * estado de publicación es {@code ACTIVO}.
     *
     * @param idServicio identificador del servicio a actualizar
     * @param peticion nuevos datos del servicio; los campos {@code null} no se modifican
     * @return el servicio ya actualizado, con su detalle completo
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el servicio no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el precio es menor a 0.01 USD,
     *         si se envía una lista vacía de subcategorías, si quien llama no es dueño del perfil ni admin,
     *         o si se activa el servicio sin identidad verificada
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "catalogo", allEntries = true)
    @Auditable(accion = "SERVICIO_ACTUALIZAR", modulo = AuditModule.CATALOGO,
            entidad = "servicios", idEntidad = "#idServicio",
            detalle = "{estadoPublicacion: #peticion.estadoPublicacion, precioBase: #peticion.precioBase}")
    public OfferingResponse updateOffering(Long idServicio, UpdateOfferingRequest peticion) {
        if (peticion.getPrecioBase() == null || peticion.getPrecioBase().compareTo(new BigDecimal("0.01")) < 0) {
            throw new BusinessRuleException("El precio debe ser de al menos 0.01 USD");
        }

        Offering servicio = servicioRepository.findById(idServicio)
                .orElseThrow(() -> new ResourceNotFoundException("Offering no encontrado con ID: " + idServicio));

        validateOwnershipOrAdmin(servicio.getPerfil());

        List<Subcategory> nuevasSubcategorias = null;
        if (peticion.getIdsSubcategoria() != null) {
            if (peticion.getIdsSubcategoria().isEmpty()) {
                throw new BusinessRuleException("El servicio necesita al menos una subcategoria");
            }
            nuevasSubcategorias = resolveSubcategories(peticion.getIdsSubcategoria());
        }

        if (peticion.getTituloServicio() != null && !peticion.getTituloServicio().isBlank()) {
            servicio.setTituloServicio(peticion.getTituloServicio().trim());
        }
        if (peticion.getDescripcionDetallada() != null && !peticion.getDescripcionDetallada().isBlank()) {
            servicio.setDescripcionDetallada(peticion.getDescripcionDetallada().trim());
        }
        servicio.setPrecioBase(peticion.getPrecioBase());
        if (peticion.getTipoItem() != null && !peticion.getTipoItem().isBlank()) {
            servicio.setTipoItem(peticion.getTipoItem());
        }
        if (peticion.getEstadoPublicacion() != null && !peticion.getEstadoPublicacion().isBlank()) {
            if ("ACTIVO".equals(peticion.getEstadoPublicacion())) {
                validateVerifiedIdentity(servicio.getPerfil());
            }
            servicio.setEstadoPublicacion(peticion.getEstadoPublicacion());
        }
        servicio.setUrlMiniatura(peticion.getUrlMiniatura());
        if (peticion.getCargoRevisionAdicional() != null) {
            servicio.setCargoRevisionAdicional(peticion.getCargoRevisionAdicional());
        }
        if (peticion.getLimiteRevisionesBase() != null) {
            servicio.setLimiteRevisionesBase(peticion.getLimiteRevisionesBase());
        }
        servicio.setFlujo(resolveOwnWorkflow(peticion.getIdFlujo(), servicio.getPerfil()));
        servicio.setPlantillaContrato(resolveActiveContractTemplate(peticion.getIdPlantillaContrato(), servicio.getPerfil()));
        servicio.setBriefingPlantilla(resolveOwnBriefingTemplate(peticion.getIdBriefingPlantilla(), servicio.getPerfil()));

        Offering guardado = servicioRepository.save(servicio);

        if (nuevasSubcategorias != null) {
            servicioSubcategoriaRepository.deleteByServicioIdServicio(idServicio);
            // Flush obligatorio: Hibernate ordena inserciones antes que eliminaciones
            // dentro del mismo flush, así que sin esto el INSERT de una subcategoria
            // que ya estaba asociada choca con la fila vieja (todavía no borrada en la
            // base) contra la restricción única id_servicio+id_subcategoria.
            servicioSubcategoriaRepository.flush();
            saveOfferingSubcategories(guardado, nuevasSubcategorias);
        }

        if (peticion.getEtiquetaIds() != null) {
            servicioEtiquetaRepository.deleteByServicioIdServicio(idServicio);
            saveOfferingTags(guardado, peticion.getEtiquetaIds());
        }

        return getOfferingById(guardado.getIdServicio());
    }

    /**
     * Obtiene el detalle completo de un servicio del catálogo.
     *
     * @param idServicio identificador del servicio
     * @return el servicio, con sus subcategorías, etiquetas y atributos
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el servicio no existe
     */
    @Override
    @Transactional(readOnly = true)
    public OfferingResponse getOfferingById(Long idServicio) {
        Offering servicio = servicioRepository.findById(idServicio)
                .orElseThrow(() -> new ResourceNotFoundException("Offering no encontrado con ID: " + idServicio));
        return mapToFullOfferingResponse(servicio);
    }

    /**
     * Elimina un servicio del catálogo, junto con sus asociaciones de
     * etiquetas y subcategorías.
     *
     * @param idServicio identificador del servicio a eliminar
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el servicio no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si quien llama no es dueño del perfil ni admin
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "catalogo", allEntries = true)
    @Auditable(accion = "SERVICIO_ELIMINAR", modulo = AuditModule.CATALOGO,
            entidad = "servicios", idEntidad = "#idServicio")
    public void deleteOffering(Long idServicio) {
        Offering servicio = servicioRepository.findById(idServicio)
                .orElseThrow(() -> new ResourceNotFoundException("Offering no encontrado con ID: " + idServicio));

        validateOwnershipOrAdmin(servicio.getPerfil());

        servicioEtiquetaRepository.deleteByServicioIdServicio(idServicio);
        servicioSubcategoriaRepository.deleteByServicioIdServicio(idServicio);
        servicioRepository.delete(servicio);
    }

    /**
     * Quita una subcategoría de un servicio, siempre que le quede al menos una.
     *
     * @param idServicio identificador del servicio
     * @param idSubcategoria identificador de la subcategoría a quitar
     * @return el servicio ya actualizado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el servicio no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el servicio se quedaría sin ninguna subcategoría
     */
    @Override
    @Transactional
    @CacheEvict(cacheNames = "catalogo", allEntries = true)
    @Auditable(accion = "SERVICIO_QUITAR_SUBCATEGORIA", modulo = AuditModule.CATALOGO,
            entidad = "servicios", idEntidad = "#idServicio", detalle = "{idSubcategoria: #idSubcategoria}")
    public OfferingResponse removeSubcategory(Long idServicio, Long idSubcategoria) {
        if (!servicioRepository.existsById(idServicio)) {
            throw new ResourceNotFoundException("Offering no encontrado con ID: " + idServicio);
        }
        if (servicioSubcategoriaRepository.countByServicioIdServicio(idServicio) <= 1) {
            throw new BusinessRuleException("Un servicio necesita al menos una subcategoria");
        }
        servicioSubcategoriaRepository.deleteByServicioIdServicioAndSubcategoriaIdSubcategoria(idServicio, idSubcategoria);
        return getOfferingById(idServicio);
    }

    /**
     * Lista servicios para el panel de moderación, en cualquier estado de
     * publicación (a diferencia del catálogo público, que solo muestra {@code ACTIVO}).
     *
     * @param textoBusqueda coincidencia parcial sobre título o descripción; {@code null} no filtra
     * @param page número de página (0-index)
     * @param size tamaño de página
     * @return la página de servicios resumidos, más recientes primero
     */
    @Override
    @Transactional(readOnly = true)
    public Page<OfferingSummaryResponse> listForModeration(String textoBusqueda, int page, int size) {
        Specification<Offering> spec = OfferingSpecification.conFiltros(
                null, null, null, null, null, textoBusqueda, null);
        Pageable pageable = PageRequest.of(page, size, Sort.by("idServicio").descending());
        return servicioRepository.findAll(spec, pageable).map(this::mapToOfferingSummary);
    }

    /**
     * Sube la miniatura de un servicio al almacenamiento configurado.
     *
     * @param archivo imagen de miniatura, validada contra {@code FilePolicy.PERFIL}
     * @return la URL pública desde la que se sirve la miniatura
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el archivo no cumple la política de tipo/tamaño
     */
    @Override
    public String uploadThumbnail(org.springframework.web.multipart.MultipartFile archivo) {
        uteq.edu.ec.artisync.service.shared.almacenamiento.FilePolicy.PERFIL.validar(archivo);
        String referencia = almacenamientoDocumentos.guardar(archivo, uteq.edu.ec.artisync.service.shared.almacenamiento.StoragePrefix.SERVICIOS);
        return "/api/v1/servicios/miniatura/" + referencia;
    }

    /**
     * Lista los servicios de un perfil de creador, opcionalmente filtrados
     * por estado de publicación.
     *
     * @param idPerfilCreador identificador del perfil de creador
     * @param estadoPublicacion estado a filtrar; {@code null} o vacío lista todos los estados
     * @return los servicios resumidos del creador
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el perfil no existe
     */
    @Override
    @Transactional(readOnly = true)
    public List<OfferingSummaryResponse> listOfferingsByCreator(Long idPerfilCreador, String estadoPublicacion) {
        if (!perfilRepository.existsById(idPerfilCreador)) {
            throw new ResourceNotFoundException("Perfil creador no encontrado con ID: " + idPerfilCreador);
        }
        List<Offering> servicios;
        if (estadoPublicacion != null && !estadoPublicacion.isBlank()) {
            servicios = servicioRepository.findByPerfilIdPerfilAndEstadoPublicacion(idPerfilCreador, estadoPublicacion);
        } else {
            servicios = servicioRepository.findByPerfilIdPerfil(idPerfilCreador);
        }
        return servicios.stream()
                .map(this::mapToOfferingSummary)
                .collect(Collectors.toList());
    }

    /**
     * Busca el catálogo público (solo servicios {@code ACTIVO}), con
     * etiquetas y subcategorías de cada resultado resueltas en lote (evita el
     * N+1 de resolverlas fila por fila). Cacheada bajo la clave {@code catalogo}.
     *
     * @param categoriaId si no es {@code null}, restringe a esa categoría
     * @param subcategoriaId si no es {@code null}, restringe a esa subcategoría
     * @param precioMin precio base mínimo (inclusive)
     * @param precioMax precio base máximo (inclusive)
     * @param etiquetaIds si no es vacío, restringe a servicios con alguna de esas etiquetas
     * @param textoBusqueda coincidencia parcial sobre título o descripción
     * @param sortParam orden solicitado ("precioBase,asc/desc", "tituloServicio,asc"); cualquier otro
     *                  valor (o {@code null}) ordena por id descendente
     * @param page número de página (0-index)
     * @param size tamaño de página
     * @return la página de servicios resumidos que cumplen los filtros
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "catalogo")
    public Page<OfferingSummaryResponse> searchCatalogOfferings(
            Long categoriaId,
            Long subcategoriaId,
            BigDecimal precioMin,
            BigDecimal precioMax,
            List<Long> etiquetaIds,
            String textoBusqueda,
            String sortParam,
            int page,
            int size) {

        Specification<Offering> spec = OfferingSpecification.conFiltros(
                categoriaId, subcategoriaId, precioMin, precioMax, etiquetaIds, textoBusqueda, "ACTIVO");

        Sort sort = Sort.by("idServicio").descending();
        if (sortParam != null && !sortParam.isBlank()) {
            if ("precioBase,asc".equalsIgnoreCase(sortParam) || "precioAsc".equalsIgnoreCase(sortParam)) {
                sort = Sort.by("precioBase").ascending();
            } else if ("precioBase,desc".equalsIgnoreCase(sortParam) || "precioDesc".equalsIgnoreCase(sortParam)) {
                sort = Sort.by("precioBase").descending();
            } else if ("tituloServicio,asc".equalsIgnoreCase(sortParam)) {
                sort = Sort.by("tituloServicio").ascending();
            }
        }

        Pageable pageable = PageRequest.of(page, size, sort);
        Page<Offering> paginaServicios = servicioRepository.findAll(spec, pageable);

        List<Long> idsServicios = paginaServicios.getContent().stream()
                .map(Offering::getIdServicio)
                .collect(Collectors.toList());

        Map<Long, List<TagResponse>> etiquetasPorServicio = new HashMap<>();
        Map<Long, List<SubcategoryResponse>> subcategoriasPorServicio = new HashMap<>();
        if (!idsServicios.isEmpty()) {
            List<OfferingTag> todasEtiquetas = servicioEtiquetaRepository.findByServicioIdServicioIn(idsServicios);
            etiquetasPorServicio = todasEtiquetas.stream()
                    .collect(Collectors.groupingBy(
                            se -> se.getServicio().getIdServicio(),
                            Collectors.mapping(se -> TagResponse.builder()
                                    .idEtiqueta(se.getEtiqueta().getIdEtiqueta())
                                    .nombreEtiqueta(se.getEtiqueta().getNombreEtiqueta())
                                    .actualizadoEn(se.getEtiqueta().getActualizadoEn())
                                    .build(), Collectors.toList())
                    ));

            List<OfferingSubcategory> todasSubcategorias = servicioSubcategoriaRepository.findByServicioIdServicioIn(idsServicios);
            subcategoriasPorServicio = todasSubcategorias.stream()
                    .collect(Collectors.groupingBy(
                            ss -> ss.getServicio().getIdServicio(),
                            Collectors.mapping(this::mapToSubcategoryResponse, Collectors.toList())
                    ));
        }

        final Map<Long, List<TagResponse>> etiquetasFinales = etiquetasPorServicio;
        final Map<Long, List<SubcategoryResponse>> subcategoriasFinales = subcategoriasPorServicio;
        return paginaServicios.map(s -> mapToOfferingSummary(s,
                etiquetasFinales.getOrDefault(s.getIdServicio(), Collections.emptyList()),
                subcategoriasFinales.getOrDefault(s.getIdServicio(), Collections.emptyList())));
    }

    /**
     * Lista los atributos dinámicos (valor por atributo) asignados a un servicio.
     *
     * @param idServicio identificador del servicio
     * @return los atributos asignados
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el servicio no existe
     */
    @Override
    @Transactional(readOnly = true)
    public List<AttributeResponse> listAttributesByOffering(Long idServicio) {
        if (!servicioRepository.existsById(idServicio)) {
            throw new ResourceNotFoundException("Offering no encontrado con ID: " + idServicio);
        }
        return servicioAtributoRepository.findByServicioIdServicio(idServicio)
                .stream()
                .map(this::mapToAttributeResponse)
                .collect(Collectors.toList());
    }

    /**
     * Agrega un atributo dinámico a un servicio (reutilizando el atributo
     * maestro si ya existe uno con ese nombre), hasta un máximo de 10 por servicio.
     *
     * @param idServicio identificador del servicio
     * @param peticion nombre del atributo, tipo de dato y valor asignado
     * @return el atributo ya asociado al servicio
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el servicio no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si quien llama no es dueño del perfil ni admin,
     *         si el servicio ya tiene 10 atributos, o si ese atributo ya está asociado a este servicio
     */
    @Override
    @Transactional
    public AttributeResponse addAttribute(Long idServicio, CreateAttributeRequest peticion) {
        Offering servicio = servicioRepository.findById(idServicio)
                .orElseThrow(() -> new ResourceNotFoundException("Offering no encontrado con ID: " + idServicio));

        validateOwnershipOrAdmin(servicio.getPerfil());

        long count = servicioAtributoRepository.countByServicioIdServicio(idServicio);
        if (count >= 10) {
            throw new BusinessRuleException("Se ha alcanzado el límite de 10 atributos personalizados por ítem");
        }

        DynamicAttribute atributo = atributoRepository.findByNombreAtributoIgnoreCase(peticion.getNombreAtributo().trim())
                .orElseGet(() -> {
                    DynamicAttribute nuevoAttr = DynamicAttribute.builder()
                            .nombreAtributo(peticion.getNombreAtributo().trim())
                            .tipoDato(peticion.getTipoDato() != null ? peticion.getTipoDato().trim() : "TEXTO")
                            .build();
                    return atributoRepository.save(nuevoAttr);
                });

        if (servicioAtributoRepository.findByServicioIdServicioAndAtributoIdAtributo(idServicio, atributo.getIdAtributo()).isPresent()) {
            throw new BusinessRuleException("El atributo '" + atributo.getNombreAtributo() + "' ya se encuentra asociado a este servicio");
        }

        OfferingAttribute sa = OfferingAttribute.builder()
                .servicio(servicio)
                .atributo(atributo)
                .valorAsignado(peticion.getValorAsignado().trim())
                .build();
        sa = servicioAtributoRepository.save(sa);

        return mapToAttributeResponse(sa);
    }

    /**
     * Actualiza el valor de un atributo dinámico ya asignado a un servicio.
     *
     * @param idServicio identificador del servicio
     * @param idServicioAtributo identificador de la asignación atributo-servicio a actualizar
     * @param peticion nuevo valor asignado
     * @return el atributo ya actualizado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el servicio o la asignación no existen
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si quien llama no es dueño del perfil ni admin,
     *         o si la asignación no pertenece a este servicio
     */
    @Override
    @Transactional
    public AttributeResponse updateAttribute(Long idServicio, Long idServicioAtributo, UpdateAttributeRequest peticion) {
        Offering servicio = servicioRepository.findById(idServicio)
                .orElseThrow(() -> new ResourceNotFoundException("Offering no encontrado con ID: " + idServicio));

        validateOwnershipOrAdmin(servicio.getPerfil());

        OfferingAttribute sa = servicioAtributoRepository.findById(idServicioAtributo)
                .orElseThrow(() -> new ResourceNotFoundException("Atributo del servicio no encontrado con ID: " + idServicioAtributo));

        if (!sa.getServicio().getIdServicio().equals(idServicio)) {
            throw new BusinessRuleException("El atributo no pertenece a este servicio");
        }

        sa.setValorAsignado(peticion.getValorAsignado().trim());
        sa = servicioAtributoRepository.save(sa);

        return mapToAttributeResponse(sa);
    }

    /**
     * Quita un atributo dinámico de un servicio.
     *
     * @param idServicio identificador del servicio
     * @param idServicioAtributo identificador de la asignación atributo-servicio a eliminar
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el servicio o la asignación no existen
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si quien llama no es dueño del perfil ni admin,
     *         o si la asignación no pertenece a este servicio
     */
    @Override
    @Transactional
    public void deleteAttribute(Long idServicio, Long idServicioAtributo) {
        Offering servicio = servicioRepository.findById(idServicio)
                .orElseThrow(() -> new ResourceNotFoundException("Offering no encontrado con ID: " + idServicio));

        validateOwnershipOrAdmin(servicio.getPerfil());

        OfferingAttribute sa = servicioAtributoRepository.findById(idServicioAtributo)
                .orElseThrow(() -> new ResourceNotFoundException("Atributo del servicio no encontrado con ID: " + idServicioAtributo));

        if (!sa.getServicio().getIdServicio().equals(idServicio)) {
            throw new BusinessRuleException("El atributo no pertenece a este servicio");
        }

        servicioAtributoRepository.delete(sa);
    }

    private void saveOfferingTags(Offering servicio, List<Long> etiquetaIds) {
        if (etiquetaIds != null && !etiquetaIds.isEmpty()) {
            List<Tag> etiquetas = etiquetaRepository.findAllById(etiquetaIds);
            for (Tag et : etiquetas) {
                OfferingTag se = OfferingTag.builder()
                        .servicio(servicio)
                        .etiqueta(et)
                        .build();
                servicioEtiquetaRepository.save(se);
            }
        }
    }

    private List<Subcategory> resolveSubcategories(List<Long> idsSubcategoria) {
        List<Subcategory> subcategorias = subcategoriaRepository.findAllById(idsSubcategoria);
        if (subcategorias.size() != new java.util.HashSet<>(idsSubcategoria).size()) {
            throw new ResourceNotFoundException("Una o más subcategorias indicadas no existen");
        }
        return subcategorias;
    }

    private void saveOfferingSubcategories(Offering servicio, List<Subcategory> subcategorias) {
        for (Subcategory sub : subcategorias) {
            OfferingSubcategory ss = OfferingSubcategory.builder()
                    .servicio(servicio)
                    .subcategoria(sub)
                    .build();
            servicioSubcategoriaRepository.save(ss);
        }
    }

    private OfferingResponse mapToFullOfferingResponse(Offering servicio) {
        List<AttributeResponse> atributos = servicioAtributoRepository.findByServicioIdServicio(servicio.getIdServicio())
                .stream()
                .map(this::mapToAttributeResponse)
                .collect(Collectors.toList());

        List<TagResponse> etiquetas = servicioEtiquetaRepository.findByServicioIdServicio(servicio.getIdServicio())
                .stream()
                .map(se -> TagResponse.builder()
                        .idEtiqueta(se.getEtiqueta().getIdEtiqueta())
                        .nombreEtiqueta(se.getEtiqueta().getNombreEtiqueta())
                        .actualizadoEn(se.getEtiqueta().getActualizadoEn())
                        .build())
                .collect(Collectors.toList());

        List<SubcategoryResponse> subcategorias = servicioSubcategoriaRepository.findByServicioIdServicio(servicio.getIdServicio())
                .stream()
                .map(this::mapToSubcategoryResponse)
                .collect(Collectors.toList());

        String nombreCreador = "Creador";
        if (servicio.getPerfil().getUsuario() != null) {
            nombreCreador = servicio.getPerfil().getUsuario().getNombres() + " " + servicio.getPerfil().getUsuario().getApellidos();
        }

        return OfferingResponse.builder()
                .idServicio(servicio.getIdServicio())
                .tituloServicio(servicio.getTituloServicio())
                .descripcionDetallada(servicio.getDescripcionDetallada())
                .precioBase(servicio.getPrecioBase())
                .tipoItem(servicio.getTipoItem())
                .estadoPublicacion(servicio.getEstadoPublicacion())
                .urlMiniatura(servicio.getUrlMiniatura())
                .cargoRevisionAdicional(servicio.getCargoRevisionAdicional())
                .limiteRevisionesBase(servicio.getLimiteRevisionesBase())
                .subcategorias(subcategorias)
                .idPerfilCreador(servicio.getPerfil().getIdPerfil())
                .nombreCreador(nombreCreador)
                .idFlujo(servicio.getFlujo() != null ? servicio.getFlujo().getIdFlujo() : null)
                .nombreFlujo(servicio.getFlujo() != null ? servicio.getFlujo().getNombreFlujo() : null)
                .idPlantillaContrato(servicio.getPlantillaContrato() != null ? servicio.getPlantillaContrato().getIdPlantilla() : null)
                .nombrePlantillaContrato(servicio.getPlantillaContrato() != null ? servicio.getPlantillaContrato().getNombrePlantilla() : null)
                .idBriefingPlantilla(servicio.getBriefingPlantilla() != null ? servicio.getBriefingPlantilla().getIdBriefingPlantilla() : null)
                .nombreBriefingPlantilla(servicio.getBriefingPlantilla() != null ? servicio.getBriefingPlantilla().getNombrePlantilla() : null)
                .preguntasBriefing(mapearPreguntasBriefing(servicio.getBriefingPlantilla()))
                .atributos(atributos)
                .etiquetas(etiquetas)
                .actualizadoEn(servicio.getActualizadoEn())
                .build();
    }

    private List<OfferingResponse.PreguntaBriefingItem> mapearPreguntasBriefing(BriefingTemplate plantilla) {
        if (plantilla == null) {
            return List.of();
        }
        return plantilla.getPreguntas().stream()
                .map(p -> OfferingResponse.PreguntaBriefingItem.builder()
                        .idPregunta(p.getIdPregunta())
                        .textoPregunta(p.getTextoPregunta())
                        .numeroOrden(p.getNumeroOrden())
                        .build())
                .collect(Collectors.toList());
    }

    private SubcategoryResponse mapToSubcategoryResponse(OfferingSubcategory ss) {
        Subcategory sub = ss.getSubcategoria();
        return SubcategoryResponse.builder()
                .idSubcategoria(sub.getIdSubcategoria())
                .idCategoria(sub.getCategoria().getIdCategoria())
                .nombreCategoria(sub.getCategoria().getNombreCategoria())
                .nombreSubcategoria(sub.getNombreSubcategoria())
                .idUsuarioCreador(sub.getCreador() != null ? sub.getCreador().getIdUsuario() : null)
                .nombreCreador(sub.getCreador() != null ? sub.getCreador().getNombres() + " " + sub.getCreador().getApellidos() : null)
                .revisado(sub.getRevisado())
                .actualizadoEn(sub.getActualizadoEn())
                .build();
    }

    /**
     * `null` es una respuesta válida: significa "sin flujo asignado", y el
     * servicio de pedidos cae a un flujo por defecto en ese caso. Un id que no
     * existe, o que existe pero pertenece a otro creador, se rechaza: un
     * creador solo puede asignarle a su servicio uno de sus propios flujos.
     */
    private Workflow resolveOwnWorkflow(Long idFlujo, CreatorProfile perfil) {
        if (idFlujo == null) {
            return null;
        }
        return flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(idFlujo, perfil.getUsuario().getIdUsuario())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Flujo de trabajo no encontrado con ID: " + idFlujo));
    }

    /**
     * `null` es válido: el contrato cae a la plantilla predeterminada del
     * catálogo (ver ContractServiceImpl). Si la plantilla es del catálogo
     * general (idCreador NULL) no hay chequeo de propiedad, porque lo
     * administra ADMIN, no el creador. Si es una plantilla privada (V45),
     * debe pertenecerle a este mismo creador — un creador no puede asignarle
     * a su servicio la plantilla privada de otro.
     */
    private ContractTemplate resolveActiveContractTemplate(Long idPlantillaContrato, CreatorProfile perfil) {
        if (idPlantillaContrato == null) {
            return null;
        }
        ContractTemplate plantilla = plantillaContratoRepository.findById(idPlantillaContrato)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Plantilla de contrato no encontrada con ID: " + idPlantillaContrato));
        if (plantilla.getIdCreador() != null && !plantilla.getIdCreador().equals(perfil.getUsuario().getIdUsuario())) {
            throw new ResourceNotFoundException(
                    "Plantilla de contrato no encontrada con ID: " + idPlantillaContrato);
        }
        if (!Boolean.TRUE.equals(plantilla.getActiva())) {
            throw new BusinessRuleException("La plantilla de contrato elegida ya no está activa");
        }
        return plantilla;
    }

    /**
     * `null` es válido: el servicio queda sin cuestionario y crear un pedido
     * no pide preguntas extra (ver OrderServiceImpl.createOrder). Un id que
     * no existe, o que pertenece a otro creador, se rechaza: un creador solo
     * puede asignarle a su servicio uno de sus propios cuestionarios.
     */
    private BriefingTemplate resolveOwnBriefingTemplate(Long idBriefingPlantilla, CreatorProfile perfil) {
        if (idBriefingPlantilla == null) {
            return null;
        }
        return briefingPlantillaRepository.findByIdBriefingPlantillaAndPerfilCreadorIdPerfil(
                        idBriefingPlantilla, perfil.getIdPerfil())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cuestionario de briefing no encontrado con ID: " + idBriefingPlantilla));
    }

    private OfferingSummaryResponse mapToOfferingSummary(Offering servicio) {
        List<TagResponse> etiquetas = servicioEtiquetaRepository.findByServicioIdServicio(servicio.getIdServicio())
                .stream()
                .map(se -> TagResponse.builder()
                        .idEtiqueta(se.getEtiqueta().getIdEtiqueta())
                        .nombreEtiqueta(se.getEtiqueta().getNombreEtiqueta())
                        .actualizadoEn(se.getEtiqueta().getActualizadoEn())
                        .build())
                .collect(Collectors.toList());
        List<SubcategoryResponse> subcategorias = servicioSubcategoriaRepository.findByServicioIdServicio(servicio.getIdServicio())
                .stream()
                .map(this::mapToSubcategoryResponse)
                .collect(Collectors.toList());
        return mapToOfferingSummary(servicio, etiquetas, subcategorias);
    }

    private OfferingSummaryResponse mapToOfferingSummary(Offering servicio, List<TagResponse> etiquetas, List<SubcategoryResponse> subcategorias) {
        String nombreCreador = "Creador";
        if (servicio.getPerfil().getUsuario() != null) {
            nombreCreador = servicio.getPerfil().getUsuario().getNombres() + " " + servicio.getPerfil().getUsuario().getApellidos();
        }

        return OfferingSummaryResponse.builder()
                .idServicio(servicio.getIdServicio())
                .tituloServicio(servicio.getTituloServicio())
                .precioBase(servicio.getPrecioBase())
                .tipoItem(servicio.getTipoItem())
                .estadoPublicacion(servicio.getEstadoPublicacion())
                .urlMiniatura(servicio.getUrlMiniatura())
                .subcategorias(subcategorias)
                .idPerfilCreador(servicio.getPerfil().getIdPerfil())
                .nombreCreador(nombreCreador)
                .etiquetas(etiquetas)
                .build();
    }

    private AttributeResponse mapToAttributeResponse(OfferingAttribute sa) {
        return AttributeResponse.builder()
                .idServicioAtributo(sa.getIdServicioAtributo())
                .idAtributo(sa.getAtributo().getIdAtributo())
                .nombreAtributo(sa.getAtributo().getNombreAtributo())
                .tipoDato(sa.getAtributo().getTipoDato())
                .valorAsignado(sa.getValorAsignado())
                .actualizadoEn(sa.getActualizadoEn())
                .build();
    }

    /**
     * Un servicio nace y se reactiva siempre en estado ACTIVO (no hay borrador
     * intermedio aquí), así que este es el único punto de la aplicación donde
     * "crear/publicar un servicio" ocurre de verdad. Exigir identidad
     * verificada aquí es exigirla para publicar, tal como pide el requisito.
     */
    private void validateVerifiedIdentity(CreatorProfile perfil) {
        Long idUsuario = perfil.getUsuario() != null ? perfil.getUsuario().getIdUsuario() : null;
        if (idUsuario == null || !verificacionServicio.isIdentityVerified(idUsuario)) {
            throw new BusinessRuleException(
                    "Debes verificar tu identidad antes de publicar un servicio. Sube tu documento de identidad desde tu perfil.");
        }
    }

    private void validateOwnershipOrAdmin(CreatorProfile perfil) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !"anonymousUser".equals(auth.getPrincipal())) {
            boolean esAdmin = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
            if (!esAdmin && perfil.getUsuario() != null) {
                String correoActual = auth.getName();
                if (!correoActual.equalsIgnoreCase(perfil.getUsuario().getCorreo())) {
                    throw new BusinessRuleException("No tiene permisos para gestionar servicios de este perfil del creador");
                }
            }
        }
    }
}
