package uteq.edu.ec.artisync.service.comunicacion.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaEstadoLike;
import uteq.edu.ec.artisync.entity.comunicacion.LikePortafolio;
import uteq.edu.ec.artisync.entity.perfil.PortfolioItem;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.DuplicateResourceException;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.repository.comunicacion.LikePortafolioRepository;
import uteq.edu.ec.artisync.repository.perfil.PortfolioItemRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;
import uteq.edu.ec.artisync.service.comunicacion.LikePortafolioService;

/**
 * Implementación del servicio de "me gusta" sobre ítems de portafolio.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LikePortafolioServiceImpl implements LikePortafolioService {

    private final LikePortafolioRepository likeRepository;
    private final PortfolioItemRepository portafolioItemRepository;
    private final UserRepository usuarioRepository;

    @Override
    @Transactional
    @Auditable(accion = "LIKE_DAR", modulo = AuditModule.COMUNICACION,
            entidad = "likes_portafolio", idEntidad = "#idItemPortafolio")
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idItemPortafolio identificador unico que referencia de manera univoca al registro
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaEstadoLike darLike(Long idItemPortafolio, Long idUsuario) {
        PortfolioItem item = obtenerItem(idItemPortafolio);

        // La restricción UNIQUE de la tabla ya lo impide; se comprueba antes
        // para devolver un 409 con mensaje de dominio en vez de un error de
        // integridad de base de datos.
        if (likeRepository.existsByItemPortafolioIdItemPortafolioAndUsuarioIdUsuario(idItemPortafolio, idUsuario)) {
            throw new DuplicateResourceException("Ya le diste like a esta obra");
        }

        User usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("User no encontrado: " + idUsuario));

        likeRepository.save(LikePortafolio.builder()
                .itemPortafolio(item)
                .usuario(usuario)
                .build());

        log.info("User {} dio like al ítem de portafolio {}", idUsuario, idItemPortafolio);
        return construirEstado(idItemPortafolio, true);
    }

    @Override
    @Transactional
    @Auditable(accion = "LIKE_QUITAR", modulo = AuditModule.COMUNICACION,
            entidad = "likes_portafolio", idEntidad = "#idItemPortafolio")
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idItemPortafolio identificador unico que referencia de manera univoca al registro
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaEstadoLike quitarLike(Long idItemPortafolio, Long idUsuario) {
        LikePortafolio like = likeRepository
                .findByItemPortafolioIdItemPortafolioAndUsuarioIdUsuario(idItemPortafolio, idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("No le has dado like a esta obra"));

        likeRepository.delete(like);
        log.info("User {} quitó el like del ítem de portafolio {}", idUsuario, idItemPortafolio);
        return construirEstado(idItemPortafolio, false);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param idItemPortafolio identificador unico que referencia de manera univoca al registro
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaEstadoLike obtenerEstado(Long idItemPortafolio, Long idUsuario) {
        boolean meGusta = idUsuario != null
                && likeRepository.existsByItemPortafolioIdItemPortafolioAndUsuarioIdUsuario(idItemPortafolio, idUsuario);
        return construirEstado(idItemPortafolio, meGusta);
    }

    // -------------------------------------------------------------------------
    private PortfolioItem obtenerItem(Long idItemPortafolio) {
        return portafolioItemRepository.findById(idItemPortafolio)
                .orElseThrow(() -> new ResourceNotFoundException(
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
