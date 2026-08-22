package uteq.edu.ec.artisync.service.comunicacion.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaSeguidor;
import uteq.edu.ec.artisync.entity.comunicacion.Seguidor;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoDuplicado;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.comunicacion.SeguidorRepository;
import uteq.edu.ec.artisync.repository.perfil.PerfilCreadorRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.service.comunicacion.SeguidorService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de seguimiento de creadores.
 * REQ-F-009: seguir y dejar de seguir con efecto inmediato sobre el contador del perfil público.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SeguidorServiceImpl implements SeguidorService {

    private final SeguidorRepository seguidorRepository;
    private final PerfilCreadorRepository perfilCreadorRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional
    @Auditable(accion = "SEGUIDOR_SEGUIR", modulo = ModuloAuditoria.SOCIAL,
            entidad = "seguidores", idEntidad = "#idPerfilCreador")
    public RespuestaSeguidor seguir(Long idPerfilCreador, Long idUsuarioSeguidor) {
        PerfilCreador perfil = obtenerPerfil(idPerfilCreador);

        // Un creador no puede seguirse a sí mismo.
        if (perfil.getUsuario() != null
                && idUsuarioSeguidor.equals(perfil.getUsuario().getIdUsuario())) {
            throw new ExcepcionReglaNegocio("No puedes seguir tu propio perfil de creador");
        }

        // La restricción UNIQUE de la tabla ya lo impide; se comprueba antes para
        // devolver un 409 con mensaje de dominio en vez de un error de integridad.
        if (seguidorRepository.existsByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(
                idUsuarioSeguidor, idPerfilCreador)) {
            throw new ExcepcionRecursoDuplicado("Ya sigues a este creador");
        }

        Usuario seguidor = usuarioRepository.findById(idUsuarioSeguidor)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "Usuario no encontrado: " + idUsuarioSeguidor));

        Seguidor seguimiento = seguidorRepository.save(Seguidor.builder()
                .usuarioSeguidor(seguidor)
                .perfilCreador(perfil)
                .build());

        log.info("Usuario {} sigue al creador {}", idUsuarioSeguidor, idPerfilCreador);
        return mapToResponse(seguimiento);
    }

    @Override
    @Transactional
    @Auditable(accion = "SEGUIDOR_DEJAR", modulo = ModuloAuditoria.SOCIAL,
            entidad = "seguidores", idEntidad = "#idPerfilCreador")
    public void dejarDeSeguir(Long idPerfilCreador, Long idUsuarioSeguidor) {
        Seguidor seguimiento = seguidorRepository
                .findByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(
                        idUsuarioSeguidor, idPerfilCreador)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "No sigues a este creador"));

        seguidorRepository.delete(seguimiento);
        log.info("Usuario {} deja de seguir al creador {}", idUsuarioSeguidor, idPerfilCreador);
    }

    @Override
    @Transactional(readOnly = true)
    public long contarSeguidores(Long idPerfilCreador) {
        return seguidorRepository.countByPerfilCreadorIdPerfil(idPerfilCreador);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean sigue(Long idPerfilCreador, Long idUsuarioSeguidor) {
        return seguidorRepository.existsByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(
                idUsuarioSeguidor, idPerfilCreador);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaSeguidor> listarSeguidores(Long idPerfilCreador) {
        return seguidorRepository.findByPerfilCreadorIdPerfil(idPerfilCreador)
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // -------------------------------------------------------------------------
    private PerfilCreador obtenerPerfil(Long idPerfilCreador) {
        return perfilCreadorRepository.findById(idPerfilCreador)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "Perfil de creador no encontrado: " + idPerfilCreador));
    }

    private RespuestaSeguidor mapToResponse(Seguidor seguimiento) {
        Usuario seguidor = seguimiento.getUsuarioSeguidor();
        String nombre = seguidor == null ? null
                : seguidor.getNombres() + " " + seguidor.getApellidos();

        return RespuestaSeguidor.builder()
                .idSeguimiento(seguimiento.getIdSeguimiento())
                .idUsuarioSeguidor(seguidor == null ? null : seguidor.getIdUsuario())
                .nombreSeguidor(nombre)
                .idPerfilCreador(seguimiento.getPerfilCreador() == null ? null
                        : seguimiento.getPerfilCreador().getIdPerfil())
                .notificacionesActivas(seguimiento.getNotificacionesActivas())
                .fechaSeguimiento(seguimiento.getFechaSeguimiento())
                .build();
    }
}
