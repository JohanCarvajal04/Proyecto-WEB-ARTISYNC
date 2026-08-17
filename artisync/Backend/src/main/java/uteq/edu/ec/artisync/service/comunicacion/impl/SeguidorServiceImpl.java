package uteq.edu.ec.artisync.service.comunicacion.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaSeguidor;
import uteq.edu.ec.artisync.entity.comunicacion.Seguidor;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.repository.comunicacion.SeguidorRepository;
import uteq.edu.ec.artisync.repository.perfil.PerfilCreadorRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.service.comunicacion.SeguidorService;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeguidorServiceImpl implements SeguidorService {

    private final SeguidorRepository seguidorRepository;
    private final UsuarioRepository usuarioRepository;
    private final PerfilCreadorRepository perfilCreadorRepository;

    @Override
    @Transactional
    public RespuestaSeguidor seguirCreador(Long idUsuario, Long idPerfilCreador) {
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));
        PerfilCreador perfil = perfilCreadorRepository.findById(idPerfilCreador)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de creador no encontrado"));

        if (perfil.getUsuario().getIdUsuario().equals(idUsuario)) {
            throw new IllegalArgumentException("No puedes seguirte a ti mismo");
        }

        Optional<Seguidor> existente = seguidorRepository.findByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(idUsuario, idPerfilCreador);
        if (existente.isPresent()) {
            throw new IllegalArgumentException("Ya sigues a este creador");
        }

        Seguidor seguidor = Seguidor.builder()
                .usuarioSeguidor(usuario)
                .perfilCreador(perfil)
                .notificacionesActivas(true)
                .build();

        seguidor = seguidorRepository.save(seguidor);
        log.info("Usuario {} empezó a seguir al creador {}", idUsuario, idPerfilCreador);

        return mapearRespuesta(seguidor);
    }

    @Override
    @Transactional
    public RespuestaMensaje dejarDeSeguir(Long idUsuario, Long idPerfilCreador) {
        Seguidor seguidor = seguidorRepository.findByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(idUsuario, idPerfilCreador)
                .orElseThrow(() -> new ResourceNotFoundException("No sigues a este creador"));

        seguidorRepository.delete(seguidor);
        log.info("Usuario {} dejó de seguir al creador {}", idUsuario, idPerfilCreador);

        return new RespuestaMensaje("Has dejado de seguir al creador exitosamente");
    }

    @Override
    @Transactional(readOnly = true)
    public long contarSeguidores(Long idPerfilCreador) {
        return seguidorRepository.countByPerfilCreadorIdPerfil(idPerfilCreador);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean estaSiguiendo(Long idUsuario, Long idPerfilCreador) {
        if (idUsuario == null) return false;
        return seguidorRepository.existsByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(idUsuario, idPerfilCreador);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaSeguidor> listarSeguidores(Long idPerfilCreador) {
        List<Seguidor> seguidores = seguidorRepository.findByPerfilCreadorIdPerfil(idPerfilCreador);
        return seguidores.stream().map(this::mapearRespuesta).collect(Collectors.toList());
    }

    private RespuestaSeguidor mapearRespuesta(Seguidor seguidor) {
        return RespuestaSeguidor.builder()
                .idSeguimiento(seguidor.getIdSeguimiento())
                .idUsuarioSeguidor(seguidor.getUsuarioSeguidor().getIdUsuario())
                .nombreSeguidor(seguidor.getUsuarioSeguidor().getNombreUsuario())
                .idPerfilCreador(seguidor.getPerfilCreador().getIdPerfil())
                .notificacionesActivas(seguidor.getNotificacionesActivas())
                .fechaSeguimiento(seguidor.getFechaSeguimiento())
                .build();
    }
}
