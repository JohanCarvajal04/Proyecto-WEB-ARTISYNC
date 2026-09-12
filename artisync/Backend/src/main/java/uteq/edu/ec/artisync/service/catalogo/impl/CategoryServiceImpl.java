package uteq.edu.ec.artisync.service.catalogo.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.peticion.catalogo.UpdateCategoryRequest;
import uteq.edu.ec.artisync.dto.peticion.catalogo.CreateCategoryRequest;
import uteq.edu.ec.artisync.dto.peticion.catalogo.CreateSubcategoryRequest;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.CategoryResponse;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.SubcategoryResponse;
import uteq.edu.ec.artisync.entity.catalogo.Category;
import uteq.edu.ec.artisync.entity.catalogo.Subcategory;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.catalogo.CategoryRepository;
import uteq.edu.ec.artisync.repository.catalogo.OfferingSubcategoryRepository;
import uteq.edu.ec.artisync.repository.catalogo.SubcategoryRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;
import uteq.edu.ec.artisync.service.catalogo.ICategoryService;
import uteq.edu.ec.artisync.service.comunicacion.NotificationService;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements ICategoryService {

    private final CategoryRepository categoriaRepository;
    private final SubcategoryRepository subcategoriaRepository;
    private final OfferingSubcategoryRepository servicioSubcategoriaRepository;
    private final UserRepository usuarioRepository;
    private final NotificationService notificacionService;

    /** @return las categorías activas y ya revisadas, para el catálogo público, ordenadas alfabéticamente */
    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> listarCategoriasActivas() {
        return categoriaRepository.findByEstadoActivaTrueOrderByNombreCategoriaAsc()
                .stream()
                .map(this::mapearACategoriaRespuesta)
                .collect(Collectors.toList());
    }

    /** @return todas las categorías (incluidas inactivas y sin revisar), para administración */
    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> listarTodasLasCategorias() {
        return categoriaRepository.findAllByOrderByNombreCategoriaAsc()
                .stream()
                .map(this::mapearACategoriaRespuesta)
                .collect(Collectors.toList());
    }

    /**
     * @param idCategoria identificador de la categoría
     * @return la categoría solicitada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la categoría no existe
     */
    @Override
    @Transactional(readOnly = true)
    public CategoryResponse obtenerCategoriaPorId(Long idCategoria) {
        Category cat = categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new ResourceNotFoundException("Category no encontrada con ID: " + idCategoria));
        return mapearACategoriaRespuesta(cat);
    }

    /**
     * Crea una categoría. Si {@code idUsuarioCreador} no es {@code null} (autoservicio de un
     * creador), la categoría queda sin revisar; si es {@code null} (admin/moderador), queda ya revisada.
     *
     * @param idUsuarioCreador identificador del creador que la crea; {@code null} si la crea un admin/moderador
     * @param peticion nombre y estado activo de la categoría
     * @return la categoría creada
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si ya existe una categoría con ese nombre
     */
    @Override
    @Transactional
    @Auditable(accion = "CATEGORIA_CREAR", modulo = AuditModule.CATALOGO,
            entidad = "categorias", idEntidad = "#resultado.idCategoria",
            detalle = "{nombreCategoria: #peticion.nombreCategoria}")
    public CategoryResponse crearCategoria(Long idUsuarioCreador, CreateCategoryRequest peticion) {
        if (categoriaRepository.existsByNombreCategoriaIgnoreCase(peticion.getNombreCategoria())) {
            throw new BusinessRuleException("Ya existe una categoria con el nombre: " + peticion.getNombreCategoria());
        }
        Category cat = Category.builder()
                .nombreCategoria(peticion.getNombreCategoria().trim())
                .estadoActiva(peticion.getEstadoActiva() != null ? peticion.getEstadoActiva() : true)
                .creador(resolverCreador(idUsuarioCreador))
                .revisado(idUsuarioCreador == null)
                .build();
        cat = categoriaRepository.save(cat);
        return mapearACategoriaRespuesta(cat);
    }

    /**
     * Actualiza el nombre y/o estado activo de una categoría; los campos {@code null} no se modifican.
     *
     * @param idCategoria identificador de la categoría
     * @param peticion nuevo nombre y/o estado activo
     * @return la categoría ya actualizada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la categoría no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si ya existe otra categoría con el nuevo nombre
     */
    @Override
    @Transactional
    @Auditable(accion = "CATEGORIA_ACTUALIZAR", modulo = AuditModule.CATALOGO,
            entidad = "categorias", idEntidad = "#idCategoria",
            detalle = "{nombreCategoria: #peticion.nombreCategoria, estadoActiva: #peticion.estadoActiva}")
    public CategoryResponse actualizarCategoria(Long idCategoria, UpdateCategoryRequest peticion) {
        Category cat = categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new ResourceNotFoundException("Category no encontrada con ID: " + idCategoria));

        if (peticion.getNombreCategoria() != null && !peticion.getNombreCategoria().isBlank()) {
            if (!cat.getNombreCategoria().equalsIgnoreCase(peticion.getNombreCategoria()) &&
                    categoriaRepository.existsByNombreCategoriaIgnoreCase(peticion.getNombreCategoria())) {
                throw new BusinessRuleException("Ya existe una categoria con el nombre: " + peticion.getNombreCategoria());
            }
            cat.setNombreCategoria(peticion.getNombreCategoria().trim());
        }
        if (peticion.getEstadoActiva() != null) {
            cat.setEstadoActiva(peticion.getEstadoActiva());
        }
        cat = categoriaRepository.save(cat);
        return mapearACategoriaRespuesta(cat);
    }

    /**
     * Elimina una categoría, siempre que ninguna de sus subcategorías tenga
     * servicios publicados. Si la categoría la creó un creador, notifica el motivo.
     *
     * @param idCategoria identificador de la categoría a eliminar
     * @param motivo justificación de la eliminación; obligatorio si la categoría la creó un creador
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la categoría no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si alguna de sus subcategorías tiene
     *         servicios publicados, o si la creó un creador y no se indica motivo
     */
    @Override
    @Transactional
    @Auditable(accion = "CATEGORIA_ELIMINAR", modulo = AuditModule.CATALOGO,
            entidad = "categorias", idEntidad = "#idCategoria", detalle = "{motivo: #motivo}")
    public void eliminarCategoria(Long idCategoria, String motivo) {
        Category cat = categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new ResourceNotFoundException("Category no encontrada con ID: " + idCategoria));
        // subcategorias.id_categoria cascada al borrar la categoria, pero
        // servicios.id_subcategoria NO cascada desde subcategorias: si alguna
        // subcategoria de esta categoria tiene servicios, el DELETE fallaria
        // a mitad de camino con una DataIntegrityViolationException cruda.
        if (servicioSubcategoriaRepository.existsBySubcategoriaCategoriaIdCategoria(idCategoria)) {
            throw new BusinessRuleException(
                    "No se puede eliminar la categoria: tiene servicios publicados en alguna de sus subcategorias.");
        }
        if (cat.getCreador() != null && (motivo == null || motivo.isBlank())) {
            throw new BusinessRuleException("Debes indicar un motivo para eliminar una categoria creada por un creador");
        }
        categoriaRepository.deleteById(idCategoria);
        if (cat.getCreador() != null) {
            notificacionService.notify(cat.getCreador(), "CATEGORIA_ELIMINADA",
                    "Tu categoria '" + cat.getNombreCategoria() + "' fue eliminada por un moderador. Motivo: " + motivo);
        }
    }

    /**
     * @param idCategoria identificador de la categoría
     * @return las subcategorías de esa categoría, ordenadas alfabéticamente
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la categoría no existe
     */
    @Override
    @Transactional(readOnly = true)
    public List<SubcategoryResponse> listarSubcategoriasPorCategoria(Long idCategoria) {
        if (!categoriaRepository.existsById(idCategoria)) {
            throw new ResourceNotFoundException("Category no encontrada con ID: " + idCategoria);
        }
        return subcategoriaRepository.findByCategoriaIdCategoriaOrderByNombreSubcategoriaAsc(idCategoria)
                .stream()
                .map(this::mapearASubcategoriaRespuesta)
                .collect(Collectors.toList());
    }

    /** @return todas las subcategorías del catálogo, ordenadas alfabéticamente */
    @Override
    @Transactional(readOnly = true)
    public List<SubcategoryResponse> listarTodasLasSubcategorias() {
        return subcategoriaRepository.findAllByOrderByNombreSubcategoriaAsc()
                .stream()
                .map(this::mapearASubcategoriaRespuesta)
                .collect(Collectors.toList());
    }

    /**
     * Crea una subcategoría bajo una categoría existente. Si {@code idUsuarioCreador} no es
     * {@code null} (autoservicio de un creador), queda sin revisar; si es {@code null} (admin/moderador),
     * queda ya revisada.
     *
     * @param idUsuarioCreador identificador del creador que la crea; {@code null} si la crea un admin/moderador
     * @param peticion categoría a la que pertenece y nombre de la subcategoría
     * @return la subcategoría creada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la categoría no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si ya existe esa subcategoría en esa categoría
     */
    @Override
    @Transactional
    @Auditable(accion = "SUBCATEGORIA_CREAR", modulo = AuditModule.CATALOGO,
            entidad = "subcategorias", idEntidad = "#resultado.idSubcategoria",
            detalle = "{idCategoria: #peticion.idCategoria, nombreSubcategoria: #peticion.nombreSubcategoria}")
    public SubcategoryResponse crearSubcategoria(Long idUsuarioCreador, CreateSubcategoryRequest peticion) {
        Category cat = categoriaRepository.findById(peticion.getIdCategoria())
                .orElseThrow(() -> new ResourceNotFoundException("Category no encontrada con ID: " + peticion.getIdCategoria()));

        if (subcategoriaRepository.existsByCategoriaIdCategoriaAndNombreSubcategoriaIgnoreCase(
                cat.getIdCategoria(), peticion.getNombreSubcategoria())) {
            throw new BusinessRuleException("Ya existe la subcategoria " + peticion.getNombreSubcategoria() + " en esta categoria");
        }

        Subcategory sub = Subcategory.builder()
                .categoria(cat)
                .nombreSubcategoria(peticion.getNombreSubcategoria().trim())
                .creador(resolverCreador(idUsuarioCreador))
                .revisado(idUsuarioCreador == null)
                .build();
        sub = subcategoriaRepository.save(sub);
        return mapearASubcategoriaRespuesta(sub);
    }

    /**
     * Elimina una subcategoría, siempre que no tenga servicios publicados. Si
     * la creó un creador, notifica el motivo.
     *
     * @param idSubcategoria identificador de la subcategoría a eliminar
     * @param motivo justificación de la eliminación; obligatorio si la subcategoría la creó un creador
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la subcategoría no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si tiene servicios publicados,
     *         o si la creó un creador y no se indica motivo
     */
    @Override
    @Transactional
    @Auditable(accion = "SUBCATEGORIA_ELIMINAR", modulo = AuditModule.CATALOGO,
            entidad = "subcategorias", idEntidad = "#idSubcategoria", detalle = "{motivo: #motivo}")
    public void eliminarSubcategoria(Long idSubcategoria, String motivo) {
        Subcategory sub = subcategoriaRepository.findById(idSubcategoria)
                .orElseThrow(() -> new ResourceNotFoundException("Subcategory no encontrada con ID: " + idSubcategoria));
        if (servicioSubcategoriaRepository.existsBySubcategoriaIdSubcategoria(idSubcategoria)) {
            throw new BusinessRuleException(
                    "No se puede eliminar la subcategoria: tiene servicios publicados.");
        }
        if (sub.getCreador() != null && (motivo == null || motivo.isBlank())) {
            throw new BusinessRuleException("Debes indicar un motivo para eliminar una subcategoria creada por un creador");
        }
        subcategoriaRepository.deleteById(idSubcategoria);
        if (sub.getCreador() != null) {
            notificacionService.notify(sub.getCreador(), "SUBCATEGORIA_ELIMINADA",
                    "Tu subcategoria '" + sub.getNombreSubcategoria() + "' fue eliminada por un moderador. Motivo: " + motivo);
        }
    }

    /** @return las categorías creadas por creadores aún sin revisar, más recientes primero */
    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> listarCategoriasPendientesRevision() {
        return categoriaRepository.findByRevisadoFalseOrderByActualizadoEnDesc()
                .stream()
                .map(this::mapearACategoriaRespuesta)
                .collect(Collectors.toList());
    }

    /** @return las subcategorías creadas por creadores aún sin revisar, más recientes primero */
    @Override
    @Transactional(readOnly = true)
    public List<SubcategoryResponse> listarSubcategoriasPendientesRevision() {
        return subcategoriaRepository.findByRevisadoFalseOrderByActualizadoEnDesc()
                .stream()
                .map(this::mapearASubcategoriaRespuesta)
                .collect(Collectors.toList());
    }

    /**
     * Marca una categoría creada por un creador como ya revisada por un moderador.
     *
     * @param idCategoria identificador de la categoría
     * @return la categoría ya marcada como revisada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la categoría no existe
     */
    @Override
    @Transactional
    @Auditable(accion = "CATEGORIA_REVISAR", modulo = AuditModule.CATALOGO,
            entidad = "categorias", idEntidad = "#idCategoria")
    public CategoryResponse marcarCategoriaRevisada(Long idCategoria) {
        Category cat = categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new ResourceNotFoundException("Category no encontrada con ID: " + idCategoria));
        cat.setRevisado(true);
        cat = categoriaRepository.save(cat);
        return mapearACategoriaRespuesta(cat);
    }

    /**
     * Marca una subcategoría creada por un creador como ya revisada por un moderador.
     *
     * @param idSubcategoria identificador de la subcategoría
     * @return la subcategoría ya marcada como revisada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la subcategoría no existe
     */
    @Override
    @Transactional
    @Auditable(accion = "SUBCATEGORIA_REVISAR", modulo = AuditModule.CATALOGO,
            entidad = "subcategorias", idEntidad = "#idSubcategoria")
    public SubcategoryResponse marcarSubcategoriaRevisada(Long idSubcategoria) {
        Subcategory sub = subcategoriaRepository.findById(idSubcategoria)
                .orElseThrow(() -> new ResourceNotFoundException("Subcategory no encontrada con ID: " + idSubcategoria));
        sub.setRevisado(true);
        sub = subcategoriaRepository.save(sub);
        return mapearASubcategoriaRespuesta(sub);
    }

    private User resolverCreador(Long idUsuarioCreador) {
        if (idUsuarioCreador == null) {
            return null;
        }
        return usuarioRepository.findById(idUsuarioCreador)
                .orElseThrow(() -> new ResourceNotFoundException("User no encontrado con ID: " + idUsuarioCreador));
    }

    private CategoryResponse mapearACategoriaRespuesta(Category cat) {
        return CategoryResponse.builder()
                .idCategoria(cat.getIdCategoria())
                .nombreCategoria(cat.getNombreCategoria())
                .estadoActiva(cat.getEstadoActiva())
                .idUsuarioCreador(cat.getCreador() != null ? cat.getCreador().getIdUsuario() : null)
                .nombreCreador(cat.getCreador() != null ? cat.getCreador().getNombres() + " " + cat.getCreador().getApellidos() : null)
                .revisado(cat.getRevisado())
                .actualizadoEn(cat.getActualizadoEn())
                .build();
    }

    private SubcategoryResponse mapearASubcategoriaRespuesta(Subcategory sub) {
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
}
