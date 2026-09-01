package uteq.edu.ec.artisync.service.catalogo.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionActualizarCategoria;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearCategoria;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearSubcategoria;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaCategoria;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaSubcategoria;
import uteq.edu.ec.artisync.entity.catalogo.Categoria;
import uteq.edu.ec.artisync.entity.catalogo.FlujoTrabajo;
import uteq.edu.ec.artisync.entity.catalogo.Subcategoria;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.catalogo.CategoriaRepository;
import uteq.edu.ec.artisync.repository.catalogo.FlujoTrabajoRepository;
import uteq.edu.ec.artisync.repository.catalogo.ServicioRepository;
import uteq.edu.ec.artisync.repository.catalogo.SubcategoriaRepository;
import uteq.edu.ec.artisync.service.catalogo.ICategoriaServicio;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoriaServicioImpl implements ICategoriaServicio {

    private final CategoriaRepository categoriaRepository;
    private final SubcategoriaRepository subcategoriaRepository;
    private final FlujoTrabajoRepository flujoTrabajoRepository;
    private final ServicioRepository servicioRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaCategoria> listarCategoriasActivas() {
        return categoriaRepository.findByEstadoActivaTrueOrderByNombreCategoriaAsc()
                .stream()
                .map(this::mapearACategoriaRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaCategoria> listarTodasLasCategorias() {
        return categoriaRepository.findAllByOrderByNombreCategoriaAsc()
                .stream()
                .map(this::mapearACategoriaRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaCategoria obtenerCategoriaPorId(Long idCategoria) {
        Categoria cat = categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Categoria no encontrada con ID: " + idCategoria));
        return mapearACategoriaRespuesta(cat);
    }

    @Override
    @Transactional
    @Auditable(accion = "CATEGORIA_CREAR", modulo = ModuloAuditoria.CATALOGO,
            entidad = "categorias", idEntidad = "#resultado.idCategoria",
            detalle = "{nombreCategoria: #peticion.nombreCategoria}")
    public RespuestaCategoria crearCategoria(PeticionCrearCategoria peticion) {
        if (categoriaRepository.existsByNombreCategoriaIgnoreCase(peticion.getNombreCategoria())) {
            throw new ExcepcionReglaNegocio("Ya existe una categoria con el nombre: " + peticion.getNombreCategoria());
        }
        Categoria cat = Categoria.builder()
                .nombreCategoria(peticion.getNombreCategoria().trim())
                .estadoActiva(peticion.getEstadoActiva() != null ? peticion.getEstadoActiva() : true)
                .flujo(resolverFlujo(peticion.getIdFlujo()))
                .build();
        cat = categoriaRepository.save(cat);
        return mapearACategoriaRespuesta(cat);
    }

    @Override
    @Transactional
    @Auditable(accion = "CATEGORIA_ACTUALIZAR", modulo = ModuloAuditoria.CATALOGO,
            entidad = "categorias", idEntidad = "#idCategoria",
            detalle = "{nombreCategoria: #peticion.nombreCategoria, estadoActiva: #peticion.estadoActiva}")
    public RespuestaCategoria actualizarCategoria(Long idCategoria, PeticionActualizarCategoria peticion) {
        Categoria cat = categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Categoria no encontrada con ID: " + idCategoria));

        if (peticion.getNombreCategoria() != null && !peticion.getNombreCategoria().isBlank()) {
            if (!cat.getNombreCategoria().equalsIgnoreCase(peticion.getNombreCategoria()) &&
                    categoriaRepository.existsByNombreCategoriaIgnoreCase(peticion.getNombreCategoria())) {
                throw new ExcepcionReglaNegocio("Ya existe una categoria con el nombre: " + peticion.getNombreCategoria());
            }
            cat.setNombreCategoria(peticion.getNombreCategoria().trim());
        }
        if (peticion.getEstadoActiva() != null) {
            cat.setEstadoActiva(peticion.getEstadoActiva());
        }
        if (peticion.getIdFlujo() != null) {
            cat.setFlujo(resolverFlujo(peticion.getIdFlujo()));
        }
        cat = categoriaRepository.save(cat);
        return mapearACategoriaRespuesta(cat);
    }

    @Override
    @Transactional
    @Auditable(accion = "CATEGORIA_ELIMINAR", modulo = ModuloAuditoria.CATALOGO,
            entidad = "categorias", idEntidad = "#idCategoria")
    public void eliminarCategoria(Long idCategoria) {
        if (!categoriaRepository.existsById(idCategoria)) {
            throw new ExcepcionRecursoNoEncontrado("Categoria no encontrada con ID: " + idCategoria);
        }
        // subcategorias.id_categoria cascada al borrar la categoria, pero
        // servicios.id_subcategoria NO cascada desde subcategorias: si alguna
        // subcategoria de esta categoria tiene servicios, el DELETE fallaria
        // a mitad de camino con una DataIntegrityViolationException cruda.
        if (servicioRepository.existsBySubcategoriaCategoriaIdCategoria(idCategoria)) {
            throw new ExcepcionReglaNegocio(
                    "No se puede eliminar la categoria: tiene servicios publicados en alguna de sus subcategorias.");
        }
        categoriaRepository.deleteById(idCategoria);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaSubcategoria> listarSubcategoriasPorCategoria(Long idCategoria) {
        if (!categoriaRepository.existsById(idCategoria)) {
            throw new ExcepcionRecursoNoEncontrado("Categoria no encontrada con ID: " + idCategoria);
        }
        return subcategoriaRepository.findByCategoriaIdCategoriaOrderByNombreSubcategoriaAsc(idCategoria)
                .stream()
                .map(this::mapearASubcategoriaRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaSubcategoria> listarTodasLasSubcategorias() {
        return subcategoriaRepository.findAllByOrderByNombreSubcategoriaAsc()
                .stream()
                .map(this::mapearASubcategoriaRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Auditable(accion = "SUBCATEGORIA_CREAR", modulo = ModuloAuditoria.CATALOGO,
            entidad = "subcategorias", idEntidad = "#resultado.idSubcategoria",
            detalle = "{idCategoria: #peticion.idCategoria, nombreSubcategoria: #peticion.nombreSubcategoria}")
    public RespuestaSubcategoria crearSubcategoria(PeticionCrearSubcategoria peticion) {
        Categoria cat = categoriaRepository.findById(peticion.getIdCategoria())
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Categoria no encontrada con ID: " + peticion.getIdCategoria()));

        if (subcategoriaRepository.existsByCategoriaIdCategoriaAndNombreSubcategoriaIgnoreCase(
                cat.getIdCategoria(), peticion.getNombreSubcategoria())) {
            throw new ExcepcionReglaNegocio("Ya existe la subcategoria " + peticion.getNombreSubcategoria() + " en esta categoria");
        }

        Subcategoria sub = Subcategoria.builder()
                .categoria(cat)
                .nombreSubcategoria(peticion.getNombreSubcategoria().trim())
                .build();
        sub = subcategoriaRepository.save(sub);
        return mapearASubcategoriaRespuesta(sub);
    }

    @Override
    @Transactional
    @Auditable(accion = "SUBCATEGORIA_ELIMINAR", modulo = ModuloAuditoria.CATALOGO,
            entidad = "subcategorias", idEntidad = "#idSubcategoria")
    public void eliminarSubcategoria(Long idSubcategoria) {
        if (!subcategoriaRepository.existsById(idSubcategoria)) {
            throw new ExcepcionRecursoNoEncontrado("Subcategoria no encontrada con ID: " + idSubcategoria);
        }
        if (servicioRepository.existsBySubcategoriaIdSubcategoria(idSubcategoria)) {
            throw new ExcepcionReglaNegocio(
                    "No se puede eliminar la subcategoria: tiene servicios publicados.");
        }
        subcategoriaRepository.deleteById(idSubcategoria);
    }

    private RespuestaCategoria mapearACategoriaRespuesta(Categoria cat) {
        return RespuestaCategoria.builder()
                .idCategoria(cat.getIdCategoria())
                .nombreCategoria(cat.getNombreCategoria())
                .estadoActiva(cat.getEstadoActiva())
                .idFlujo(cat.getFlujo() != null ? cat.getFlujo().getIdFlujo() : null)
                .nombreFlujo(cat.getFlujo() != null ? cat.getFlujo().getNombreFlujo() : null)
                .actualizadoEn(cat.getActualizadoEn())
                .build();
    }

    /**
     * `null` es una respuesta válida: significa "sin flujo asignado", y el
     * servicio de pedidos ya cae a un flujo por defecto en ese caso. Un id que
     * no existe sí es un error del cliente y se rechaza.
     */
    private FlujoTrabajo resolverFlujo(Long idFlujo) {
        if (idFlujo == null) {
            return null;
        }
        return flujoTrabajoRepository.findById(idFlujo)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "Flujo de trabajo no encontrado con ID: " + idFlujo));
    }

    private RespuestaSubcategoria mapearASubcategoriaRespuesta(Subcategoria sub) {
        return RespuestaSubcategoria.builder()
                .idSubcategoria(sub.getIdSubcategoria())
                .idCategoria(sub.getCategoria().getIdCategoria())
                .nombreCategoria(sub.getCategoria().getNombreCategoria())
                .nombreSubcategoria(sub.getNombreSubcategoria())
                .actualizadoEn(sub.getActualizadoEn())
                .build();
    }
}
