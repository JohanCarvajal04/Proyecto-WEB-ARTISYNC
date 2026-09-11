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
import uteq.edu.ec.artisync.entity.comunicacion.BriefingPlantilla;
import uteq.edu.ec.artisync.entity.pedido.ContractTemplate;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.catalogo.*;
import uteq.edu.ec.artisync.repository.comunicacion.BriefingPlantillaRepository;
import uteq.edu.ec.artisync.repository.pedido.ContractTemplateRepository;
import uteq.edu.ec.artisync.repository.perfil.PerfilCreadorRepository;
import uteq.edu.ec.artisync.service.catalogo.IOfferingCatalogService;
import uteq.edu.ec.artisync.service.perfil.IVerificacionServicio;
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
    private final PerfilCreadorRepository perfilRepository;
    private final SubcategoryRepository subcategoriaRepository;
    private final DynamicAttributeRepository atributoRepository;
    private final OfferingAttributeRepository servicioAtributoRepository;
    private final TagRepository etiquetaRepository;
    private final OfferingTagRepository servicioEtiquetaRepository;
    private final OfferingSubcategoryRepository servicioSubcategoriaRepository;
    private final IVerificacionServicio verificacionServicio;
    private final WorkflowRepository flujoTrabajoRepository;
    private final ContractTemplateRepository plantillaContratoRepository;
    private final BriefingPlantillaRepository briefingPlantillaRepository;
    private final uteq.edu.ec.artisync.service.shared.almacenamiento.AlmacenamientoDocumentos almacenamientoDocumentos;

    @Override
    @Transactional
    @CacheEvict(cacheNames = "catalogo", allEntries = true)
    @Auditable(accion = "SERVICIO_CREAR", modulo = AuditModule.CATALOGO,
            entidad = "servicios", idEntidad = "#resultado.idServicio",
            detalle = "{tituloServicio: #peticion.tituloServicio, precioBase: #peticion.precioBase}")
    /**
     * Procesa y persiste la creacion de un nuevo recurso en el contexto de negocio aplicable.
     *
     * @param idPerfilCreador identificador unico que referencia de manera univoca al registro
     * @param peticion estructura de transferencia de datos con la informacion estructurada de entrada
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public OfferingResponse crearServicio(Long idPerfilCreador, CreateOfferingRequest peticion) {
        if (peticion.getPrecioBase() == null || peticion.getPrecioBase().compareTo(new BigDecimal("0.01")) < 0) {
            throw new BusinessRuleException("El precio debe ser de al menos 0.01 USD");
        }

        PerfilCreador perfil = perfilRepository.findById(idPerfilCreador)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil creador no encontrado con ID: " + idPerfilCreador));

        validarPropiedadOAdmin(perfil);
        validarIdentidadVerificada(perfil);

        List<Subcategory> subcategorias = resolverSubcategorias(peticion.getIdsSubcategoria());

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
                .flujo(resolverFlujoPropio(peticion.getIdFlujo(), perfil))
                .plantillaContrato(resolverPlantillaContratoActiva(peticion.getIdPlantillaContrato(), perfil))
                .briefingPlantilla(resolverBriefingPlantillaPropia(peticion.getIdBriefingPlantilla(), perfil))
                .build();

        Offering guardado = servicioRepository.save(servicio);

        guardarSubcategoriasServicio(guardado, subcategorias);
        guardarEtiquetasServicio(guardado, peticion.getEtiquetaIds());

        return obtenerServicioPorId(guardado.getIdServicio());
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "catalogo", allEntries = true)
    @Auditable(accion = "SERVICIO_ACTUALIZAR", modulo = AuditModule.CATALOGO,
            entidad = "servicios", idEntidad = "#idServicio",
            detalle = "{estadoPublicacion: #peticion.estadoPublicacion, precioBase: #peticion.precioBase}")
    /**
     * Aplica modificaciones y validaciones de negocio sobre los datos de un registro existente.
     *
     * @param idServicio identificador unico que referencia de manera univoca al registro
     * @param peticion estructura de transferencia de datos con la informacion estructurada de entrada
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public OfferingResponse actualizarServicio(Long idServicio, UpdateOfferingRequest peticion) {
        if (peticion.getPrecioBase() == null || peticion.getPrecioBase().compareTo(new BigDecimal("0.01")) < 0) {
            throw new BusinessRuleException("El precio debe ser de al menos 0.01 USD");
        }

        Offering servicio = servicioRepository.findById(idServicio)
                .orElseThrow(() -> new ResourceNotFoundException("Offering no encontrado con ID: " + idServicio));

        validarPropiedadOAdmin(servicio.getPerfil());

        List<Subcategory> nuevasSubcategorias = null;
        if (peticion.getIdsSubcategoria() != null) {
            if (peticion.getIdsSubcategoria().isEmpty()) {
                throw new BusinessRuleException("El servicio necesita al menos una subcategoria");
            }
            nuevasSubcategorias = resolverSubcategorias(peticion.getIdsSubcategoria());
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
                validarIdentidadVerificada(servicio.getPerfil());
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
        servicio.setFlujo(resolverFlujoPropio(peticion.getIdFlujo(), servicio.getPerfil()));
        servicio.setPlantillaContrato(resolverPlantillaContratoActiva(peticion.getIdPlantillaContrato(), servicio.getPerfil()));
        servicio.setBriefingPlantilla(resolverBriefingPlantillaPropia(peticion.getIdBriefingPlantilla(), servicio.getPerfil()));

        Offering guardado = servicioRepository.save(servicio);

        if (nuevasSubcategorias != null) {
            servicioSubcategoriaRepository.deleteByServicioIdServicio(idServicio);
            // Flush obligatorio: Hibernate ordena inserciones antes que eliminaciones
            // dentro del mismo flush, así que sin esto el INSERT de una subcategoria
            // que ya estaba asociada choca con la fila vieja (todavía no borrada en la
            // base) contra la restricción única id_servicio+id_subcategoria.
            servicioSubcategoriaRepository.flush();
            guardarSubcategoriasServicio(guardado, nuevasSubcategorias);
        }

        if (peticion.getEtiquetaIds() != null) {
            servicioEtiquetaRepository.deleteByServicioIdServicio(idServicio);
            guardarEtiquetasServicio(guardado, peticion.getEtiquetaIds());
        }

        return obtenerServicioPorId(guardado.getIdServicio());
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param idServicio identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public OfferingResponse obtenerServicioPorId(Long idServicio) {
        Offering servicio = servicioRepository.findById(idServicio)
                .orElseThrow(() -> new ResourceNotFoundException("Offering no encontrado con ID: " + idServicio));
        return mapearAServicioRespuestaCompleta(servicio);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "catalogo", allEntries = true)
    @Auditable(accion = "SERVICIO_ELIMINAR", modulo = AuditModule.CATALOGO,
            entidad = "servicios", idEntidad = "#idServicio")
    /**
     * Ejecuta la eliminacion logica o fisica del registro indicado, comprobando dependencias previas.
     *
     * @param idServicio identificador unico que referencia de manera univoca al registro
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public void eliminarServicio(Long idServicio) {
        Offering servicio = servicioRepository.findById(idServicio)
                .orElseThrow(() -> new ResourceNotFoundException("Offering no encontrado con ID: " + idServicio));

        validarPropiedadOAdmin(servicio.getPerfil());

        servicioEtiquetaRepository.deleteByServicioIdServicio(idServicio);
        servicioSubcategoriaRepository.deleteByServicioIdServicio(idServicio);
        servicioRepository.delete(servicio);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "catalogo", allEntries = true)
    @Auditable(accion = "SERVICIO_QUITAR_SUBCATEGORIA", modulo = AuditModule.CATALOGO,
            entidad = "servicios", idEntidad = "#idServicio", detalle = "{idSubcategoria: #idSubcategoria}")
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idServicio identificador unico que referencia de manera univoca al registro
     * @param idSubcategoria identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public OfferingResponse quitarSubcategoria(Long idServicio, Long idSubcategoria) {
        if (!servicioRepository.existsById(idServicio)) {
            throw new ResourceNotFoundException("Offering no encontrado con ID: " + idServicio);
        }
        if (servicioSubcategoriaRepository.countByServicioIdServicio(idServicio) <= 1) {
            throw new BusinessRuleException("Un servicio necesita al menos una subcategoria");
        }
        servicioSubcategoriaRepository.deleteByServicioIdServicioAndSubcategoriaIdSubcategoria(idServicio, idSubcategoria);
        return obtenerServicioPorId(idServicio);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @param textoBusqueda parametro requerido para la correcta ejecucion del procedimiento
     * @param page parametro requerido para la correcta ejecucion del procedimiento
     * @param size parametro requerido para la correcta ejecucion del procedimiento
     * @return una estructura de datos paginada con la porcion de resultados solicitada y metadatos de pagina
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public Page<OfferingSummaryResponse> listarParaModeracion(String textoBusqueda, int page, int size) {
        Specification<Offering> spec = OfferingSpecification.conFiltros(
                null, null, null, null, null, textoBusqueda, null);
        Pageable pageable = PageRequest.of(page, size, Sort.by("idServicio").descending());
        return servicioRepository.findAll(spec, pageable).map(this::mapearAServicioResumido);
    }

    @Override
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param archivo objeto binario multipart representando el documento o medio fisico
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public String subirMiniatura(org.springframework.web.multipart.MultipartFile archivo) {
        uteq.edu.ec.artisync.service.shared.almacenamiento.PoliticaArchivo.PERFIL.validar(archivo);
        String referencia = almacenamientoDocumentos.guardar(archivo, uteq.edu.ec.artisync.service.shared.almacenamiento.PrefijoAlmacenamiento.SERVICIOS);
        return "/api/v1/servicios/miniatura/" + referencia;
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @param idPerfilCreador identificador unico que referencia de manera univoca al registro
     * @param estadoPublicacion parametro requerido para la correcta ejecucion del procedimiento
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<OfferingSummaryResponse> listarServiciosPorCreador(Long idPerfilCreador, String estadoPublicacion) {
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
                .map(this::mapearAServicioResumido)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(cacheNames = "catalogo")
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     * @param keyword palabra clave para la busqueda
     * @param categorias lista de categorias a filtrar
     * @param modalidades lista de modalidades a filtrar
     * @param precioMin precio minimo
     * @param precioMax precio maximo
     * @param pageable configuracion de paginacion
     * @return una estructura de datos paginada con la porcion de resultados solicitada
     */
    public Page<OfferingSummaryResponse> buscarCatalogoServicios(
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
                            Collectors.mapping(this::mapearASubcategoriaRespuesta, Collectors.toList())
                    ));
        }

        final Map<Long, List<TagResponse>> etiquetasFinales = etiquetasPorServicio;
        final Map<Long, List<SubcategoryResponse>> subcategoriasFinales = subcategoriasPorServicio;
        return paginaServicios.map(s -> mapearAServicioResumido(s,
                etiquetasFinales.getOrDefault(s.getIdServicio(), Collections.emptyList()),
                subcategoriasFinales.getOrDefault(s.getIdServicio(), Collections.emptyList())));
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @param idServicio identificador unico que referencia de manera univoca al registro
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<AttributeResponse> listarAtributosPorServicio(Long idServicio) {
        if (!servicioRepository.existsById(idServicio)) {
            throw new ResourceNotFoundException("Offering no encontrado con ID: " + idServicio);
        }
        return servicioAtributoRepository.findByServicioIdServicio(idServicio)
                .stream()
                .map(this::mapearAAtributoRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idServicio identificador unico que referencia de manera univoca al registro
     * @param peticion estructura de transferencia de datos con la informacion estructurada de entrada
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public AttributeResponse agregarAtributo(Long idServicio, CreateAttributeRequest peticion) {
        Offering servicio = servicioRepository.findById(idServicio)
                .orElseThrow(() -> new ResourceNotFoundException("Offering no encontrado con ID: " + idServicio));

        validarPropiedadOAdmin(servicio.getPerfil());

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

        return mapearAAtributoRespuesta(sa);
    }

    @Override
    @Transactional
    /**
     * Aplica modificaciones y validaciones de negocio sobre los datos de un registro existente.
     *
     * @param idServicio identificador unico que referencia de manera univoca al registro
     * @param idServicioAtributo identificador unico que referencia de manera univoca al registro
     * @param peticion estructura de transferencia de datos con la informacion estructurada de entrada
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public AttributeResponse actualizarAtributo(Long idServicio, Long idServicioAtributo, UpdateAttributeRequest peticion) {
        Offering servicio = servicioRepository.findById(idServicio)
                .orElseThrow(() -> new ResourceNotFoundException("Offering no encontrado con ID: " + idServicio));

        validarPropiedadOAdmin(servicio.getPerfil());

        OfferingAttribute sa = servicioAtributoRepository.findById(idServicioAtributo)
                .orElseThrow(() -> new ResourceNotFoundException("Atributo del servicio no encontrado con ID: " + idServicioAtributo));

        if (!sa.getServicio().getIdServicio().equals(idServicio)) {
            throw new BusinessRuleException("El atributo no pertenece a este servicio");
        }

        sa.setValorAsignado(peticion.getValorAsignado().trim());
        sa = servicioAtributoRepository.save(sa);

        return mapearAAtributoRespuesta(sa);
    }

    @Override
    @Transactional
    /**
     * Ejecuta la eliminacion logica o fisica del registro indicado, comprobando dependencias previas.
     *
     * @param idServicio identificador unico que referencia de manera univoca al registro
     * @param idServicioAtributo identificador unico que referencia de manera univoca al registro
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public void eliminarAtributo(Long idServicio, Long idServicioAtributo) {
        Offering servicio = servicioRepository.findById(idServicio)
                .orElseThrow(() -> new ResourceNotFoundException("Offering no encontrado con ID: " + idServicio));

        validarPropiedadOAdmin(servicio.getPerfil());

        OfferingAttribute sa = servicioAtributoRepository.findById(idServicioAtributo)
                .orElseThrow(() -> new ResourceNotFoundException("Atributo del servicio no encontrado con ID: " + idServicioAtributo));

        if (!sa.getServicio().getIdServicio().equals(idServicio)) {
            throw new BusinessRuleException("El atributo no pertenece a este servicio");
        }

        servicioAtributoRepository.delete(sa);
    }

    private void guardarEtiquetasServicio(Offering servicio, List<Long> etiquetaIds) {
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

    private List<Subcategory> resolverSubcategorias(List<Long> idsSubcategoria) {
        List<Subcategory> subcategorias = subcategoriaRepository.findAllById(idsSubcategoria);
        if (subcategorias.size() != new java.util.HashSet<>(idsSubcategoria).size()) {
            throw new ResourceNotFoundException("Una o más subcategorias indicadas no existen");
        }
        return subcategorias;
    }

    private void guardarSubcategoriasServicio(Offering servicio, List<Subcategory> subcategorias) {
        for (Subcategory sub : subcategorias) {
            OfferingSubcategory ss = OfferingSubcategory.builder()
                    .servicio(servicio)
                    .subcategoria(sub)
                    .build();
            servicioSubcategoriaRepository.save(ss);
        }
    }

    private OfferingResponse mapearAServicioRespuestaCompleta(Offering servicio) {
        List<AttributeResponse> atributos = servicioAtributoRepository.findByServicioIdServicio(servicio.getIdServicio())
                .stream()
                .map(this::mapearAAtributoRespuesta)
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
                .map(this::mapearASubcategoriaRespuesta)
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

    private List<OfferingResponse.PreguntaBriefingItem> mapearPreguntasBriefing(BriefingPlantilla plantilla) {
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

    private SubcategoryResponse mapearASubcategoriaRespuesta(OfferingSubcategory ss) {
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
    private Workflow resolverFlujoPropio(Long idFlujo, PerfilCreador perfil) {
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
    private ContractTemplate resolverPlantillaContratoActiva(Long idPlantillaContrato, PerfilCreador perfil) {
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
     * no pide preguntas extra (ver OrderServiceImpl.crearPedido). Un id que
     * no existe, o que pertenece a otro creador, se rechaza: un creador solo
     * puede asignarle a su servicio uno de sus propios cuestionarios.
     */
    private BriefingPlantilla resolverBriefingPlantillaPropia(Long idBriefingPlantilla, PerfilCreador perfil) {
        if (idBriefingPlantilla == null) {
            return null;
        }
        return briefingPlantillaRepository.findByIdBriefingPlantillaAndPerfilCreadorIdPerfil(
                        idBriefingPlantilla, perfil.getIdPerfil())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Cuestionario de briefing no encontrado con ID: " + idBriefingPlantilla));
    }

    private OfferingSummaryResponse mapearAServicioResumido(Offering servicio) {
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
                .map(this::mapearASubcategoriaRespuesta)
                .collect(Collectors.toList());
        return mapearAServicioResumido(servicio, etiquetas, subcategorias);
    }

    private OfferingSummaryResponse mapearAServicioResumido(Offering servicio, List<TagResponse> etiquetas, List<SubcategoryResponse> subcategorias) {
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

    private AttributeResponse mapearAAtributoRespuesta(OfferingAttribute sa) {
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
    private void validarIdentidadVerificada(PerfilCreador perfil) {
        Long idUsuario = perfil.getUsuario() != null ? perfil.getUsuario().getIdUsuario() : null;
        if (idUsuario == null || !verificacionServicio.estaIdentidadVerificada(idUsuario)) {
            throw new BusinessRuleException(
                    "Debes verificar tu identidad antes de publicar un servicio. Sube tu documento de identidad desde tu perfil.");
        }
    }

    private void validarPropiedadOAdmin(PerfilCreador perfil) {
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
