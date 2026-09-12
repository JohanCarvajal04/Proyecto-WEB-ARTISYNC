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
import uteq.edu.ec.artisync.dto.peticion.comunicacion.CreateCommentRequest;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.CommentResponse;
import uteq.edu.ec.artisync.entity.comunicacion.PortfolioComment;
import uteq.edu.ec.artisync.entity.perfil.PortfolioItem;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.repository.comunicacion.PortfolioCommentRepository;
import uteq.edu.ec.artisync.repository.perfil.PortfolioItemRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;
import uteq.edu.ec.artisync.service.comunicacion.PortfolioCommentService;

/**
 * Implementación del servicio de comentarios sobre ítems de portafolio.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioCommentServiceImpl implements PortfolioCommentService {

    private static final String ESTADO_ACTIVO = "Activo";
    private static final String ESTADO_OCULTO = "Oculto";
    /**
     * REQ-F-010: el borrado que hace el propio autor o el dueño del portafolio
     * es lógico, no físico, precisamente para que quede "consultable por el
     * administrador" — un DELETE de fila lo haría desaparecer también para él.
     */
    private static final String ESTADO_ELIMINADO = "Eliminado";

    private final PortfolioCommentRepository comentarioRepository;
    private final PortfolioItemRepository portafolioItemRepository;
    private final UserRepository usuarioRepository;

    @Override
    @Transactional
    @Auditable(accion = "COMENTARIO_CREAR", modulo = AuditModule.COMUNICACION,
            entidad = "comentarios_portafolio", idEntidad = "#idItemPortafolio")
    /**
     * Crea un comentario sobre una obra del portafolio, en estado {@code Activo}.
     *
     * @param idItemPortafolio identificador de la obra comentada
     * @param peticion texto del comentario
     * @param idUsuarioAutor identificador del usuario que comenta
     * @return el comentario creado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la obra o el usuario no existen
     */
    public CommentResponse crearComentario(Long idItemPortafolio, CreateCommentRequest peticion, Long idUsuarioAutor) {
        PortfolioItem item = portafolioItemRepository.findById(idItemPortafolio)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Ítem de portafolio no encontrado: " + idItemPortafolio));

        User autor = usuarioRepository.findById(idUsuarioAutor)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User no encontrado: " + idUsuarioAutor));

        PortfolioComment comentario = comentarioRepository.save(PortfolioComment.builder()
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
     * @param idItemPortafolio identificador de la obra
     * @param pageable paginación y ordenamiento solicitados
     * @return los comentarios activos (públicos) de esa obra
     */
    public Page<CommentResponse> listarComentarios(Long idItemPortafolio, Pageable pageable) {
        return comentarioRepository
                .findByItemPortafolioIdItemPortafolioAndEstadoModeracion(idItemPortafolio, ESTADO_ACTIVO, pageable)
                .map(this::mapToResponse);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * @param idItemPortafolio identificador de la obra
     * @return la cantidad de comentarios activos (públicos) de esa obra
     */
    public long contarComentarios(Long idItemPortafolio) {
        return comentarioRepository.countByItemPortafolioIdItemPortafolioAndEstadoModeracion(
                idItemPortafolio, ESTADO_ACTIVO);
    }

    /**
     * REQ-F-010: el autor o el dueño del portafolio hacen un borrado lógico
     * (estado "Eliminado"): desaparece de la vista pública pero el admin sigue
     * pudiendo consultarlo vía {@link #listarParaModeracion}. Solo el purgado
     * explícito de ADMIN (vía {@code AdminCommentController}) borra la fila
     * de verdad; es una herramienta de moderación aparte, no lo que pide este
     * requisito.
     */
    @Override
    @Transactional
    @Auditable(accion = "COMENTARIO_ELIMINAR", modulo = AuditModule.COMUNICACION,
            entidad = "comentarios_portafolio", idEntidad = "#idComentario")
    /**
     * Elimina un comentario: un admin lo purga físicamente; el autor o el
     * dueño del portafolio solo lo marcan como {@code Eliminado} (borrado lógico).
     *
     * @param idComentario identificador del comentario
     * @param idUsuarioSolicitante identificador de quien elimina
     * @param esAdmin {@code true} si quien elimina es un admin (purga la fila)
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el comentario no existe
     * @throws org.springframework.security.access.AccessDeniedException si quien elimina no es
     *         admin, ni el autor, ni el dueño del portafolio
     */
    public void eliminarComentario(Long idComentario, Long idUsuarioSolicitante, boolean esAdmin) {
        PortfolioComment comentario = obtenerComentario(idComentario);

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
     * @param pageable paginación y ordenamiento solicitados
     * @return todos los comentarios (en cualquier estado de moderación), para el panel de moderación
     */
    public Page<CommentResponse> listarParaModeracion(Pageable pageable) {
        return comentarioRepository.findAll(pageable).map(this::mapToResponse);
    }

    @Override
    @Transactional
    @Auditable(accion = "COMENTARIO_OCULTAR", modulo = AuditModule.COMUNICACION,
            entidad = "comentarios_portafolio", idEntidad = "#idComentario")
    /**
     * Oculta un comentario por moderación.
     *
     * @param idComentario identificador del comentario
     * @return el comentario ya marcado como {@code Oculto}
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el comentario no existe
     */
    public CommentResponse ocultarComentario(Long idComentario) {
        PortfolioComment comentario = obtenerComentarioParaModerar(idComentario);
        comentario.setEstadoModeracion(ESTADO_OCULTO);
        log.info("Comentario {} ocultado por moderación", idComentario);
        return mapToResponse(comentario);
    }

    @Override
    @Transactional
    @Auditable(accion = "COMENTARIO_REACTIVAR", modulo = AuditModule.COMUNICACION,
            entidad = "comentarios_portafolio", idEntidad = "#idComentario")
    /**
     * Reactiva un comentario previamente ocultado por moderación.
     *
     * @param idComentario identificador del comentario
     * @return el comentario ya marcado como {@code Activo}
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el comentario no existe
     */
    public CommentResponse reactivarComentario(Long idComentario) {
        PortfolioComment comentario = obtenerComentarioParaModerar(idComentario);
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
    private PortfolioComment obtenerComentarioParaModerar(Long idComentario) {
        return comentarioRepository.findByIdParaModerar(idComentario)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Comentario no encontrado: " + idComentario));
    }

    private PortfolioComment obtenerComentario(Long idComentario) {
        return comentarioRepository.findById(idComentario)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Comentario no encontrado: " + idComentario));
    }

    private CommentResponse mapToResponse(PortfolioComment comentario) {
        User autor = comentario.getUsuarioAutor();
        String nombreAutor = autor == null ? null
                : autor.getNombres() + " " + autor.getApellidos();

        return CommentResponse.builder()
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
