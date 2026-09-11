package uteq.edu.ec.artisync.service.comunicacion.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionCrearComentario;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaComentario;
import uteq.edu.ec.artisync.entity.comunicacion.ComentarioPortafolio;
import uteq.edu.ec.artisync.entity.perfil.PortafolioItem;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.repository.comunicacion.ComentarioPortafolioRepository;
import uteq.edu.ec.artisync.repository.perfil.PortafolioItemRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;
import uteq.edu.ec.artisync.service.comunicacion.ComentarioPortafolioService;

/**
 * Implementación del servicio de comentarios sobre ítems de portafolio.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ComentarioPortafolioServiceImpl implements ComentarioPortafolioService {

    private static final String ESTADO_ACTIVO = "Activo";
    private static final String ESTADO_OCULTO = "Oculto";
    /**
     * REQ-F-010: el borrado que hace el propio autor o el dueño del portafolio
     * es lógico, no físico, precisamente para que quede "consultable por el
     * administrador" — un DELETE de fila lo haría desaparecer también para él.
     */
    private static final String ESTADO_ELIMINADO = "Eliminado";

    private final ComentarioPortafolioRepository comentarioRepository;
    private final PortafolioItemRepository portafolioItemRepository;
    private final UserRepository usuarioRepository;

    @Override
    @Transactional
    @Auditable(accion = "COMENTARIO_CREAR", modulo = AuditModule.COMUNICACION,
            entidad = "comentarios_portafolio", idEntidad = "#idItemPortafolio")
    /**
     * Procesa y persiste la creacion de un nuevo recurso en el contexto de negocio aplicable.
     *
     * @param idItemPortafolio identificador unico que referencia de manera univoca al registro
     * @param peticion estructura de transferencia de datos con la informacion estructurada de entrada
     * @param idUsuarioAutor identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaComentario crearComentario(Long idItemPortafolio, PeticionCrearComentario peticion, Long idUsuarioAutor) {
        PortafolioItem item = portafolioItemRepository.findById(idItemPortafolio)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ítem de portafolio no encontrado: " + idItemPortafolio));

        User autor = usuarioRepository.findById(idUsuarioAutor)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User no encontrado: " + idUsuarioAutor));

        ComentarioPortafolio comentario = comentarioRepository.save(ComentarioPortafolio.builder()
                .itemPortafolio(item)
                .usuarioAutor(autor)
                .textoComentario(peticion.getTextoComentario())
                .estadoModeracion(ESTADO_ACTIVO)
                .build());

        log.info("User {} comentó el ítem de portafolio {}", idUsuarioAutor, idItemPortafolio);
        return mapToResponse(comentario);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @param idItemPortafolio identificador unico que referencia de manera univoca al registro
     * @param pageable configuracion de paginacion y ordenamiento para la capa de datos
     * @return una estructura de datos paginada con la porcion de resultados solicitada y metadatos de pagina
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public Page<RespuestaComentario> listarComentarios(Long idItemPortafolio, Pageable pageable) {
        return comentarioRepository
                .findByItemPortafolioIdItemPortafolioAndEstadoModeracion(idItemPortafolio, ESTADO_ACTIVO, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idItemPortafolio identificador unico que referencia de manera univoca al registro
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public long contarComentarios(Long idItemPortafolio) {
        return comentarioRepository.countByItemPortafolioIdItemPortafolioAndEstadoModeracion(
                idItemPortafolio, ESTADO_ACTIVO);
    }

    /**
     * REQ-F-010: el autor o el dueño del portafolio hacen un borrado lógico
     * (estado "Eliminado"): desaparece de la vista pública pero el admin sigue
     * pudiendo consultarlo vía {@link #listarParaModeracion}. Solo el purgado
     * explícito de ADMIN (vía {@code AdminComentarioControlador}) borra la fila
     * de verdad; es una herramienta de moderación aparte, no lo que pide este
     * requisito.
     */
    @Override
    @Transactional
    @Auditable(accion = "COMENTARIO_ELIMINAR", modulo = AuditModule.COMUNICACION,
            entidad = "comentarios_portafolio", idEntidad = "#idComentario")
    /**
     * Ejecuta la eliminacion logica o fisica del registro indicado, comprobando dependencias previas.
     *
     * @param idComentario identificador unico que referencia de manera univoca al registro
     * @param idUsuarioSolicitante identificador unico que referencia de manera univoca al registro
     * @param esAdmin parametro requerido para la correcta ejecucion del procedimiento
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public void eliminarComentario(Long idComentario, Long idUsuarioSolicitante, boolean esAdmin) {
        ComentarioPortafolio comentario = obtenerComentario(idComentario);

        boolean esAutor = comentario.getUsuarioAutor() != null
                && comentario.getUsuarioAutor().getIdUsuario().equals(idUsuarioSolicitante);
        boolean esDuenoPortafolio = comentario.getItemPortafolio() != null
                && comentario.getItemPortafolio().getPortafolio() != null
                && comentario.getItemPortafolio().getPortafolio().getPerfil() != null
                && comentario.getItemPortafolio().getPortafolio().getPerfil().getUsuario() != null
                && comentario.getItemPortafolio().getPortafolio().getPerfil().getUsuario().getIdUsuario()
                        .equals(idUsuarioSolicitante);

        if (!esAdmin && !esAutor && !esDuenoPortafolio) {
            throw new AccessDeniedException("No tienes permisos para eliminar este comentario");
        }

        if (esAdmin) {
            comentarioRepository.delete(comentario);
            log.info("Comentario {} purgado definitivamente por un administrador", idComentario);
        } else {
            comentario.setEstadoModeracion(ESTADO_ELIMINADO);
            log.info("Comentario {} eliminado (borrado lógico) por usuario {}", idComentario, idUsuarioSolicitante);
        }
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @param pageable configuracion de paginacion y ordenamiento para la capa de datos
     * @return una estructura de datos paginada con la porcion de resultados solicitada y metadatos de pagina
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public Page<RespuestaComentario> listarParaModeracion(Pageable pageable) {
        return comentarioRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional
    @Auditable(accion = "COMENTARIO_OCULTAR", modulo = AuditModule.COMUNICACION,
            entidad = "comentarios_portafolio", idEntidad = "#idComentario")
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idComentario identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaComentario ocultarComentario(Long idComentario) {
        ComentarioPortafolio comentario = obtenerComentarioParaModerar(idComentario);
        comentario.setEstadoModeracion(ESTADO_OCULTO);
        log.info("Comentario {} ocultado por moderación", idComentario);
        return mapToResponse(comentario);
    }

    @Override
    @Transactional
    @Auditable(accion = "COMENTARIO_REACTIVAR", modulo = AuditModule.COMUNICACION,
            entidad = "comentarios_portafolio", idEntidad = "#idComentario")
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idComentario identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaComentario reactivarComentario(Long idComentario) {
        ComentarioPortafolio comentario = obtenerComentarioParaModerar(idComentario);
        comentario.setEstadoModeracion(ESTADO_ACTIVO);
        log.info("Comentario {} reactivado por moderación", idComentario);
        return mapToResponse(comentario);
    }

    // -------------------------------------------------------------------------

    /**
     * Con bloqueo pesimista de fila: ocultar/reactivar son las dos únicas
     * operaciones donde dos moderadores podrían pisarse la decisión sin
     * ningún aviso. La segunda llamada espera a que la primera confirme y
     * relee el estado ya actualizado antes de aplicar la suya.
     */
    private ComentarioPortafolio obtenerComentarioParaModerar(Long idComentario) {
        return comentarioRepository.findByIdParaModerar(idComentario)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Comentario no encontrado: " + idComentario));
    }

    private ComentarioPortafolio obtenerComentario(Long idComentario) {
        return comentarioRepository.findById(idComentario)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Comentario no encontrado: " + idComentario));
    }

    private RespuestaComentario mapToResponse(ComentarioPortafolio comentario) {
        User autor = comentario.getUsuarioAutor();
        String nombreAutor = autor == null ? null
                : autor.getNombres() + " " + autor.getApellidos();

        return RespuestaComentario.builder()
                .idComentario(comentario.getIdComentario())
                .idItemPortafolio(comentario.getItemPortafolio() == null ? null
                        : comentario.getItemPortafolio().getIdItemPortafolio())
                .idUsuarioAutor(autor == null ? null : autor.getIdUsuario())
                .nombreAutor(nombreAutor)
                .textoComentario(comentario.getTextoComentario())
                .estadoModeracion(comentario.getEstadoModeracion())
                .fechaPublicacion(comentario.getFechaPublicacion())
                .build();
    }
}
