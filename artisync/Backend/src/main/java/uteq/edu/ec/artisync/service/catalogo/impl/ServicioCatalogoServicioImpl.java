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
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
import uteq.edu.ec.artisync.dto.peticion.catalogo.*;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.*;
import uteq.edu.ec.artisync.entity.catalogo.*;
import uteq.edu.ec.artisync.entity.comunicacion.BriefingPlantilla;
import uteq.edu.ec.artisync.entity.pedido.PlantillaContrato;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.catalogo.*;
import uteq.edu.ec.artisync.repository.comunicacion.BriefingPlantillaRepository;
import uteq.edu.ec.artisync.repository.pedido.PlantillaContratoRepository;
import uteq.edu.ec.artisync.repository.perfil.PerfilCreadorRepository;
import uteq.edu.ec.artisync.service.catalogo.IServicioCatalogoServicio;
import uteq.edu.ec.artisync.service.perfil.IVerificacionServicio;
import uteq.edu.ec.artisync.specification.catalogo.ServicioSpecification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServicioCatalogoServicioImpl implements IServicioCatalogoServicio {

    private final ServicioRepository servicioRepository;
    private final PerfilCreadorRepository perfilRepository;
    private final SubcategoriaRepository subcategoriaRepository;
    private final AtributoDinamicoRepository atributoRepository;
    private final ServicioAtributoRepository servicioAtributoRepository;
    private final EtiquetaRepository etiquetaRepository;
    private final ServicioEtiquetaRepository servicioEtiquetaRepository;
    private final ServicioSubcategoriaRepository servicioSubcategoriaRepository;
    private final IVerificacionServicio verificacionServicio;
    private final FlujoTrabajoRepository flujoTrabajoRepository;
    private final PlantillaContratoRepository plantillaContratoRepository;
    private final BriefingPlantillaRepository briefingPlantillaRepository;
    private final uteq.edu.ec.artisync.service.shared.almacenamiento.AlmacenamientoDocumentos almacenamientoDocumentos;

    @Override
    @Transactional
    @CacheEvict(cacheNames = "catalogo", allEntries = true)
    @Auditable(accion = "SERVICIO_CREAR", modulo = ModuloAuditoria.CATALOGO,
            entidad = "servicios", idEntidad = "#resultado.idServicio",
            detalle = "{tituloServicio: #peticion.tituloServicio, precioBase: #peticion.precioBase}")
    public RespuestaServicio crearServicio(Long idPerfilCreador, PeticionCrearServicio peticion) {
        if (peticion.getPrecioBase() == null || peticion.getPrecioBase().compareTo(new BigDecimal("0.01")) < 0) {
            throw new ExcepcionReglaNegocio("El precio debe ser de al menos 0.01 USD");
        }

        PerfilCreador perfil = perfilRepository.findById(idPerfilCreador)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Perfil creador no encontrado con ID: " + idPerfilCreador));

        validarPropiedadOAdmin(perfil);
        validarIdentidadVerificada(perfil);

        List<Subcategoria> subcategorias = resolverSubcategorias(peticion.getIdsSubcategoria());

        Servicio servicio = Servicio.builder()
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
                .plantillaContrato(resolverPlantillaContratoActiva(peticion.getIdPlantillaContrato()))
                .briefingPlantilla(resolverBriefingPlantillaPropia(peticion.getIdBriefingPlantilla(), perfil))
                .build();

        Servicio guardado = servicioRepository.save(servicio);

        guardarSubcategoriasServicio(guardado, subcategorias);
        guardarEtiquetasServicio(guardado, peticion.getEtiquetaIds());

        return obtenerServicioPorId(guardado.getIdServicio());
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "catalogo", allEntries = true)
    @Auditable(accion = "SERVICIO_ACTUALIZAR", modulo = ModuloAuditoria.CATALOGO,
            entidad = "servicios", idEntidad = "#idServicio",
            detalle = "{estadoPublicacion: #peticion.estadoPublicacion, precioBase: #peticion.precioBase}")
    public RespuestaServicio actualizarServicio(Long idServicio, PeticionActualizarServicio peticion) {
        if (peticion.getPrecioBase() == null || peticion.getPrecioBase().compareTo(new BigDecimal("0.01")) < 0) {
            throw new ExcepcionReglaNegocio("El precio debe ser de al menos 0.01 USD");
        }

        Servicio servicio = servicioRepository.findById(idServicio)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Servicio no encontrado con ID: " + idServicio));

        validarPropiedadOAdmin(servicio.getPerfil());

        List<Subcategoria> nuevasSubcategorias = null;
        if (peticion.getIdsSubcategoria() != null) {
            if (peticion.getIdsSubcategoria().isEmpty()) {
                throw new ExcepcionReglaNegocio("El servicio necesita al menos una subcategoria");
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
        servicio.setPlantillaContrato(resolverPlantillaContratoActiva(peticion.getIdPlantillaContrato()));
        servicio.setBriefingPlantilla(resolverBriefingPlantillaPropia(peticion.getIdBriefingPlantilla(), servicio.getPerfil()));

        Servicio guardado = servicioRepository.save(servicio);

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
    public RespuestaServicio obtenerServicioPorId(Long idServicio) {
        Servicio servicio = servicioRepository.findById(idServicio)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Servicio no encontrado con ID: " + idServicio));
        return mapearAServicioRespuestaCompleta(servicio);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "catalogo", allEntries = true)
    @Auditable(accion = "SERVICIO_ELIMINAR", modulo = ModuloAuditoria.CATALOGO,
            entidad = "servicios", idEntidad = "#idServicio")
    public void eliminarServicio(Long idServicio) {
        Servicio servicio = servicioRepository.findById(idServicio)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Servicio no encontrado con ID: " + idServicio));

        validarPropiedadOAdmin(servicio.getPerfil());

        servicioEtiquetaRepository.deleteByServicioIdServicio(idServicio);
        servicioSubcategoriaRepository.deleteByServicioIdServicio(idServicio);
        servicioRepository.delete(servicio);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = "catalogo", allEntries = true)
    @Auditable(accion = "SERVICIO_QUITAR_SUBCATEGORIA", modulo = ModuloAuditoria.CATALOGO,
            entidad = "servicios", idEntidad = "#idServicio", detalle = "{idSubcategoria: #idSubcategoria}")
    public RespuestaServicio quitarSubcategoria(Long idServicio, Long idSubcategoria) {
        if (!servicioRepository.existsById(idServicio)) {
            throw new ExcepcionRecursoNoEncontrado("Servicio no encontrado con ID: " + idServicio);
        }
        if (servicioSubcategoriaRepository.countByServicioIdServicio(idServicio) <= 1) {
            throw new ExcepcionReglaNegocio("Un servicio necesita al menos una subcategoria");
        }
        servicioSubcategoriaRepository.deleteByServicioIdServicioAndSubcategoriaIdSubcategoria(idServicio, idSubcategoria);
        return obtenerServicioPorId(idServicio);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RespuestaServicioResumido> listarParaModeracion(String textoBusqueda, int page, int size) {
        Specification<Servicio> spec = ServicioSpecification.conFiltros(
                null, null, null, null, null, textoBusqueda, null);
        Pageable pageable = PageRequest.of(page, size, Sort.by("idServicio").descending());
        return servicioRepository.findAll(spec, pageable).map(this::mapearAServicioResumido);
    }

    @Override
    public String subirMiniatura(org.springframework.web.multipart.MultipartFile archivo) {
        uteq.edu.ec.artisync.service.shared.almacenamiento.PoliticaArchivo.PERFIL.validar(archivo);
        String referencia = almacenamientoDocumentos.guardar(archivo, uteq.edu.ec.artisync.service.shared.almacenamiento.PrefijoAlmacenamiento.SERVICIOS);
        return "/api/v1/servicios/miniatura/" + referencia;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaServicioResumido> listarServiciosPorCreador(Long idPerfilCreador, String estadoPublicacion) {
        if (!perfilRepository.existsById(idPerfilCreador)) {
            throw new ExcepcionRecursoNoEncontrado("Perfil creador no encontrado con ID: " + idPerfilCreador);
        }
        List<Servicio> servicios;
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
    public Page<RespuestaServicioResumido> buscarCatalogoServicios(
            Long categoriaId,
            Long subcategoriaId,
            BigDecimal precioMin,
            BigDecimal precioMax,
            List<Long> etiquetaIds,
            String textoBusqueda,
            String sortParam,
            int page,
            int size) {

        Specification<Servicio> spec = ServicioSpecification.conFiltros(
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
        Page<Servicio> paginaServicios = servicioRepository.findAll(spec, pageable);

        List<Long> idsServicios = paginaServicios.getContent().stream()
                .map(Servicio::getIdServicio)
                .collect(Collectors.toList());

        Map<Long, List<RespuestaEtiqueta>> etiquetasPorServicio = new HashMap<>();
        Map<Long, List<RespuestaSubcategoria>> subcategoriasPorServicio = new HashMap<>();
        if (!idsServicios.isEmpty()) {
            List<ServicioEtiqueta> todasEtiquetas = servicioEtiquetaRepository.findByServicioIdServicioIn(idsServicios);
            etiquetasPorServicio = todasEtiquetas.stream()
                    .collect(Collectors.groupingBy(
                            se -> se.getServicio().getIdServicio(),
                            Collectors.mapping(se -> RespuestaEtiqueta.builder()
                                    .idEtiqueta(se.getEtiqueta().getIdEtiqueta())
                                    .nombreEtiqueta(se.getEtiqueta().getNombreEtiqueta())
                                    .actualizadoEn(se.getEtiqueta().getActualizadoEn())
                                    .build(), Collectors.toList())
                    ));

            List<ServicioSubcategoria> todasSubcategorias = servicioSubcategoriaRepository.findByServicioIdServicioIn(idsServicios);
            subcategoriasPorServicio = todasSubcategorias.stream()
                    .collect(Collectors.groupingBy(
                            ss -> ss.getServicio().getIdServicio(),
                            Collectors.mapping(this::mapearASubcategoriaRespuesta, Collectors.toList())
                    ));
        }

        final Map<Long, List<RespuestaEtiqueta>> etiquetasFinales = etiquetasPorServicio;
        final Map<Long, List<RespuestaSubcategoria>> subcategoriasFinales = subcategoriasPorServicio;
        return paginaServicios.map(s -> mapearAServicioResumido(s,
                etiquetasFinales.getOrDefault(s.getIdServicio(), Collections.emptyList()),
                subcategoriasFinales.getOrDefault(s.getIdServicio(), Collections.emptyList())));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaAtributo> listarAtributosPorServicio(Long idServicio) {
        if (!servicioRepository.existsById(idServicio)) {
            throw new ExcepcionRecursoNoEncontrado("Servicio no encontrado con ID: " + idServicio);
        }
        return servicioAtributoRepository.findByServicioIdServicio(idServicio)
                .stream()
                .map(this::mapearAAtributoRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public RespuestaAtributo agregarAtributo(Long idServicio, PeticionCrearAtributo peticion) {
        Servicio servicio = servicioRepository.findById(idServicio)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Servicio no encontrado con ID: " + idServicio));

        validarPropiedadOAdmin(servicio.getPerfil());

        long count = servicioAtributoRepository.countByServicioIdServicio(idServicio);
        if (count >= 10) {
            throw new ExcepcionReglaNegocio("Se ha alcanzado el límite de 10 atributos personalizados por ítem");
        }

        AtributoDinamico atributo = atributoRepository.findByNombreAtributoIgnoreCase(peticion.getNombreAtributo().trim())
                .orElseGet(() -> {
                    AtributoDinamico nuevoAttr = AtributoDinamico.builder()
                            .nombreAtributo(peticion.getNombreAtributo().trim())
                            .tipoDato(peticion.getTipoDato() != null ? peticion.getTipoDato().trim() : "TEXTO")
                            .build();
                    return atributoRepository.save(nuevoAttr);
                });

        if (servicioAtributoRepository.findByServicioIdServicioAndAtributoIdAtributo(idServicio, atributo.getIdAtributo()).isPresent()) {
            throw new ExcepcionReglaNegocio("El atributo '" + atributo.getNombreAtributo() + "' ya se encuentra asociado a este servicio");
        }

        ServicioAtributo sa = ServicioAtributo.builder()
                .servicio(servicio)
                .atributo(atributo)
                .valorAsignado(peticion.getValorAsignado().trim())
                .build();
        sa = servicioAtributoRepository.save(sa);

        return mapearAAtributoRespuesta(sa);
    }

    @Override
    @Transactional
    public RespuestaAtributo actualizarAtributo(Long idServicio, Long idServicioAtributo, PeticionActualizarAtributo peticion) {
        Servicio servicio = servicioRepository.findById(idServicio)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Servicio no encontrado con ID: " + idServicio));

        validarPropiedadOAdmin(servicio.getPerfil());

        ServicioAtributo sa = servicioAtributoRepository.findById(idServicioAtributo)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Atributo del servicio no encontrado con ID: " + idServicioAtributo));

        if (!sa.getServicio().getIdServicio().equals(idServicio)) {
            throw new ExcepcionReglaNegocio("El atributo no pertenece a este servicio");
        }

        sa.setValorAsignado(peticion.getValorAsignado().trim());
        sa = servicioAtributoRepository.save(sa);

        return mapearAAtributoRespuesta(sa);
    }

    @Override
    @Transactional
    public void eliminarAtributo(Long idServicio, Long idServicioAtributo) {
        Servicio servicio = servicioRepository.findById(idServicio)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Servicio no encontrado con ID: " + idServicio));

        validarPropiedadOAdmin(servicio.getPerfil());

        ServicioAtributo sa = servicioAtributoRepository.findById(idServicioAtributo)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Atributo del servicio no encontrado con ID: " + idServicioAtributo));

        if (!sa.getServicio().getIdServicio().equals(idServicio)) {
            throw new ExcepcionReglaNegocio("El atributo no pertenece a este servicio");
        }

        servicioAtributoRepository.delete(sa);
    }

    private void guardarEtiquetasServicio(Servicio servicio, List<Long> etiquetaIds) {
        if (etiquetaIds != null && !etiquetaIds.isEmpty()) {
            List<Etiqueta> etiquetas = etiquetaRepository.findAllById(etiquetaIds);
            for (Etiqueta et : etiquetas) {
                ServicioEtiqueta se = ServicioEtiqueta.builder()
                        .servicio(servicio)
                        .etiqueta(et)
                        .build();
                servicioEtiquetaRepository.save(se);
            }
        }
    }

    private List<Subcategoria> resolverSubcategorias(List<Long> idsSubcategoria) {
        List<Subcategoria> subcategorias = subcategoriaRepository.findAllById(idsSubcategoria);
        if (subcategorias.size() != new java.util.HashSet<>(idsSubcategoria).size()) {
            throw new ExcepcionRecursoNoEncontrado("Una o más subcategorias indicadas no existen");
        }
        return subcategorias;
    }

    private void guardarSubcategoriasServicio(Servicio servicio, List<Subcategoria> subcategorias) {
        for (Subcategoria sub : subcategorias) {
            ServicioSubcategoria ss = ServicioSubcategoria.builder()
                    .servicio(servicio)
                    .subcategoria(sub)
                    .build();
            servicioSubcategoriaRepository.save(ss);
        }
    }

    private RespuestaServicio mapearAServicioRespuestaCompleta(Servicio servicio) {
        List<RespuestaAtributo> atributos = servicioAtributoRepository.findByServicioIdServicio(servicio.getIdServicio())
                .stream()
                .map(this::mapearAAtributoRespuesta)
                .collect(Collectors.toList());

        List<RespuestaEtiqueta> etiquetas = servicioEtiquetaRepository.findByServicioIdServicio(servicio.getIdServicio())
                .stream()
                .map(se -> RespuestaEtiqueta.builder()
                        .idEtiqueta(se.getEtiqueta().getIdEtiqueta())
                        .nombreEtiqueta(se.getEtiqueta().getNombreEtiqueta())
                        .actualizadoEn(se.getEtiqueta().getActualizadoEn())
                        .build())
                .collect(Collectors.toList());

        List<RespuestaSubcategoria> subcategorias = servicioSubcategoriaRepository.findByServicioIdServicio(servicio.getIdServicio())
                .stream()
                .map(this::mapearASubcategoriaRespuesta)
                .collect(Collectors.toList());

        String nombreCreador = "Creador";
        if (servicio.getPerfil().getUsuario() != null) {
            nombreCreador = servicio.getPerfil().getUsuario().getNombres() + " " + servicio.getPerfil().getUsuario().getApellidos();
        }

        return RespuestaServicio.builder()
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

    private List<RespuestaServicio.PreguntaBriefingItem> mapearPreguntasBriefing(BriefingPlantilla plantilla) {
        if (plantilla == null) {
            return List.of();
        }
        return plantilla.getPreguntas().stream()
                .map(p -> RespuestaServicio.PreguntaBriefingItem.builder()
                        .idPregunta(p.getIdPregunta())
                        .textoPregunta(p.getTextoPregunta())
                        .numeroOrden(p.getNumeroOrden())
                        .build())
                .collect(Collectors.toList());
    }

    private RespuestaSubcategoria mapearASubcategoriaRespuesta(ServicioSubcategoria ss) {
        Subcategoria sub = ss.getSubcategoria();
        return RespuestaSubcategoria.builder()
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
    private FlujoTrabajo resolverFlujoPropio(Long idFlujo, PerfilCreador perfil) {
        if (idFlujo == null) {
            return null;
        }
        return flujoTrabajoRepository.findByIdFlujoAndCreadorIdUsuario(idFlujo, perfil.getUsuario().getIdUsuario())
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "Flujo de trabajo no encontrado con ID: " + idFlujo));
    }

    /**
     * `null` es válido: el contrato cae a la plantilla predeterminada del
     * catálogo (ver ContratoServicioImpl). No hay chequeo de propiedad porque
     * el catálogo lo administra ADMIN, no el creador; solo se exige que la
     * plantilla exista y siga activa.
     */
    private PlantillaContrato resolverPlantillaContratoActiva(Long idPlantillaContrato) {
        if (idPlantillaContrato == null) {
            return null;
        }
        PlantillaContrato plantilla = plantillaContratoRepository.findById(idPlantillaContrato)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "Plantilla de contrato no encontrada con ID: " + idPlantillaContrato));
        if (!Boolean.TRUE.equals(plantilla.getActiva())) {
            throw new ExcepcionReglaNegocio("La plantilla de contrato elegida ya no está activa");
        }
        return plantilla;
    }

    /**
     * `null` es válido: el servicio queda sin cuestionario y crear un pedido
     * no pide preguntas extra (ver PedidoServicioImpl.crearPedido). Un id que
     * no existe, o que pertenece a otro creador, se rechaza: un creador solo
     * puede asignarle a su servicio uno de sus propios cuestionarios.
     */
    private BriefingPlantilla resolverBriefingPlantillaPropia(Long idBriefingPlantilla, PerfilCreador perfil) {
        if (idBriefingPlantilla == null) {
            return null;
        }
        return briefingPlantillaRepository.findByIdBriefingPlantillaAndPerfilCreadorIdPerfil(
                        idBriefingPlantilla, perfil.getIdPerfil())
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "Cuestionario de briefing no encontrado con ID: " + idBriefingPlantilla));
    }

    private RespuestaServicioResumido mapearAServicioResumido(Servicio servicio) {
        List<RespuestaEtiqueta> etiquetas = servicioEtiquetaRepository.findByServicioIdServicio(servicio.getIdServicio())
                .stream()
                .map(se -> RespuestaEtiqueta.builder()
                        .idEtiqueta(se.getEtiqueta().getIdEtiqueta())
                        .nombreEtiqueta(se.getEtiqueta().getNombreEtiqueta())
                        .actualizadoEn(se.getEtiqueta().getActualizadoEn())
                        .build())
                .collect(Collectors.toList());
        List<RespuestaSubcategoria> subcategorias = servicioSubcategoriaRepository.findByServicioIdServicio(servicio.getIdServicio())
                .stream()
                .map(this::mapearASubcategoriaRespuesta)
                .collect(Collectors.toList());
        return mapearAServicioResumido(servicio, etiquetas, subcategorias);
    }

    private RespuestaServicioResumido mapearAServicioResumido(Servicio servicio, List<RespuestaEtiqueta> etiquetas, List<RespuestaSubcategoria> subcategorias) {
        String nombreCreador = "Creador";
        if (servicio.getPerfil().getUsuario() != null) {
            nombreCreador = servicio.getPerfil().getUsuario().getNombres() + " " + servicio.getPerfil().getUsuario().getApellidos();
        }

        return RespuestaServicioResumido.builder()
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

    private RespuestaAtributo mapearAAtributoRespuesta(ServicioAtributo sa) {
        return RespuestaAtributo.builder()
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
            throw new ExcepcionReglaNegocio(
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
                    throw new ExcepcionReglaNegocio("No tiene permisos para gestionar servicios de este perfil del creador");
                }
            }
        }
    }
}
