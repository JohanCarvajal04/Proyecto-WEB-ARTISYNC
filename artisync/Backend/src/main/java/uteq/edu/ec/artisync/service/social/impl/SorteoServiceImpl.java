package uteq.edu.ec.artisync.service.social.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
import uteq.edu.ec.artisync.dto.peticion.social.PeticionActualizarSorteo;
import uteq.edu.ec.artisync.dto.peticion.social.PeticionCrearSorteo;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.social.RespuestaGanador;
import uteq.edu.ec.artisync.dto.respuesta.social.RespuestaParticipante;
import uteq.edu.ec.artisync.dto.respuesta.social.RespuestaPremio;
import uteq.edu.ec.artisync.dto.respuesta.social.RespuestaSorteo;
import uteq.edu.ec.artisync.entity.social.ParticipanteSorteo;
import uteq.edu.ec.artisync.entity.social.PremioSorteo;
import uteq.edu.ec.artisync.entity.social.Sorteo;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoDuplicado;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.comunicacion.SeguidorRepository;
import uteq.edu.ec.artisync.repository.perfil.PerfilCreadorRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.repository.social.ParticipanteSorteoRepository;
import uteq.edu.ec.artisync.repository.social.SorteoRepository;
import uteq.edu.ec.artisync.service.social.SorteoService;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementación del servicio de sorteos.
 * RF-23: CRUD, participación con validaciones y selección de ganadores.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SorteoServiceImpl implements SorteoService {

    private final SorteoRepository sorteoRepository;
    private final ParticipanteSorteoRepository participanteSorteoRepository;
    private final PerfilCreadorRepository perfilCreadorRepository;
    private final UsuarioRepository usuarioRepository;
    private final SeguidorRepository seguidorRepository;

    // =========================================================================
    // CRUD de Sorteos (CREADOR)
    // =========================================================================

    @Override
    @Transactional
    @Auditable(accion = "SORTEO_CREAR", modulo = ModuloAuditoria.SOCIAL,
            entidad = "sorteos", idEntidad = "#resultado.idSorteo",
            detalle = "{tituloSorteo: #peticion.tituloSorteo, cantidadGanadores: #peticion.cantidadGanadores}")
    public RespuestaSorteo crearSorteo(Long idUsuario, PeticionCrearSorteo peticion) {
        var perfil = perfilCreadorRepository.findByUsuarioIdUsuario(idUsuario)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "No tienes un perfil de creador activo"));

        if (peticion.getFechaCierre().isBefore(peticion.getFechaInicio())) {
            throw new ExcepcionReglaNegocio(
                    "La fecha de cierre debe ser posterior a la fecha de inicio");
        }

        validarCantidadPremios(peticion.getPremios().size(), peticion.getCantidadGanadores());

        Sorteo sorteo = Sorteo.builder()
                .perfilCreador(perfil)
                .tituloSorteo(peticion.getTituloSorteo())
                .cantidadGanadores(peticion.getCantidadGanadores())
                .fechaInicio(peticion.getFechaInicio())
                .fechaCierre(peticion.getFechaCierre())
                .requiereSeguidor(peticion.isRequiereSeguidor())
                .estadoSorteo("Activo")
                .build();
        sorteo.setPremios(construirPremios(sorteo, peticion.getPremios()));

        sorteo = sorteoRepository.save(sorteo);
        log.info("Sorteo '{}' creado por usuario {}", sorteo.getTituloSorteo(), idUsuario);
        return mapToResponse(sorteo, null, 0L, false);
    }

    /** REQ-F-023: la cantidad de ganadores debe coincidir exactamente con la cantidad de premios definidos. */
    private void validarCantidadPremios(int cantidadPremios, int cantidadGanadores) {
        if (cantidadPremios != cantidadGanadores) {
            throw new ExcepcionReglaNegocio(
                    "La cantidad de ganadores (" + cantidadGanadores
                            + ") debe coincidir con la cantidad de premios definidos (" + cantidadPremios + ")");
        }
    }

    private List<PremioSorteo> construirPremios(Sorteo sorteo, List<String> descripciones) {
        List<PremioSorteo> premios = new ArrayList<>();
        for (int i = 0; i < descripciones.size(); i++) {
            premios.add(PremioSorteo.builder()
                    .sorteo(sorteo)
                    .descripcionPremio(descripciones.get(i))
                    .orden(i + 1)
                    .build());
        }
        return premios;
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaSorteo obtenerSorteo(Long idSorteo, Long idUsuarioActual) {
        Sorteo sorteo = findSorteoOrThrow(idSorteo);
        long total = participanteSorteoRepository.findBySorteoIdSorteo(idSorteo).size();
        boolean yoParticipo = idUsuarioActual != null &&
                participanteSorteoRepository.existsBySorteoIdSorteoAndUsuarioIdUsuario(idSorteo, idUsuarioActual);

        List<RespuestaGanador> ganadores = obtenerGanadoresSiFinalizado(sorteo);
        return mapToResponse(sorteo, ganadores, total, yoParticipo);
    }

    /**
     * Solo hay ganadores despues del cierre del sorteo. Se usa tanto en el detalle
     * como en los listados, para que las tarjetas de la lista muestren el premio
     * de cada quien sin depender de abrir el detalle del sorteo.
     */
    private List<RespuestaGanador> obtenerGanadoresSiFinalizado(Sorteo sorteo) {
        if (!"Finalizado".equals(sorteo.getEstadoSorteo())) {
            return null;
        }
        return participanteSorteoRepository
                .findBySorteoIdSorteoAndEsGanadorTrue(sorteo.getIdSorteo())
                .stream().map(this::mapToGanadorResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Auditable(accion = "SORTEO_ACTUALIZAR", modulo = ModuloAuditoria.SOCIAL,
            entidad = "sorteos", idEntidad = "#idSorteo")
    public RespuestaSorteo actualizarSorteo(Long idSorteo, Long idUsuario, PeticionActualizarSorteo peticion) {
        Sorteo sorteo = verificarPropietario(idSorteo, idUsuario);
        boolean tieneParticipantes = participanteSorteoRepository.existsBySorteoIdSorteo(idSorteo);

        if (tieneParticipantes) {
            if (peticion.getCantidadGanadores() != null &&
                    !peticion.getCantidadGanadores().equals(sorteo.getCantidadGanadores())) {
                throw new ExcepcionReglaNegocio(
                        "No se puede modificar este campo una vez iniciadas las inscripciones");
            }
            if (peticion.getPremios() != null) {
                throw new ExcepcionReglaNegocio(
                        "No se puede modificar este campo una vez iniciadas las inscripciones");
            }
            if (peticion.getFechaCierre() != null &&
                    !peticion.getFechaCierre().equals(sorteo.getFechaCierre())) {
                throw new ExcepcionReglaNegocio(
                        "No se puede modificar la fecha de cierre una vez iniciadas las inscripciones");
            }
        }

        // fechaInicio no es editable por este DTO, así que solo se valida contra
        // ella (nunca se recalcula): sin este chequeo, una fechaCierre nueva
        // anterior a la fechaInicio original dejaba el sorteo en un estado
        // imposible — participar() rechaza tanto "aún no ha comenzado" como
        // "el periodo de inscripción ha finalizado" para cualquier instante.
        if (peticion.getFechaCierre() != null && peticion.getFechaCierre().isBefore(sorteo.getFechaInicio())) {
            throw new ExcepcionReglaNegocio(
                    "La fecha de cierre debe ser posterior a la fecha de inicio del sorteo");
        }

        if (peticion.getTituloSorteo() != null) sorteo.setTituloSorteo(peticion.getTituloSorteo());
        if (!tieneParticipantes && peticion.getCantidadGanadores() != null)
            sorteo.setCantidadGanadores(peticion.getCantidadGanadores());
        if (!tieneParticipantes && peticion.getFechaCierre() != null)
            sorteo.setFechaCierre(peticion.getFechaCierre());
        if (!tieneParticipantes && peticion.getPremios() != null) {
            validarCantidadPremios(peticion.getPremios().size(), sorteo.getCantidadGanadores());
            sorteo.getPremios().clear();
            sorteo.getPremios().addAll(construirPremios(sorteo, peticion.getPremios()));
        }

        sorteo = sorteoRepository.save(sorteo);
        long total = participanteSorteoRepository.findBySorteoIdSorteo(idSorteo).size();
        return mapToResponse(sorteo, null, total, false);
    }

    @Override
    @Transactional
    @Auditable(accion = "SORTEO_ELIMINAR", modulo = ModuloAuditoria.SOCIAL,
            entidad = "sorteos", idEntidad = "#idSorteo")
    public RespuestaMensaje eliminarSorteo(Long idSorteo, Long idUsuario) {
        Sorteo sorteo = verificarPropietario(idSorteo, idUsuario);
        if (participanteSorteoRepository.existsBySorteoIdSorteo(idSorteo)) {
            throw new ExcepcionReglaNegocio(
                    "No se puede eliminar un sorteo con participantes inscritos");
        }
        sorteoRepository.delete(sorteo);
        log.info("Sorteo {} eliminado por usuario {}", idSorteo, idUsuario);
        return new RespuestaMensaje("Sorteo eliminado correctamente");
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaSorteo> listarSorteosPorCreador(Long idPerfilCreador, Long idUsuarioActual) {
        return sorteoRepository.findByPerfilCreadorIdPerfil(idPerfilCreador)
                .stream()
                .map(s -> {
                    long total = participanteSorteoRepository.findBySorteoIdSorteo(s.getIdSorteo()).size();
                    boolean yoParticipo = idUsuarioActual != null &&
                            participanteSorteoRepository.existsBySorteoIdSorteoAndUsuarioIdUsuario(
                                    s.getIdSorteo(), idUsuarioActual);
                    return mapToResponse(s, obtenerGanadoresSiFinalizado(s), total, yoParticipo);
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaSorteo> listarSorteosActivos(Long idUsuarioActual) {
        return sorteoRepository.findByEstadoSorteo("Activo")
                .stream()
                .map(s -> {
                    long total = participanteSorteoRepository.findBySorteoIdSorteo(s.getIdSorteo()).size();
                    boolean yoParticipo = idUsuarioActual != null &&
                            participanteSorteoRepository.existsBySorteoIdSorteoAndUsuarioIdUsuario(
                                    s.getIdSorteo(), idUsuarioActual);
                    return mapToResponse(s, obtenerGanadoresSiFinalizado(s), total, yoParticipo);
                })
                .collect(Collectors.toList());
    }

    // =========================================================================
    // Participación
    // =========================================================================

    @Override
    @Transactional
    public RespuestaParticipante participar(Long idSorteo, Long idUsuario) {
        Sorteo sorteo = findSorteoOrThrow(idSorteo);

        // Validar estado
        if (!"Activo".equals(sorteo.getEstadoSorteo())) {
            throw new ExcepcionReglaNegocio("El sorteo no está activo");
        }

        // Validar rango de fechas
        LocalDateTime ahora = LocalDateTime.now();
        if (ahora.isBefore(sorteo.getFechaInicio())) {
            throw new ExcepcionReglaNegocio("El sorteo aún no ha comenzado");
        }
        if (ahora.isAfter(sorteo.getFechaCierre())) {
            throw new ExcepcionReglaNegocio("El periodo de inscripción ha finalizado");
        }

        // Validar inscripción duplicada
        if (participanteSorteoRepository.existsBySorteoIdSorteoAndUsuarioIdUsuario(idSorteo, idUsuario)) {
            throw new ExcepcionRecursoDuplicado("Ya estás inscrito en este sorteo");
        }

        // Validar requisito de seguidor
        if (Boolean.TRUE.equals(sorteo.getRequiereSeguidor())) {
            boolean esSeguidor = seguidorRepository.existsByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(
                    idUsuario, sorteo.getPerfilCreador().getIdPerfil());
            if (!esSeguidor) {
                throw new ExcepcionReglaNegocio(
                        "Este sorteo requiere que sigas al creador para poder participar");
            }
        }

        Usuario usuario = usuarioRepository.getReferenceById(idUsuario);
        ParticipanteSorteo participante = ParticipanteSorteo.builder()
                .sorteo(sorteo)
                .usuario(usuario)
                .esGanador(false)
                .build();
        participante = participanteSorteoRepository.save(participante);
        log.info("Usuario {} inscrito en sorteo {}", idUsuario, idSorteo);
        return mapToParticipanteResponse(participante);
    }

    @Override
    @Transactional
    public RespuestaMensaje cancelarParticipacion(Long idSorteo, Long idUsuario) {
        Sorteo sorteo = findSorteoOrThrow(idSorteo);
        if (!"Activo".equals(sorteo.getEstadoSorteo())) {
            throw new ExcepcionReglaNegocio("No puedes cancelar la inscripción en un sorteo que ya ha finalizado");
        }
        ParticipanteSorteo participante = participanteSorteoRepository
                .findBySorteoIdSorteo(idSorteo)
                .stream()
                .filter(p -> p.getUsuario().getIdUsuario().equals(idUsuario))
                .findFirst()
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "No estás inscrito en este sorteo"));
        participanteSorteoRepository.delete(participante);
        return new RespuestaMensaje("Inscripción cancelada correctamente");
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaParticipante> listarParticipantes(Long idSorteo) {
        findSorteoOrThrow(idSorteo); // Valida que existe
        return participanteSorteoRepository.findBySorteoIdSorteo(idSorteo)
                .stream().map(this::mapToParticipanteResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaGanador> listarGanadores(Long idSorteo) {
        Sorteo sorteo = findSorteoOrThrow(idSorteo);
        if (!"Finalizado".equals(sorteo.getEstadoSorteo())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Los ganadores solo están disponibles después del cierre del sorteo");
        }
        return participanteSorteoRepository.findBySorteoIdSorteoAndEsGanadorTrue(idSorteo)
                .stream().map(this::mapToGanadorResponse).collect(Collectors.toList());
    }

    // =========================================================================
    // Helpers privados
    // =========================================================================

    private Sorteo findSorteoOrThrow(Long idSorteo) {
        return sorteoRepository.findById(idSorteo)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Sorteo no encontrado: " + idSorteo));
    }

    private Sorteo verificarPropietario(Long idSorteo, Long idUsuario) {
        Sorteo sorteo = findSorteoOrThrow(idSorteo);
        var perfil = perfilCreadorRepository.findByUsuarioIdUsuario(idUsuario)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("No tienes perfil de creador"));
        if (!sorteo.getPerfilCreador().getIdPerfil().equals(perfil.getIdPerfil())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No tienes permiso para modificar este sorteo");
        }
        return sorteo;
    }

    private RespuestaSorteo mapToResponse(Sorteo sorteo, List<RespuestaGanador> ganadores,
                                          long total, boolean yoParticipo) {
        return RespuestaSorteo.builder()
                .idSorteo(sorteo.getIdSorteo())
                .tituloSorteo(sorteo.getTituloSorteo())
                .cantidadGanadores(sorteo.getCantidadGanadores())
                .fechaInicio(sorteo.getFechaInicio())
                .fechaCierre(sorteo.getFechaCierre())
                .estadoSorteo(sorteo.getEstadoSorteo())
                .requiereSeguidor(sorteo.getRequiereSeguidor())
                .idPerfilCreador(sorteo.getPerfilCreador().getIdPerfil())
                .nombreCreador(sorteo.getPerfilCreador().getUsuario().getNombres()
                        + " " + sorteo.getPerfilCreador().getUsuario().getApellidos())
                .totalParticipantes(total)
                .yoParticipo(yoParticipo)
                .ganadores(ganadores)
                .premios(mapToPremiosResponse(sorteo, ganadores))
                .build();
    }

    /** Cruza cada premio del sorteo con su ganador (si ya hubo sorteo y ese premio fue asignado). */
    private List<RespuestaPremio> mapToPremiosResponse(Sorteo sorteo, List<RespuestaGanador> ganadores) {
        Map<Long, RespuestaGanador> ganadorPorPremio = new HashMap<>();
        if (ganadores != null) {
            for (RespuestaGanador g : ganadores) {
                if (g.getIdPremio() != null) {
                    ganadorPorPremio.put(g.getIdPremio(), g);
                }
            }
        }
        return sorteo.getPremios().stream()
                .map(p -> RespuestaPremio.builder()
                        .idPremio(p.getIdPremio())
                        .descripcionPremio(p.getDescripcionPremio())
                        .orden(p.getOrden())
                        .ganador(ganadorPorPremio.get(p.getIdPremio()))
                        .build())
                .collect(Collectors.toList());
    }

    private RespuestaParticipante mapToParticipanteResponse(ParticipanteSorteo p) {
        return RespuestaParticipante.builder()
                .idParticipacion(p.getIdParticipacion())
                .idUsuario(p.getUsuario().getIdUsuario())
                .nombreUsuario(p.getUsuario().getNombres() + " " + p.getUsuario().getApellidos())
                .fechaInscripcion(p.getFechaInscripcion())
                .esGanador(p.getEsGanador())
                .build();
    }

    private RespuestaGanador mapToGanadorResponse(ParticipanteSorteo p) {
        return RespuestaGanador.builder()
                .idParticipacion(p.getIdParticipacion())
                .idUsuario(p.getUsuario().getIdUsuario())
                .nombreUsuario(p.getUsuario().getNombres() + " " + p.getUsuario().getApellidos())
                .fechaNotificacionPremio(p.getFechaNotificacionPremio())
                .idPremio(p.getPremio() != null ? p.getPremio().getIdPremio() : null)
                .descripcionPremio(p.getPremio() != null ? p.getPremio().getDescripcionPremio() : null)
                .build();
    }
}
