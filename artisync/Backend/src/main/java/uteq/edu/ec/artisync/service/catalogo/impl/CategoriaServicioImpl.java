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
import uteq.edu.ec.artisync.entity.catalogo.Subcategoria;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.catalogo.CategoriaRepository;
import uteq.edu.ec.artisync.repository.catalogo.ServicioSubcategoriaRepository;
import uteq.edu.ec.artisync.repository.catalogo.SubcategoriaRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.service.catalogo.ICategoriaServicio;
import uteq.edu.ec.artisync.service.comunicacion.NotificacionService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoriaServicioImpl implements ICategoriaServicio {

    private final CategoriaRepository categoriaRepository;
    private final SubcategoriaRepository subcategoriaRepository;
    private final ServicioSubcategoriaRepository servicioSubcategoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final NotificacionService notificacionService;

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
    public RespuestaCategoria crearCategoria(Long idUsuarioCreador, PeticionCrearCategoria peticion) {
        if (categoriaRepository.existsByNombreCategoriaIgnoreCase(peticion.getNombreCategoria())) {
            throw new ExcepcionReglaNegocio("Ya existe una categoria con el nombre: " + peticion.getNombreCategoria());
        }
        Categoria cat = Categoria.builder()
                .nombreCategoria(peticion.getNombreCategoria().trim())
                .estadoActiva(peticion.getEstadoActiva() != null ? peticion.getEstadoActiva() : true)
                .creador(resolverCreador(idUsuarioCreador))
                .revisado(idUsuarioCreador == null)
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
        cat = categoriaRepository.save(cat);
        return mapearACategoriaRespuesta(cat);
    }

    @Override
    @Transactional
    @Auditable(accion = "CATEGORIA_ELIMINAR", modulo = ModuloAuditoria.CATALOGO,
            entidad = "categorias", idEntidad = "#idCategoria", detalle = "{motivo: #motivo}")
    public void eliminarCategoria(Long idCategoria, String motivo) {
        Categoria cat = categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Categoria no encontrada con ID: " + idCategoria));
        // subcategorias.id_categoria cascada al borrar la categoria, pero
        // servicios.id_subcategoria NO cascada desde subcategorias: si alguna
        // subcategoria de esta categoria tiene servicios, el DELETE fallaria
        // a mitad de camino con una DataIntegrityViolationException cruda.
        if (servicioSubcategoriaRepository.existsBySubcategoriaCategoriaIdCategoria(idCategoria)) {
            throw new ExcepcionReglaNegocio(
                    "No se puede eliminar la categoria: tiene servicios publicados en alguna de sus subcategorias.");
        }
        if (cat.getCreador() != null && (motivo == null || motivo.isBlank())) {
            throw new ExcepcionReglaNegocio("Debes indicar un motivo para eliminar una categoria creada por un creador");
        }
        categoriaRepository.deleteById(idCategoria);
        if (cat.getCreador() != null) {
            notificacionService.notificar(cat.getCreador(), "CATEGORIA_ELIMINADA",
                    "Tu categoria '" + cat.getNombreCategoria() + "' fue eliminada por un moderador. Motivo: " + motivo);
        }
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
    public RespuestaSubcategoria crearSubcategoria(Long idUsuarioCreador, PeticionCrearSubcategoria peticion) {
        Categoria cat = categoriaRepository.findById(peticion.getIdCategoria())
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Categoria no encontrada con ID: " + peticion.getIdCategoria()));

        if (subcategoriaRepository.existsByCategoriaIdCategoriaAndNombreSubcategoriaIgnoreCase(
                cat.getIdCategoria(), peticion.getNombreSubcategoria())) {
            throw new ExcepcionReglaNegocio("Ya existe la subcategoria " + peticion.getNombreSubcategoria() + " en esta categoria");
        }

        Subcategoria sub = Subcategoria.builder()
                .categoria(cat)
                .nombreSubcategoria(peticion.getNombreSubcategoria().trim())
                .creador(resolverCreador(idUsuarioCreador))
                .revisado(idUsuarioCreador == null)
                .build();
        sub = subcategoriaRepository.save(sub);
        return mapearASubcategoriaRespuesta(sub);
    }

    @Override
    @Transactional
    @Auditable(accion = "SUBCATEGORIA_ELIMINAR", modulo = ModuloAuditoria.CATALOGO,
            entidad = "subcategorias", idEntidad = "#idSubcategoria", detalle = "{motivo: #motivo}")
    public void eliminarSubcategoria(Long idSubcategoria, String motivo) {
        Subcategoria sub = subcategoriaRepository.findById(idSubcategoria)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Subcategoria no encontrada con ID: " + idSubcategoria));
        if (servicioSubcategoriaRepository.existsBySubcategoriaIdSubcategoria(idSubcategoria)) {
            throw new ExcepcionReglaNegocio(
                    "No se puede eliminar la subcategoria: tiene servicios publicados.");
        }
        if (sub.getCreador() != null && (motivo == null || motivo.isBlank())) {
            throw new ExcepcionReglaNegocio("Debes indicar un motivo para eliminar una subcategoria creada por un creador");
        }
        subcategoriaRepository.deleteById(idSubcategoria);
        if (sub.getCreador() != null) {
            notificacionService.notificar(sub.getCreador(), "SUBCATEGORIA_ELIMINADA",
                    "Tu subcategoria '" + sub.getNombreSubcategoria() + "' fue eliminada por un moderador. Motivo: " + motivo);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaCategoria> listarCategoriasPendientesRevision() {
        return categoriaRepository.findByRevisadoFalseOrderByActualizadoEnDesc()
                .stream()
                .map(this::mapearACategoriaRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaSubcategoria> listarSubcategoriasPendientesRevision() {
        return subcategoriaRepository.findByRevisadoFalseOrderByActualizadoEnDesc()
                .stream()
                .map(this::mapearASubcategoriaRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Auditable(accion = "CATEGORIA_REVISAR", modulo = ModuloAuditoria.CATALOGO,
            entidad = "categorias", idEntidad = "#idCategoria")
    public RespuestaCategoria marcarCategoriaRevisada(Long idCategoria) {
        Categoria cat = categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Categoria no encontrada con ID: " + idCategoria));
        cat.setRevisado(true);
        cat = categoriaRepository.save(cat);
        return mapearACategoriaRespuesta(cat);
    }

    @Override
    @Transactional
    @Auditable(accion = "SUBCATEGORIA_REVISAR", modulo = ModuloAuditoria.CATALOGO,
            entidad = "subcategorias", idEntidad = "#idSubcategoria")
    public RespuestaSubcategoria marcarSubcategoriaRevisada(Long idSubcategoria) {
        Subcategoria sub = subcategoriaRepository.findById(idSubcategoria)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Subcategoria no encontrada con ID: " + idSubcategoria));
        sub.setRevisado(true);
        sub = subcategoriaRepository.save(sub);
        return mapearASubcategoriaRespuesta(sub);
    }

    private Usuario resolverCreador(Long idUsuarioCreador) {
        if (idUsuarioCreador == null) {
            return null;
        }
        return usuarioRepository.findById(idUsuarioCreador)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Usuario no encontrado con ID: " + idUsuarioCreador));
    }

    private RespuestaCategoria mapearACategoriaRespuesta(Categoria cat) {
        return RespuestaCategoria.builder()
                .idCategoria(cat.getIdCategoria())
                .nombreCategoria(cat.getNombreCategoria())
                .estadoActiva(cat.getEstadoActiva())
                .idUsuarioCreador(cat.getCreador() != null ? cat.getCreador().getIdUsuario() : null)
                .nombreCreador(cat.getCreador() != null ? cat.getCreador().getNombres() + " " + cat.getCreador().getApellidos() : null)
                .revisado(cat.getRevisado())
                .actualizadoEn(cat.getActualizadoEn())
                .build();
    }

    private RespuestaSubcategoria mapearASubcategoriaRespuesta(Subcategoria sub) {
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
}
