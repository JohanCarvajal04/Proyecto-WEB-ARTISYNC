package uteq.edu.ec.artisync.service.perfil.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.peticion.perfil.CreatePortfolioRequest;
import uteq.edu.ec.artisync.dto.peticion.perfil.UpdatePortfolioRequest;
import uteq.edu.ec.artisync.dto.respuesta.perfil.PortfolioResponse;
import uteq.edu.ec.artisync.entity.perfil.CreatorProfile;
import uteq.edu.ec.artisync.entity.perfil.Portfolio;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.DuplicateResourceException;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.perfil.CreatorProfileRepository;
import uteq.edu.ec.artisync.repository.perfil.PortfolioRepository;
import uteq.edu.ec.artisync.service.perfil.IPortfolioService;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements IPortfolioService {

    /** Una visita del mismo usuario al mismo portafolio solo cuenta una vez por día. */
    private static final Duration VENTANA_DEDUP_VISITA = Duration.ofHours(24);

    private final PortfolioRepository portafolioRepository;
    private final CreatorProfileRepository perfilRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    /**
     * Crea el portafolio de un perfil de creador (relación 1:1), con opciones
     * de personalización por defecto si no se indican.
     *
     * @param peticion perfil dueño, visibilidad y opciones de personalización
     * @param idUsuarioLogueado identificador de quien crea; debe ser dueño del perfil
     * @return el portafolio creado
     * @throws uteq.edu.ec.artisync.exception.DuplicateResourceException si el perfil ya tiene un portafolio
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el perfil no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si quien crea no es dueño del perfil
     */
    public PortfolioResponse crearPortafolio(CreatePortfolioRequest peticion, Long idUsuarioLogueado) {
        if (portafolioRepository.findByPerfilIdPerfil(peticion.idPerfil()).isPresent()) {
            throw new DuplicateResourceException("El perfil de creador ya cuenta con un portafolio registrado.");
        }

        CreatorProfile perfil = perfilRepository.findById(peticion.idPerfil())
                .orElseThrow(() -> new ResourceNotFoundException("Perfil no encontrado con ID: " + peticion.idPerfil()));

        if (!perfil.getUsuario().getIdUsuario().equals(idUsuarioLogueado)) {
            throw new BusinessRuleException("No tiene permisos para crear un portafolio para este perfil.");
        }

        Portfolio portafolio = Portfolio.builder()
                .perfil(perfil)
                .esPublico(peticion.esPublico() != null ? peticion.esPublico() : true)
                .opcionesPersonalizacion(peticion.opcionesPersonalizacion() != null ? peticion.opcionesPersonalizacion() : java.util.Map.of(
                        "primary", "#0d6efd",
                        "secondary", "#6c757d",
                        "bg", "#f8f9fa",
                        "text", "#212529",
                        "surface", "#ffffff"
                ))
                .totalVisitasAcumuladas(0)
                .build();

        Portfolio guardado = portafolioRepository.save(portafolio);
        return mapearARespuesta(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * @param idPortafolio identificador del portafolio
     * @return el portafolio solicitado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el portafolio no existe
     *         o su dueño tiene la cuenta desactivada
     */
    public PortfolioResponse obtenerPortafolioPorId(Long idPortafolio) {
        Portfolio portafolio = portafolioRepository.findById(idPortafolio)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio no encontrado con ID: " + idPortafolio));
        exigirCuentaActiva(portafolio);
        return mapearARespuesta(portafolio);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * @param idPerfil identificador del perfil de creador
     * @return el portafolio de ese perfil
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el perfil no tiene
     *         portafolio, o su dueño tiene la cuenta desactivada
     */
    public PortfolioResponse obtenerPortafolioPorPerfil(Long idPerfil) {
        Portfolio portafolio = portafolioRepository.findByPerfilIdPerfil(idPerfil)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró portafolio para el perfil con ID: " + idPerfil));
        exigirCuentaActiva(portafolio);
        return mapearARespuesta(portafolio);
    }

    /**
     * REQ-NF-018 (ajuste de seguimiento): mismo criterio que
     * CreatorProfileServiceImpl.exigirCuentaActiva. Un portafolio sin perfil
     * asociado (dato huérfano, ya contemplado por mapearARespuesta) no se
     * confunde con "cuenta desactivada" — solo se rechaza cuando SÍ hay un
     * dueño identificado y su cuenta está inactiva.
     */
    private void exigirCuentaActiva(Portfolio portafolio) {
        User usuario = portafolio.getPerfil() != null ? portafolio.getPerfil().getUsuario() : null;
        if (usuario != null && !Boolean.TRUE.equals(usuario.getEstadoCuenta())) {
            throw new ResourceNotFoundException("Portfolio no disponible");
        }
    }

    @Override
    @Transactional(readOnly = true)
    /** @return todos los portafolios registrados */
    public List<PortfolioResponse> listarPortafolios() {
        return portafolioRepository.findAll().stream()
                .map(this::mapearARespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Auditable(accion = "PORTAFOLIO_ACTUALIZAR", modulo = AuditModule.PORTAFOLIO,
            entidad = "portafolios", idEntidad = "#idPortafolio",
            detalle = "{esPublico: #peticion.esPublico}")
    /**
     * Actualiza la visibilidad y/o las opciones de personalización de un portafolio propio.
     *
     * @param idPortafolio identificador del portafolio
     * @param peticion campos a actualizar; los {@code null} no se modifican
     * @param idUsuarioLogueado identificador de quien edita; debe ser dueño del portafolio
     * @return el portafolio ya actualizado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el portafolio no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si quien edita no es su dueño
     */
    public PortfolioResponse actualizarPortafolio(Long idPortafolio, UpdatePortfolioRequest peticion, Long idUsuarioLogueado) {
        Portfolio portafolio = portafolioRepository.findById(idPortafolio)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio no encontrado con ID: " + idPortafolio));

        Long idDuenio = portafolio.getPerfil().getUsuario().getIdUsuario();
        if (!idDuenio.equals(idUsuarioLogueado)) {
            throw new BusinessRuleException("No tiene permisos para modificar este portafolio.");
        }

        if (peticion.esPublico() != null) {
            portafolio.setEsPublico(peticion.esPublico());
        }
        if (peticion.opcionesPersonalizacion() != null) {
            portafolio.setOpcionesPersonalizacion(peticion.opcionesPersonalizacion());
        }

        Portfolio actualizado = portafolioRepository.save(portafolio);
        return mapearARespuesta(actualizado);
    }

    @Override
    @Transactional
    /**
     * Registra una visita al portafolio, deduplicada por usuario dentro de
     * una ventana de 24h (una visita real cuenta una sola vez al día).
     *
     * @param idPortafolio identificador del portafolio visitado
     * @param idUsuario identificador del usuario que visita
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el portafolio no existe
     */
    public void incrementarVisitas(Long idPortafolio, Long idUsuario) {
        if (!marcarVisitaSiEsNueva(idPortafolio, idUsuario)) {
            return;
        }
        Portfolio portafolio = portafolioRepository.findById(idPortafolio)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio no encontrado con ID: " + idPortafolio));
        portafolio.setTotalVisitasAcumuladas(portafolio.getTotalVisitasAcumuladas() + 1);
        portafolioRepository.save(portafolio);
    }

    /**
     * SETNX con TTL: true solo la primera vez que este usuario visita este
     * portafolio dentro de la ventana. Antes cualquier usuario autenticado podía
     * llamar al endpoint en bucle e inflar el contador a voluntad; con esto una
     * cuenta solo suma una visita real por portafolio al día.
     *
     * Fail-open ante caída de Redis (mismo criterio que AuthAttemptsService):
     * si Redis no responde, se cuenta la visita en vez de bloquear la métrica.
     */
    private boolean marcarVisitaSiEsNueva(Long idPortafolio, Long idUsuario) {
        String clave = "visita-portafolio:" + idPortafolio + ":" + idUsuario;
        try {
            Boolean esNueva = redisTemplate.opsForValue().setIfAbsent(clave, "1", VENTANA_DEDUP_VISITA);
            return !Boolean.FALSE.equals(esNueva);
        } catch (DataAccessException e) {
            log.warn("No se pudo deduplicar la visita al portafolio {} en Redis; se cuenta igual (fail-open): {}",
                    idPortafolio, e.getMessage());
            return true;
        }
    }

    @Override
    @Transactional
    @Auditable(accion = "PORTAFOLIO_ELIMINAR", modulo = AuditModule.PORTAFOLIO,
            entidad = "portafolios", idEntidad = "#idPortafolio")
    /**
     * @param idPortafolio identificador del portafolio a eliminar
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el portafolio no existe
     */
    public void eliminarPortafolio(Long idPortafolio) {
        if (!portafolioRepository.existsById(idPortafolio)) {
            throw new ResourceNotFoundException("Portfolio no encontrado con ID: " + idPortafolio);
        }
        portafolioRepository.deleteById(idPortafolio);
    }

    private PortfolioResponse mapearARespuesta(Portfolio portafolio) {
        return PortfolioResponse.builder()
                .idPortafolio(portafolio.getIdPortafolio())
                .idPerfil(portafolio.getPerfil() != null ? portafolio.getPerfil().getIdPerfil() : null)
                .fechaCreacion(portafolio.getFechaCreacion())
                .totalVisitasAcumuladas(portafolio.getTotalVisitasAcumuladas())
                .esPublico(portafolio.getEsPublico())
                .opcionesPersonalizacion(portafolio.getOpcionesPersonalizacion())
                .build();
    }
}

