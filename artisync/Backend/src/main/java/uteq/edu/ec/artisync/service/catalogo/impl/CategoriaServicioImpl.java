package uteq.edu.ec.artisync.service.catalogo.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionActualizarCategoria;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearCategoria;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearSubcategoria;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaCategoria;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaSubcategoria;
import uteq.edu.ec.artisync.entity.catalogo.Categoria;
import uteq.edu.ec.artisync.entity.catalogo.Subcategoria;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.catalogo.CategoriaRepository;
import uteq.edu.ec.artisync.repository.catalogo.ServicioSubcategoriaRepository;
import uteq.edu.ec.artisync.repository.catalogo.SubcategoriaRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;
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
    private final UserRepository usuarioRepository;
    private final NotificacionService notificacionService;

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<RespuestaCategoria> listarCategoriasActivas() {
        return categoriaRepository.findByEstadoActivaTrueOrderByNombreCategoriaAsc()
                .stream()
                .map(this::mapearACategoriaRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<RespuestaCategoria> listarTodasLasCategorias() {
        return categoriaRepository.findAllByOrderByNombreCategoriaAsc()
                .stream()
                .map(this::mapearACategoriaRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param idCategoria identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaCategoria obtenerCategoriaPorId(Long idCategoria) {
        Categoria cat = categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada con ID: " + idCategoria));
        return mapearACategoriaRespuesta(cat);
    }

    @Override
    @Transactional
    @Auditable(accion = "CATEGORIA_CREAR", modulo = AuditModule.CATALOGO,
            entidad = "categorias", idEntidad = "#resultado.idCategoria",
            detalle = "{nombreCategoria: #peticion.nombreCategoria}")
    /**
     * Procesa y persiste la creacion de un nuevo recurso en el contexto de negocio aplicable.
     *
     * @param idUsuarioCreador identificador unico que referencia de manera univoca al registro
     * @param peticion estructura de transferencia de datos con la informacion estructurada de entrada
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaCategoria crearCategoria(Long idUsuarioCreador, PeticionCrearCategoria peticion) {
        if (categoriaRepository.existsByNombreCategoriaIgnoreCase(peticion.getNombreCategoria())) {
            throw new BusinessRuleException("Ya existe una categoria con el nombre: " + peticion.getNombreCategoria());
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
    @Auditable(accion = "CATEGORIA_ACTUALIZAR", modulo = AuditModule.CATALOGO,
            entidad = "categorias", idEntidad = "#idCategoria",
            detalle = "{nombreCategoria: #peticion.nombreCategoria, estadoActiva: #peticion.estadoActiva}")
    /**
     * Aplica modificaciones y validaciones de negocio sobre los datos de un registro existente.
     *
     * @param idCategoria identificador unico que referencia de manera univoca al registro
     * @param peticion estructura de transferencia de datos con la informacion estructurada de entrada
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaCategoria actualizarCategoria(Long idCategoria, PeticionActualizarCategoria peticion) {
        Categoria cat = categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada con ID: " + idCategoria));

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

    @Override
    @Transactional
    @Auditable(accion = "CATEGORIA_ELIMINAR", modulo = AuditModule.CATALOGO,
            entidad = "categorias", idEntidad = "#idCategoria", detalle = "{motivo: #motivo}")
    /**
     * Ejecuta la eliminacion logica o fisica del registro indicado, comprobando dependencias previas.
     *
     * @param idCategoria identificador unico que referencia de manera univoca al registro
     * @param motivo parametro requerido para la correcta ejecucion del procedimiento
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public void eliminarCategoria(Long idCategoria, String motivo) {
        Categoria cat = categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada con ID: " + idCategoria));
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
            notificacionService.notificar(cat.getCreador(), "CATEGORIA_ELIMINADA",
                    "Tu categoria '" + cat.getNombreCategoria() + "' fue eliminada por un moderador. Motivo: " + motivo);
        }
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @param idCategoria identificador unico que referencia de manera univoca al registro
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<RespuestaSubcategoria> listarSubcategoriasPorCategoria(Long idCategoria) {
        if (!categoriaRepository.existsById(idCategoria)) {
            throw new ResourceNotFoundException("Categoria no encontrada con ID: " + idCategoria);
        }
        return subcategoriaRepository.findByCategoriaIdCategoriaOrderByNombreSubcategoriaAsc(idCategoria)
                .stream()
                .map(this::mapearASubcategoriaRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<RespuestaSubcategoria> listarTodasLasSubcategorias() {
        return subcategoriaRepository.findAllByOrderByNombreSubcategoriaAsc()
                .stream()
                .map(this::mapearASubcategoriaRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Auditable(accion = "SUBCATEGORIA_CREAR", modulo = AuditModule.CATALOGO,
            entidad = "subcategorias", idEntidad = "#resultado.idSubcategoria",
            detalle = "{idCategoria: #peticion.idCategoria, nombreSubcategoria: #peticion.nombreSubcategoria}")
    /**
     * Procesa y persiste la creacion de un nuevo recurso en el contexto de negocio aplicable.
     *
     * @param idUsuarioCreador identificador unico que referencia de manera univoca al registro
     * @param peticion estructura de transferencia de datos con la informacion estructurada de entrada
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaSubcategoria crearSubcategoria(Long idUsuarioCreador, PeticionCrearSubcategoria peticion) {
        Categoria cat = categoriaRepository.findById(peticion.getIdCategoria())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada con ID: " + peticion.getIdCategoria()));

        if (subcategoriaRepository.existsByCategoriaIdCategoriaAndNombreSubcategoriaIgnoreCase(
                cat.getIdCategoria(), peticion.getNombreSubcategoria())) {
            throw new BusinessRuleException("Ya existe la subcategoria " + peticion.getNombreSubcategoria() + " en esta categoria");
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
    @Auditable(accion = "SUBCATEGORIA_ELIMINAR", modulo = AuditModule.CATALOGO,
            entidad = "subcategorias", idEntidad = "#idSubcategoria", detalle = "{motivo: #motivo}")
    /**
     * Ejecuta la eliminacion logica o fisica del registro indicado, comprobando dependencias previas.
     *
     * @param idSubcategoria identificador unico que referencia de manera univoca al registro
     * @param motivo parametro requerido para la correcta ejecucion del procedimiento
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public void eliminarSubcategoria(Long idSubcategoria, String motivo) {
        Subcategoria sub = subcategoriaRepository.findById(idSubcategoria)
                .orElseThrow(() -> new ResourceNotFoundException("Subcategoria no encontrada con ID: " + idSubcategoria));
        if (servicioSubcategoriaRepository.existsBySubcategoriaIdSubcategoria(idSubcategoria)) {
            throw new BusinessRuleException(
                    "No se puede eliminar la subcategoria: tiene servicios publicados.");
        }
        if (sub.getCreador() != null && (motivo == null || motivo.isBlank())) {
            throw new BusinessRuleException("Debes indicar un motivo para eliminar una subcategoria creada por un creador");
        }
        subcategoriaRepository.deleteById(idSubcategoria);
        if (sub.getCreador() != null) {
            notificacionService.notificar(sub.getCreador(), "SUBCATEGORIA_ELIMINADA",
                    "Tu subcategoria '" + sub.getNombreSubcategoria() + "' fue eliminada por un moderador. Motivo: " + motivo);
        }
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<RespuestaCategoria> listarCategoriasPendientesRevision() {
        return categoriaRepository.findByRevisadoFalseOrderByActualizadoEnDesc()
                .stream()
                .map(this::mapearACategoriaRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<RespuestaSubcategoria> listarSubcategoriasPendientesRevision() {
        return subcategoriaRepository.findByRevisadoFalseOrderByActualizadoEnDesc()
                .stream()
                .map(this::mapearASubcategoriaRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Auditable(accion = "CATEGORIA_REVISAR", modulo = AuditModule.CATALOGO,
            entidad = "categorias", idEntidad = "#idCategoria")
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idCategoria identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaCategoria marcarCategoriaRevisada(Long idCategoria) {
        Categoria cat = categoriaRepository.findById(idCategoria)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada con ID: " + idCategoria));
        cat.setRevisado(true);
        cat = categoriaRepository.save(cat);
        return mapearACategoriaRespuesta(cat);
    }

    @Override
    @Transactional
    @Auditable(accion = "SUBCATEGORIA_REVISAR", modulo = AuditModule.CATALOGO,
            entidad = "subcategorias", idEntidad = "#idSubcategoria")
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idSubcategoria identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaSubcategoria marcarSubcategoriaRevisada(Long idSubcategoria) {
        Subcategoria sub = subcategoriaRepository.findById(idSubcategoria)
                .orElseThrow(() -> new ResourceNotFoundException("Subcategoria no encontrada con ID: " + idSubcategoria));
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
