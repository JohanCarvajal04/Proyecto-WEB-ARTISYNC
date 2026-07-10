package uteq.edu.ec.artisync.service.catalogo.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.peticion.catalogo.*;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.*;
import uteq.edu.ec.artisync.entity.catalogo.*;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.catalogo.*;
import uteq.edu.ec.artisync.repository.perfil.PerfilCreadorRepository;
import uteq.edu.ec.artisync.service.catalogo.IServicioCatalogoServicio;
import uteq.edu.ec.artisync.specification.catalogo.ServicioSpecification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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

    @Override
    @Transactional
    public RespuestaServicio crearServicio(Long idPerfilCreador, PeticionCrearServicio peticion) {
        if (peticion.getPrecioBase() == null || peticion.getPrecioBase().compareTo(new BigDecimal("0.01")) < 0) {
            throw new ExcepcionReglaNegocio("El precio debe ser de al menos 0.01 USD");
        }

        PerfilCreador perfil = perfilRepository.findById(idPerfilCreador)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Perfil creador no encontrado con ID: " + idPerfilCreador));

        validarPropiedadOAdmin(perfil);

        Subcategoria subcategoria = subcategoriaRepository.findById(peticion.getIdSubcategoria())
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Subcategoria no encontrada con ID: " + peticion.getIdSubcategoria()));

        Servicio servicio = Servicio.builder()
                .perfil(perfil)
                .subcategoria(subcategoria)
                .tituloServicio(peticion.getTituloServicio().trim())
                .descripcionDetallada(peticion.getDescripcionDetallada().trim())
                .precioBase(peticion.getPrecioBase())
                .urlMiniatura(peticion.getUrlMiniatura())
                .tipoItem(peticion.getTipoItem() != null ? peticion.getTipoItem() : "SERVICIO")
                .estadoPublicacion("ACTIVO")
                .cargoRevisionAdicional(peticion.getCargoRevisionAdicional() != null ? peticion.getCargoRevisionAdicional() : BigDecimal.ZERO)
                .limiteRevisionesBase(peticion.getLimiteRevisionesBase() != null ? peticion.getLimiteRevisionesBase() : 0)
                .build();

        Servicio guardado = servicioRepository.save(servicio);

        guardarEtiquetasServicio(guardado, peticion.getEtiquetaIds());

        return obtenerServicioPorId(guardado.getIdServicio());
    }

    @Override
    @Transactional
    public RespuestaServicio actualizarServicio(Long idServicio, PeticionActualizarServicio peticion) {
        if (peticion.getPrecioBase() == null || peticion.getPrecioBase().compareTo(new BigDecimal("0.01")) < 0) {
            throw new ExcepcionReglaNegocio("El precio debe ser de al menos 0.01 USD");
        }

        Servicio servicio = servicioRepository.findById(idServicio)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Servicio no encontrado con ID: " + idServicio));

        validarPropiedadOAdmin(servicio.getPerfil());

        if (peticion.getIdSubcategoria() != null && !peticion.getIdSubcategoria().equals(servicio.getSubcategoria().getIdSubcategoria())) {
            Subcategoria subcategoria = subcategoriaRepository.findById(peticion.getIdSubcategoria())
                    .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Subcategoria no encontrada con ID: " + peticion.getIdSubcategoria()));
            servicio.setSubcategoria(subcategoria);
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
            servicio.setEstadoPublicacion(peticion.getEstadoPublicacion());
        }
        servicio.setUrlMiniatura(peticion.getUrlMiniatura());
        if (peticion.getCargoRevisionAdicional() != null) {
            servicio.setCargoRevisionAdicional(peticion.getCargoRevisionAdicional());
        }
        if (peticion.getLimiteRevisionesBase() != null) {
            servicio.setLimiteRevisionesBase(peticion.getLimiteRevisionesBase());
        }

        Servicio guardado = servicioRepository.save(servicio);

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
    public void eliminarServicio(Long idServicio) {
        Servicio servicio = servicioRepository.findById(idServicio)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Servicio no encontrado con ID: " + idServicio));

        validarPropiedadOAdmin(servicio.getPerfil());

        servicioEtiquetaRepository.deleteByServicioIdServicio(idServicio);
        servicioRepository.delete(servicio);
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

        return paginaServicios.map(this::mapearAServicioResumido);
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
                .idSubcategoria(servicio.getSubcategoria().getIdSubcategoria())
                .nombreSubcategoria(servicio.getSubcategoria().getNombreSubcategoria())
                .idCategoria(servicio.getSubcategoria().getCategoria().getIdCategoria())
                .nombreCategoria(servicio.getSubcategoria().getCategoria().getNombreCategoria())
                .idPerfilCreador(servicio.getPerfil().getIdPerfil())
                .nombreCreador(nombreCreador)
                .atributos(atributos)
                .etiquetas(etiquetas)
                .actualizadoEn(servicio.getActualizadoEn())
                .build();
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
                .idSubcategoria(servicio.getSubcategoria().getIdSubcategoria())
                .nombreSubcategoria(servicio.getSubcategoria().getNombreSubcategoria())
                .idCategoria(servicio.getSubcategoria().getCategoria().getIdCategoria())
                .nombreCategoria(servicio.getSubcategoria().getCategoria().getNombreCategoria())
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
