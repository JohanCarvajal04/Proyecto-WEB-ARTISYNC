package uteq.edu.ec.artisync.service.profile.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.request.profile.CreatePortfolioRequest;
import uteq.edu.ec.artisync.dto.request.profile.UpdatePortfolioRequest;
import uteq.edu.ec.artisync.dto.response.profile.PortfolioResponse;
import uteq.edu.ec.artisync.entity.profile.CreatorProfile;
import uteq.edu.ec.artisync.entity.profile.Portfolio;
import uteq.edu.ec.artisync.entity.security.User;
import uteq.edu.ec.artisync.exception.DuplicateResourceException;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.profile.CreatorProfileRepository;
import uteq.edu.ec.artisync.repository.profile.PortfolioRepository;
import uteq.edu.ec.artisync.service.profile.IPortfolioService;

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

    /**
     * {@inheritDoc}
     * @param peticion el peticion
     * @param idUsuarioLogueado el identificador de usuario logueado
     * @return el resultado de la operacion, de tipo {@code PortfolioResponse}
     */
    @Override
    @Transactional
    public PortfolioResponse createPortfolio(CreatePortfolioRequest peticion, Long idUsuarioLogueado) {
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
        return mapToResponse(guardado);
    }

    /**
     * {@inheritDoc}
     * @param idPortafolio el identificador de portafolio
     * @return el resultado de la operacion, de tipo {@code PortfolioResponse}
     */
    @Override
    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolioById(Long idPortafolio) {
        Portfolio portafolio = portafolioRepository.findById(idPortafolio)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio no encontrado con ID: " + idPortafolio));
        requireActiveAccount(portafolio);
        return mapToResponse(portafolio);
    }

    /**
     * {@inheritDoc}
     * @param idPerfil el identificador de perfil
     * @return el resultado de la operacion, de tipo {@code PortfolioResponse}
     */
    @Override
    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolioByProfile(Long idPerfil) {
        Portfolio portafolio = portafolioRepository.findByPerfilIdPerfil(idPerfil)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontró portafolio para el perfil con ID: " + idPerfil));
        requireActiveAccount(portafolio);
        return mapToResponse(portafolio);
    }

    /**
     * REQ-NF-018 (ajuste de seguimiento): mismo criterio que
     * CreatorProfileServiceImpl.requireActiveAccount. Un portafolio sin perfil
     * asociado (dato huérfano, ya contemplado por mapToResponse) no se
     * confunde con "cuenta desactivada" — solo se rechaza cuando SÍ hay un
     * dueño identificado y su cuenta está inactiva.
     */
    private void requireActiveAccount(Portfolio portafolio) {
        User usuario = portafolio.getPerfil() != null ? portafolio.getPerfil().getUsuario() : null;
        if (usuario != null && !Boolean.TRUE.equals(usuario.getEstadoCuenta())) {
            throw new ResourceNotFoundException("Portfolio no disponible");
        }
    }

    /**
     * {@inheritDoc}
     * @return la lista de PortfolioResponse encontrados
     */
    @Override
    @Transactional(readOnly = true)
    public List<PortfolioResponse> listPortfolios() {
        return portafolioRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     * @param idPortafolio el identificador de portafolio
     * @param peticion el peticion
     * @param idUsuarioLogueado el identificador de usuario logueado
     * @return el resultado de la operacion, de tipo {@code PortfolioResponse}
     */
    @Override
    @Transactional
    @Auditable(accion = "PORTAFOLIO_ACTUALIZAR", modulo = AuditModule.PORTAFOLIO,
            entidad = "portafolios", idEntidad = "#idPortafolio",
            detalle = "{esPublico: #peticion.esPublico}")
    public PortfolioResponse updatePortfolio(Long idPortafolio, UpdatePortfolioRequest peticion, Long idUsuarioLogueado) {
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
        return mapToResponse(actualizado);
    }

    /**
     * {@inheritDoc}
     * @param idPortafolio el identificador de portafolio
     * @param idUsuario el identificador de usuario
     */
    @Override
    @Transactional
    public void incrementVisits(Long idPortafolio, Long idUsuario) {
        if (!markVisitIfNew(idPortafolio, idUsuario)) {
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
    private boolean markVisitIfNew(Long idPortafolio, Long idUsuario) {
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

    /**
     * {@inheritDoc}
     * @param idPortafolio el identificador de portafolio
     */
    @Override
    @Transactional
    @Auditable(accion = "PORTAFOLIO_ELIMINAR", modulo = AuditModule.PORTAFOLIO,
            entidad = "portafolios", idEntidad = "#idPortafolio")
    public void deletePortfolio(Long idPortafolio) {
        if (!portafolioRepository.existsById(idPortafolio)) {
            throw new ResourceNotFoundException("Portfolio no encontrado con ID: " + idPortafolio);
        }
        portafolioRepository.deleteById(idPortafolio);
    }

    private PortfolioResponse mapToResponse(Portfolio portafolio) {
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

