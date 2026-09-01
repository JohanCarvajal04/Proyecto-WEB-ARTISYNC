package uteq.edu.ec.artisync.service.comunicacion.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaEstadoLike;
import uteq.edu.ec.artisync.entity.comunicacion.LikePortafolio;
import uteq.edu.ec.artisync.entity.perfil.PortafolioItem;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoDuplicado;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.repository.comunicacion.LikePortafolioRepository;
import uteq.edu.ec.artisync.repository.perfil.PortafolioItemRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.service.comunicacion.LikePortafolioService;

/**
 * Implementación del servicio de "me gusta" sobre ítems de portafolio.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LikePortafolioServiceImpl implements LikePortafolioService {

    private final LikePortafolioRepository likeRepository;
    private final PortafolioItemRepository portafolioItemRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    @Auditable(accion = "LIKE_DAR", modulo = ModuloAuditoria.COMUNICACION,
            entidad = "likes_portafolio", idEntidad = "#idItemPortafolio")
    public RespuestaEstadoLike darLike(Long idItemPortafolio, Long idUsuario) {
        PortafolioItem item = obtenerItem(idItemPortafolio);

        // La restricción UNIQUE de la tabla ya lo impide; se comprueba antes
        // para devolver un 409 con mensaje de dominio en vez de un error de
        // integridad de base de datos.
        if (likeRepository.existsByItemPortafolioIdItemPortafolioAndUsuarioIdUsuario(idItemPortafolio, idUsuario)) {
            throw new ExcepcionRecursoDuplicado("Ya le diste like a esta obra");
        }

        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Usuario no encontrado: " + idUsuario));

        likeRepository.save(LikePortafolio.builder()
                .itemPortafolio(item)
                .usuario(usuario)
                .build());

        log.info("Usuario {} dio like al ítem de portafolio {}", idUsuario, idItemPortafolio);
        return construirEstado(idItemPortafolio, true);
    }

    @Override
    @Transactional
    @Auditable(accion = "LIKE_QUITAR", modulo = ModuloAuditoria.COMUNICACION,
            entidad = "likes_portafolio", idEntidad = "#idItemPortafolio")
    public RespuestaEstadoLike quitarLike(Long idItemPortafolio, Long idUsuario) {
        LikePortafolio like = likeRepository
                .findByItemPortafolioIdItemPortafolioAndUsuarioIdUsuario(idItemPortafolio, idUsuario)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("No le has dado like a esta obra"));

        likeRepository.delete(like);
        log.info("Usuario {} quitó el like del ítem de portafolio {}", idUsuario, idItemPortafolio);
        return construirEstado(idItemPortafolio, false);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaEstadoLike obtenerEstado(Long idItemPortafolio, Long idUsuario) {
        boolean meGusta = idUsuario != null
                && likeRepository.existsByItemPortafolioIdItemPortafolioAndUsuarioIdUsuario(idItemPortafolio, idUsuario);
        return construirEstado(idItemPortafolio, meGusta);
    }

    // -------------------------------------------------------------------------
    private PortafolioItem obtenerItem(Long idItemPortafolio) {
        return portafolioItemRepository.findById(idItemPortafolio)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "Ítem de portafolio no encontrado: " + idItemPortafolio));
    }

    private RespuestaEstadoLike construirEstado(Long idItemPortafolio, boolean meGusta) {
        long total = likeRepository.countByItemPortafolioIdItemPortafolio(idItemPortafolio);
        return RespuestaEstadoLike.builder()
                .idItemPortafolio(idItemPortafolio)
                .totalLikes(total)
                .meGusta(meGusta)
                .build();
    }
}
