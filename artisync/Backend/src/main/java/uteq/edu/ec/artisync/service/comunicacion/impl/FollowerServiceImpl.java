package uteq.edu.ec.artisync.service.comunicacion.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.FollowedCreatorUpdateResponse;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.FollowStatusResponse;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.FollowerResponse;
import uteq.edu.ec.artisync.entity.comunicacion.Follower;
import uteq.edu.ec.artisync.entity.perfil.CreatorProfile;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.comunicacion.FollowerRepository;
import uteq.edu.ec.artisync.repository.perfil.CreatorProfileRepository;
import uteq.edu.ec.artisync.service.comunicacion.IFollowerService;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FollowerServiceImpl implements IFollowerService {

    private final FollowerRepository seguidorRepository;
    private final CreatorProfileRepository perfilCreadorRepository;

    @Override
    @Transactional
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idUsuarioSeguidor identificador unico que referencia de manera univoca al registro
     * @param idPerfilCreador identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public FollowStatusResponse seguirCreador(Long idUsuarioSeguidor, Long idPerfilCreador) {
        CreatorProfile perfil = perfilCreadorRepository.findById(idPerfilCreador)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de creador no encontrado con ID: " + idPerfilCreador));

        if (Objects.equals(perfil.getUsuario().getIdUsuario(), idUsuarioSeguidor)) {
            throw new BusinessRuleException("Un creador no puede seguirse a sí mismo.");
        }

        // Ejecutar función SQL fn_seguir_creador en PostgreSQL
        seguidorRepository.ejecutarFnSeguirCreador(idUsuarioSeguidor, idPerfilCreador);

        Long total = seguidorRepository.ejecutarFnConteoSeguidores(idPerfilCreador);

        return FollowStatusResponse.builder()
                .esSeguidor(true)
                .totalSeguidores(total)
                .esPropioPerfil(false)
                .build();
    }

    @Override
    @Transactional
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idUsuarioSeguidor identificador unico que referencia de manera univoca al registro
     * @param idPerfilCreador identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public FollowStatusResponse dejarDeSeguirCreador(Long idUsuarioSeguidor, Long idPerfilCreador) {
        perfilCreadorRepository.findById(idPerfilCreador)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de creador no encontrado con ID: " + idPerfilCreador));

        // Ejecutar función SQL fn_dejar_de_seguir_creador en PostgreSQL
        seguidorRepository.ejecutarFnDejarDeSeguirCreador(idUsuarioSeguidor, idPerfilCreador);

        Long total = seguidorRepository.ejecutarFnConteoSeguidores(idPerfilCreador);

        return FollowStatusResponse.builder()
                .esSeguidor(false)
                .totalSeguidores(total)
                .esPropioPerfil(false)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param idUsuarioConsulta identificador unico que referencia de manera univoca al registro
     * @param idPerfilCreador identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public FollowStatusResponse obtenerEstadoSeguimiento(Long idUsuarioConsulta, Long idPerfilCreador) {
        CreatorProfile perfil = perfilCreadorRepository.findById(idPerfilCreador)
                .orElseThrow(() -> new ResourceNotFoundException("Perfil de creador no encontrado con ID: " + idPerfilCreador));

        boolean esPropioPerfil = idUsuarioConsulta != null && Objects.equals(perfil.getUsuario().getIdUsuario(), idUsuarioConsulta);
        boolean esSeguidor = false;

        if (idUsuarioConsulta != null && !esPropioPerfil) {
            Boolean res = seguidorRepository.ejecutarFnEsSeguidor(idUsuarioConsulta, idPerfilCreador);
            esSeguidor = Boolean.TRUE.equals(res);
        }

        Long total = seguidorRepository.ejecutarFnConteoSeguidores(idPerfilCreador);

        return FollowStatusResponse.builder()
                .esSeguidor(esSeguidor)
                .totalSeguidores(total != null ? total : 0L)
                .esPropioPerfil(esPropioPerfil)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @param idPerfilCreador identificador unico que referencia de manera univoca al registro
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<FollowerResponse> listarSeguidores(Long idPerfilCreador) {
        List<Follower> lista = seguidorRepository.findByPerfilCreadorIdPerfil(idPerfilCreador);
        return lista.stream()
                .map(s -> FollowerResponse.builder()
                        .idSeguimiento(s.getIdSeguimiento())
                        .idUsuarioSeguidor(s.getUsuarioSeguidor().getIdUsuario())
                        .nombreSeguidor((s.getUsuarioSeguidor().getNombres() + " " + s.getUsuarioSeguidor().getApellidos()).trim())
                        .idPerfilCreador(s.getPerfilCreador().getIdPerfil())
                        .notificacionesActivas(s.getNotificacionesActivas())
                        .fechaSeguimiento(s.getFechaSeguimiento())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @param idUsuarioSeguidor identificador unico que referencia de manera univoca al registro
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<FollowedCreatorUpdateResponse> listarCreadoresSeguidosNovedades(Long idUsuarioSeguidor) {
        List<Follower> seguidos = seguidorRepository.findByUsuarioSeguidorIdUsuario(idUsuarioSeguidor);
        return seguidos.stream()
                .map(s -> {
                    CreatorProfile p = s.getPerfilCreador();
                    String nombre = (p.getUsuario().getNombres() + " " + p.getUsuario().getApellidos()).trim();
                    String handle = "@" + (p.getUsuario().getNombres() != null ? p.getUsuario().getNombres().toLowerCase().replace(" ", "") : "creador");
                    return FollowedCreatorUpdateResponse.builder()
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
    /**
     * Aplica modificaciones y validaciones de negocio sobre los datos de un registro existente.
     *
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @param urlPortada parametro requerido para la correcta ejecucion del procedimiento
     * @param tituloProfesional parametro requerido para la correcta ejecucion del procedimiento
     * @return valor logico verdadero si la comprobacion fue exitosa, o falso si no cumplio los requisitos
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public boolean actualizarPortadaYTitulo(Long idUsuario, String urlPortada, String tituloProfesional) {
        CreatorProfile perfil = perfilCreadorRepository.findByUsuarioIdUsuario(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("No tienes un perfil de creador asociado."));

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
