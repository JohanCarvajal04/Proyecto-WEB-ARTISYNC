package uteq.edu.ec.artisync.service.comunicacion.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaCreadorSeguidoNovedad;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaEstadoSeguimiento;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaSeguidor;
import uteq.edu.ec.artisync.entity.comunicacion.Seguidor;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.comunicacion.SeguidorRepository;
import uteq.edu.ec.artisync.repository.perfil.PerfilCreadorRepository;
import uteq.edu.ec.artisync.service.comunicacion.ISeguidorServicio;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeguidorServicioImpl implements ISeguidorServicio {

    private final SeguidorRepository seguidorRepository;
    private final PerfilCreadorRepository perfilCreadorRepository;

    @Override
    @Transactional
    public RespuestaEstadoSeguimiento seguirCreador(Long idUsuarioSeguidor, Long idPerfilCreador) {
        PerfilCreador perfil = perfilCreadorRepository.findById(idPerfilCreador)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Perfil de creador no encontrado con ID: " + idPerfilCreador));

        if (Objects.equals(perfil.getUsuario().getIdUsuario(), idUsuarioSeguidor)) {
            throw new ExcepcionReglaNegocio("Un creador no puede seguirse a sí mismo.");
        }

        // Ejecutar función SQL fn_seguir_creador en PostgreSQL
        seguidorRepository.ejecutarFnSeguirCreador(idUsuarioSeguidor, idPerfilCreador);

        Long total = seguidorRepository.ejecutarFnConteoSeguidores(idPerfilCreador);

        return RespuestaEstadoSeguimiento.builder()
                .esSeguidor(true)
                .totalSeguidores(total)
                .esPropioPerfil(false)
                .build();
    }

    @Override
    @Transactional
    public RespuestaEstadoSeguimiento dejarDeSeguirCreador(Long idUsuarioSeguidor, Long idPerfilCreador) {
        perfilCreadorRepository.findById(idPerfilCreador)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Perfil de creador no encontrado con ID: " + idPerfilCreador));

        // Ejecutar función SQL fn_dejar_de_seguir_creador en PostgreSQL
        seguidorRepository.ejecutarFnDejarDeSeguirCreador(idUsuarioSeguidor, idPerfilCreador);

        Long total = seguidorRepository.ejecutarFnConteoSeguidores(idPerfilCreador);

        return RespuestaEstadoSeguimiento.builder()
                .esSeguidor(false)
                .totalSeguidores(total)
                .esPropioPerfil(false)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaEstadoSeguimiento obtenerEstadoSeguimiento(Long idUsuarioConsulta, Long idPerfilCreador) {
        PerfilCreador perfil = perfilCreadorRepository.findById(idPerfilCreador)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Perfil de creador no encontrado con ID: " + idPerfilCreador));

        boolean esPropioPerfil = idUsuarioConsulta != null && Objects.equals(perfil.getUsuario().getIdUsuario(), idUsuarioConsulta);
        boolean esSeguidor = false;

        if (idUsuarioConsulta != null && !esPropioPerfil) {
            Boolean res = seguidorRepository.ejecutarFnEsSeguidor(idUsuarioConsulta, idPerfilCreador);
            esSeguidor = Boolean.TRUE.equals(res);
        }

        Long total = seguidorRepository.ejecutarFnConteoSeguidores(idPerfilCreador);

        return RespuestaEstadoSeguimiento.builder()
                .esSeguidor(esSeguidor)
                .totalSeguidores(total != null ? total : 0L)
                .esPropioPerfil(esPropioPerfil)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaSeguidor> listarSeguidores(Long idPerfilCreador) {
        List<Seguidor> lista = seguidorRepository.findByPerfilCreadorIdPerfil(idPerfilCreador);
        return lista.stream()
                .map(s -> RespuestaSeguidor.builder()
                        .idUsuarioSeguidor(s.getUsuarioSeguidor().getIdUsuario())
                        .nombreSeguidor(s.getUsuarioSeguidor().getNombresUsuario() + " " + s.getUsuarioSeguidor().getApellidosUsuario())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaCreadorSeguidoNovedad> listarCreadoresSeguidosNovedades(Long idUsuarioSeguidor) {
        List<Seguidor> seguidos = seguidorRepository.findByUsuarioSeguidorIdUsuario(idUsuarioSeguidor);
        return seguidos.stream()
                .map(s -> {
                    PerfilCreador p = s.getPerfilCreador();
                    String nombre = (p.getUsuario().getNombresUsuario() + " " + p.getUsuario().getApellidosUsuario()).trim();
                    String handle = "@" + p.getUsuario().getNombresUsuario().toLowerCase().replace(" ", "");
                    return RespuestaCreadorSeguidoNovedad.builder()
                            .idPerfil(p.getIdPerfil())
                            .idUsuario(p.getUsuario().getIdUsuario())
                            .nombreCreador(nombre)
                            .handle(handle)
                            .urlFotoPerfil(p.getUsuario().getUrlFotoPerfil())
                            .tituloProfesional(p.getTituloProfesional())
                            .resumenNovedad("Actividad reciente en su catálogo y publicaciones")
                            .tipoNovedad("GENERAL")
                            .fechaNovedad(s.getFechaSeguimiento())
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public boolean actualizarPortadaYTitulo(Long idUsuario, String urlPortada, String tituloProfesional) {
        PerfilCreador perfil = perfilCreadorRepository.findByUsuarioIdUsuario(idUsuario)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("No tienes un perfil de creador asociado."));

        if (urlPortada != null) {
            perfil.setUrlPortada(urlPortada);
        }
        if (tituloProfesional != null) {
            perfil.setTituloProfesional(tituloProfesional);
        }
        perfilCreadorRepository.save(perfil);
        return true;
    }
}
