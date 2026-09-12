package uteq.edu.ec.artisync.service.social.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.peticion.social.UpdateRaffleRequest;
import uteq.edu.ec.artisync.dto.peticion.social.CreateRaffleRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.social.WinnerResponse;
import uteq.edu.ec.artisync.dto.respuesta.social.ParticipantResponse;
import uteq.edu.ec.artisync.dto.respuesta.social.PrizeResponse;
import uteq.edu.ec.artisync.dto.respuesta.social.RaffleResponse;
import uteq.edu.ec.artisync.entity.social.RaffleParticipant;
import uteq.edu.ec.artisync.entity.social.RafflePrize;
import uteq.edu.ec.artisync.entity.social.Raffle;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.DuplicateResourceException;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.comunicacion.FollowerRepository;
import uteq.edu.ec.artisync.repository.perfil.CreatorProfileRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;
import uteq.edu.ec.artisync.repository.social.RaffleParticipantRepository;
import uteq.edu.ec.artisync.repository.social.RaffleRepository;
import uteq.edu.ec.artisync.service.social.RaffleService;

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
public class RaffleServiceImpl implements RaffleService {

    private final RaffleRepository sorteoRepository;
    private final RaffleParticipantRepository participanteSorteoRepository;
    private final CreatorProfileRepository perfilCreadorRepository;
    private final UserRepository usuarioRepository;
    private final FollowerRepository seguidorRepository;

    // =========================================================================
    // CRUD de Sorteos (CREADOR)
    // =========================================================================

    /**
     * Crea un sorteo para el perfil de creador del usuario, con sus premios.
     *
     * @param idUsuario identificador del creador dueño del sorteo
     * @param peticion título, fechas, cantidad de ganadores y premios del sorteo
     * @return el sorteo creado, en estado {@code Activo}
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el usuario no tiene perfil de creador
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si la fecha de cierre es anterior a la
     *         de inicio, o si la cantidad de premios no coincide con la cantidad de ganadores
     */
    @Override
    @Transactional
    @Auditable(accion = "SORTEO_CREAR", modulo = AuditModule.SOCIAL,
            entidad = "sorteos", idEntidad = "#resultado.idSorteo",
            detalle = "{tituloSorteo: #peticion.tituloSorteo, cantidadGanadores: #peticion.cantidadGanadores}")
    public RaffleResponse crearSorteo(Long idUsuario, CreateRaffleRequest peticion) {
        var perfil = perfilCreadorRepository.findByUsuarioIdUsuario(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No tienes un perfil de creador activo"));

        if (peticion.getFechaCierre().isBefore(peticion.getFechaInicio())) {
            throw new BusinessRuleException(
                    "La fecha de cierre debe ser posterior a la fecha de inicio");
        }

        validarCantidadPremios(peticion.getPremios().size(), peticion.getCantidadGanadores());

        Raffle sorteo = Raffle.builder()
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
        log.info("Raffle '{}' creado por usuario {}", sorteo.getTituloSorteo(), idUsuario);
        return mapToResponse(sorteo, null, 0L, false);
    }

    /** REQ-F-023: la cantidad de ganadores debe coincidir exactamente con la cantidad de premios definidos. */
    private void validarCantidadPremios(int cantidadPremios, int cantidadGanadores) {
        if (cantidadPremios != cantidadGanadores) {
            throw new BusinessRuleException(
                    "La cantidad de ganadores (" + cantidadGanadores
                            + ") debe coincidir con la cantidad de premios definidos (" + cantidadPremios + ")");
        }
    }

    private List<RafflePrize> construirPremios(Raffle sorteo, List<String> descripciones) {
        List<RafflePrize> premios = new ArrayList<>();
        for (int i = 0; i < descripciones.size(); i++) {
            premios.add(RafflePrize.builder()
                    .sorteo(sorteo)
                    .descripcionPremio(descripciones.get(i))
                    .orden(i + 1)
                    .build());
        }
        return premios;
    }

    /**
     * @param idSorteo identificador del sorteo
     * @param idUsuarioActual identificador de quien consulta, para saber si ya participa; {@code null} si es anónimo
     * @return el sorteo, con sus ganadores si ya finalizó
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el sorteo no existe
     */
    @Override
    @Transactional(readOnly = true)
    public RaffleResponse obtenerSorteo(Long idSorteo, Long idUsuarioActual) {
        Raffle sorteo = findSorteoOrThrow(idSorteo);
        long total = participanteSorteoRepository.findBySorteoIdSorteo(idSorteo).size();
        boolean yoParticipo = idUsuarioActual != null &&
                participanteSorteoRepository.existsBySorteoIdSorteoAndUsuarioIdUsuario(idSorteo, idUsuarioActual);

        List<WinnerResponse> ganadores = obtenerGanadoresSiFinalizado(sorteo);
        return mapToResponse(sorteo, ganadores, total, yoParticipo);
    }

    /**
     * Solo hay ganadores despues del cierre del sorteo. Se usa tanto en el detalle
     * como en los listados, para que las tarjetas de la lista muestren el premio
     * de cada quien sin depender de abrir el detalle del sorteo.
     */
    private List<WinnerResponse> obtenerGanadoresSiFinalizado(Raffle sorteo) {
        if (!"Finalizado".equals(sorteo.getEstadoSorteo())) {
            return null;
        }
        return participanteSorteoRepository
                .findBySorteoIdSorteoAndEsGanadorTrue(sorteo.getIdSorteo())
                .stream().map(this::mapToGanadorResponse).collect(Collectors.toList());
    }

    /**
     * Actualiza un sorteo propio. Si ya tiene participantes inscritos, la
     * cantidad de ganadores, los premios y la fecha de cierre quedan congelados.
     *
     * @param idSorteo identificador del sorteo
     * @param idUsuario identificador del creador dueño del sorteo
     * @param peticion campos a actualizar; los {@code null} no se modifican
     * @return el sorteo ya actualizado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el sorteo no existe o el
     *         usuario no tiene perfil de creador
     * @throws org.springframework.web.server.ResponseStatusException 403 si el usuario no es dueño del sorteo
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si se intenta cambiar cantidad de
     *         ganadores, premios o fecha de cierre con participantes ya inscritos, o si la nueva fecha
     *         de cierre es anterior a la de inicio
     */
    @Override
    @Transactional
    @Auditable(accion = "SORTEO_ACTUALIZAR", modulo = AuditModule.SOCIAL,
            entidad = "sorteos", idEntidad = "#idSorteo")
    public RaffleResponse actualizarSorteo(Long idSorteo, Long idUsuario, UpdateRaffleRequest peticion) {
        Raffle sorteo = verificarPropietario(idSorteo, idUsuario);
        boolean tieneParticipantes = participanteSorteoRepository.existsBySorteoIdSorteo(idSorteo);

        if (tieneParticipantes) {
            if (peticion.getCantidadGanadores() != null &&
                    !peticion.getCantidadGanadores().equals(sorteo.getCantidadGanadores())) {
                throw new BusinessRuleException(
                        "No se puede modificar este campo una vez iniciadas las inscripciones");
            }
            if (peticion.getPremios() != null) {
                throw new BusinessRuleException(
                        "No se puede modificar este campo una vez iniciadas las inscripciones");
            }
            if (peticion.getFechaCierre() != null &&
                    !peticion.getFechaCierre().equals(sorteo.getFechaCierre())) {
                throw new BusinessRuleException(
                        "No se puede modificar la fecha de cierre una vez iniciadas las inscripciones");
            }
        }

        // fechaInicio no es editable por este DTO, así que solo se valida contra
        // ella (nunca se recalcula): sin este chequeo, una fechaCierre nueva
        // anterior a la fechaInicio original dejaba el sorteo en un estado
        // imposible — participar() rechaza tanto "aún no ha comenzado" como
        // "el periodo de inscripción ha finalizado" para cualquier instante.
        if (peticion.getFechaCierre() != null && peticion.getFechaCierre().isBefore(sorteo.getFechaInicio())) {
            throw new BusinessRuleException(
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

    /**
     * Elimina un sorteo propio, siempre que no tenga participantes inscritos.
     *
     * @param idSorteo identificador del sorteo
     * @param idUsuario identificador del creador dueño del sorteo
     * @return mensaje de confirmación
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el sorteo no existe o el
     *         usuario no tiene perfil de creador
     * @throws org.springframework.web.server.ResponseStatusException 403 si el usuario no es dueño del sorteo
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si ya tiene participantes inscritos
     */
    @Override
    @Transactional
    @Auditable(accion = "SORTEO_ELIMINAR", modulo = AuditModule.SOCIAL,
            entidad = "sorteos", idEntidad = "#idSorteo")
    public RespuestaMensaje eliminarSorteo(Long idSorteo, Long idUsuario) {
        Raffle sorteo = verificarPropietario(idSorteo, idUsuario);
        if (participanteSorteoRepository.existsBySorteoIdSorteo(idSorteo)) {
            throw new BusinessRuleException(
                    "No se puede eliminar un sorteo con participantes inscritos");
        }
        sorteoRepository.delete(sorteo);
        log.info("Raffle {} eliminado por usuario {}", idSorteo, idUsuario);
        return new RespuestaMensaje("Raffle eliminado correctamente");
    }

    /**
     * @param idPerfilCreador identificador del perfil de creador
     * @param idUsuarioActual identificador de quien consulta, para saber en cuáles ya participa; {@code null} si es anónimo
     * @return los sorteos de ese creador
     */
    @Override
    @Transactional(readOnly = true)
    public List<RaffleResponse> listarSorteosPorCreador(Long idPerfilCreador, Long idUsuarioActual) {
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

    /**
     * @param idUsuarioActual identificador de quien consulta, para saber en cuáles ya participa; {@code null} si es anónimo
     * @return los sorteos actualmente en estado {@code Activo}
     */
    @Override
    @Transactional(readOnly = true)
    public List<RaffleResponse> listarSorteosActivos(Long idUsuarioActual) {
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

    /**
     * Inscribe a un usuario en un sorteo activo, validando fechas, que no
     * esté ya inscrito y, si el sorteo lo requiere, que siga al creador.
     *
     * @param idSorteo identificador del sorteo
     * @param idUsuario identificador del usuario que se inscribe
     * @return la participación creada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el sorteo no existe
     * @throws uteq.edu.ec.artisync.exception.DuplicateResourceException si el usuario ya está inscrito
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el sorteo no está activo, si aún
     *         no comenzó, si el periodo de inscripción ya cerró, o si requiere seguir al creador y no lo sigue
     */
    @Override
    @Transactional
    public ParticipantResponse participar(Long idSorteo, Long idUsuario) {
        Raffle sorteo = findSorteoOrThrow(idSorteo);

        // Validar estado
        if (!"Activo".equals(sorteo.getEstadoSorteo())) {
            throw new BusinessRuleException("El sorteo no está activo");
        }

        // Validar rango de fechas
        LocalDateTime ahora = LocalDateTime.now();
        if (ahora.isBefore(sorteo.getFechaInicio())) {
            throw new BusinessRuleException("El sorteo aún no ha comenzado");
        }
        if (ahora.isAfter(sorteo.getFechaCierre())) {
            throw new BusinessRuleException("El periodo de inscripción ha finalizado");
        }

        // Validar inscripción duplicada
        if (participanteSorteoRepository.existsBySorteoIdSorteoAndUsuarioIdUsuario(idSorteo, idUsuario)) {
            throw new DuplicateResourceException("Ya estás inscrito en este sorteo");
        }

        // Validar requisito de seguidor
        if (Boolean.TRUE.equals(sorteo.getRequiereSeguidor())) {
            boolean esSeguidor = seguidorRepository.existsByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(
                    idUsuario, sorteo.getPerfilCreador().getIdPerfil());
            if (!esSeguidor) {
                throw new BusinessRuleException(
                        "Este sorteo requiere que sigas al creador para poder participar");
            }
        }

        User usuario = usuarioRepository.getReferenceById(idUsuario);
        RaffleParticipant participante = RaffleParticipant.builder()
                .sorteo(sorteo)
                .usuario(usuario)
                .esGanador(false)
                .build();
        participante = participanteSorteoRepository.save(participante);
        log.info("User {} inscrito en sorteo {}", idUsuario, idSorteo);
        return mapToParticipanteResponse(participante);
    }

    /**
     * Cancela la inscripción de un usuario en un sorteo aún activo.
     *
     * @param idSorteo identificador del sorteo
     * @param idUsuario identificador del usuario que cancela su inscripción
     * @return mensaje de confirmación
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el sorteo no existe, o si el
     *         usuario no está inscrito
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el sorteo ya finalizó
     */
    @Override
    @Transactional
    public RespuestaMensaje cancelarParticipacion(Long idSorteo, Long idUsuario) {
        Raffle sorteo = findSorteoOrThrow(idSorteo);
        if (!"Activo".equals(sorteo.getEstadoSorteo())) {
            throw new BusinessRuleException("No puedes cancelar la inscripción en un sorteo que ya ha finalizado");
        }
        RaffleParticipant participante = participanteSorteoRepository
                .findBySorteoIdSorteo(idSorteo)
                .stream()
                .filter(p -> p.getUsuario().getIdUsuario().equals(idUsuario))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No estás inscrito en este sorteo"));
        participanteSorteoRepository.delete(participante);
        return new RespuestaMensaje("Inscripción cancelada correctamente");
    }

    /**
     * @param idSorteo identificador del sorteo
     * @return los participantes inscritos en ese sorteo
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el sorteo no existe
     */
    @Override
    @Transactional(readOnly = true)
    public List<ParticipantResponse> listarParticipantes(Long idSorteo) {
        findSorteoOrThrow(idSorteo); // Valida que existe
        return participanteSorteoRepository.findBySorteoIdSorteo(idSorteo)
                .stream().map(this::mapToParticipanteResponse).collect(Collectors.toList());
    }

    /**
     * @param idSorteo identificador del sorteo
     * @return los ganadores del sorteo, disponibles solo tras su cierre
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el sorteo no existe
     * @throws org.springframework.web.server.ResponseStatusException 409 si el sorteo aún no finalizó
     */
    @Override
    @Transactional(readOnly = true)
    public List<WinnerResponse> listarGanadores(Long idSorteo) {
        Raffle sorteo = findSorteoOrThrow(idSorteo);
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

    private Raffle findSorteoOrThrow(Long idSorteo) {
        return sorteoRepository.findById(idSorteo)
                .orElseThrow(() -> new ResourceNotFoundException("Raffle no encontrado: " + idSorteo));
    }

    private Raffle verificarPropietario(Long idSorteo, Long idUsuario) {
        Raffle sorteo = findSorteoOrThrow(idSorteo);
        var perfil = perfilCreadorRepository.findByUsuarioIdUsuario(idUsuario)
                .orElseThrow(() -> new ResourceNotFoundException("No tienes perfil de creador"));
        if (!sorteo.getPerfilCreador().getIdPerfil().equals(perfil.getIdPerfil())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "No tienes permiso para modificar este sorteo");
        }
        return sorteo;
    }

    private RaffleResponse mapToResponse(Raffle sorteo, List<WinnerResponse> ganadores,
                                          long total, boolean yoParticipo) {
        return RaffleResponse.builder()
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
    private List<PrizeResponse> mapToPremiosResponse(Raffle sorteo, List<WinnerResponse> ganadores) {
        Map<Long, WinnerResponse> ganadorPorPremio = new HashMap<>();
        if (ganadores != null) {
            for (WinnerResponse g : ganadores) {
                if (g.getIdPremio() != null) {
                    ganadorPorPremio.put(g.getIdPremio(), g);
                }
            }
        }
        return sorteo.getPremios().stream()
                .map(p -> PrizeResponse.builder()
                        .idPremio(p.getIdPremio())
                        .descripcionPremio(p.getDescripcionPremio())
                        .orden(p.getOrden())
                        .ganador(ganadorPorPremio.get(p.getIdPremio()))
                        .build())
                .collect(Collectors.toList());
    }

    private ParticipantResponse mapToParticipanteResponse(RaffleParticipant p) {
        return ParticipantResponse.builder()
                .idParticipacion(p.getIdParticipacion())
                .idUsuario(p.getUsuario().getIdUsuario())
                .nombreUsuario(p.getUsuario().getNombres() + " " + p.getUsuario().getApellidos())
                .fechaInscripcion(p.getFechaInscripcion())
                .esGanador(p.getEsGanador())
                .build();
    }

    private WinnerResponse mapToGanadorResponse(RaffleParticipant p) {
        return WinnerResponse.builder()
                .idParticipacion(p.getIdParticipacion())
                .idUsuario(p.getUsuario().getIdUsuario())
                .nombreUsuario(p.getUsuario().getNombres() + " " + p.getUsuario().getApellidos())
                .fechaNotificacionPremio(p.getFechaNotificacionPremio())
                .idPremio(p.getPremio() != null ? p.getPremio().getIdPremio() : null)
                .descripcionPremio(p.getPremio() != null ? p.getPremio().getDescripcionPremio() : null)
                .build();
    }
}
