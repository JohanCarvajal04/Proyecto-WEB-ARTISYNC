package uteq.edu.ec.artisync.service.perfil.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionCrearPortafolio;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionActualizarPortafolio;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaPortafolio;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.perfil.Portafolio;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoDuplicado;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.repository.perfil.PerfilCreadorRepository;
import uteq.edu.ec.artisync.repository.perfil.PortafolioRepository;
import uteq.edu.ec.artisync.service.perfil.IPortafolioServicio;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortafolioServicioImpl implements IPortafolioServicio {

    /** Una visita del mismo usuario al mismo portafolio solo cuenta una vez por día. */
    private static final Duration VENTANA_DEDUP_VISITA = Duration.ofHours(24);

    private final PortafolioRepository portafolioRepository;
    private final PerfilCreadorRepository perfilRepository;
    private final StringRedisTemplate redisTemplate;

    @Override
    @Transactional
    public RespuestaPortafolio crearPortafolio(PeticionCrearPortafolio peticion) {
        if (portafolioRepository.findByPerfilIdPerfil(peticion.idPerfil()).isPresent()) {
            throw new ExcepcionRecursoDuplicado("El perfil de creador ya cuenta con un portafolio registrado.");
        }

        PerfilCreador perfil = perfilRepository.findById(peticion.idPerfil())
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Perfil no encontrado con ID: " + peticion.idPerfil()));

        Portafolio portafolio = Portafolio.builder()
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

        Portafolio guardado = portafolioRepository.save(portafolio);
        return mapearARespuesta(guardado);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaPortafolio obtenerPortafolioPorId(Long idPortafolio) {
        Portafolio portafolio = portafolioRepository.findById(idPortafolio)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Portafolio no encontrado con ID: " + idPortafolio));
        return mapearARespuesta(portafolio);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaPortafolio obtenerPortafolioPorPerfil(Long idPerfil) {
        Portafolio portafolio = portafolioRepository.findByPerfilIdPerfil(idPerfil)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("No se encontró portafolio para el perfil con ID: " + idPerfil));
        return mapearARespuesta(portafolio);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaPortafolio> listarPortafolios() {
        return portafolioRepository.findAll().stream()
                .map(this::mapearARespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    @Auditable(accion = "PORTAFOLIO_ACTUALIZAR", modulo = ModuloAuditoria.PORTAFOLIO,
            entidad = "portafolios", idEntidad = "#idPortafolio",
            detalle = "{esPublico: #peticion.esPublico}")
    public RespuestaPortafolio actualizarPortafolio(Long idPortafolio, PeticionActualizarPortafolio peticion) {
        Portafolio portafolio = portafolioRepository.findById(idPortafolio)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Portafolio no encontrado con ID: " + idPortafolio));

        if (peticion.esPublico() != null) {
            portafolio.setEsPublico(peticion.esPublico());
        }
        if (peticion.opcionesPersonalizacion() != null) {
            portafolio.setOpcionesPersonalizacion(peticion.opcionesPersonalizacion());
        }

        Portafolio actualizado = portafolioRepository.save(portafolio);
        return mapearARespuesta(actualizado);
    }

    @Override
    @Transactional
    public void incrementarVisitas(Long idPortafolio, Long idUsuario) {
        if (!marcarVisitaSiEsNueva(idPortafolio, idUsuario)) {
            return;
        }
        Portafolio portafolio = portafolioRepository.findById(idPortafolio)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Portafolio no encontrado con ID: " + idPortafolio));
        portafolio.setTotalVisitasAcumuladas(portafolio.getTotalVisitasAcumuladas() + 1);
        portafolioRepository.save(portafolio);
    }

    /**
     * SETNX con TTL: true solo la primera vez que este usuario visita este
     * portafolio dentro de la ventana. Antes cualquier usuario autenticado podía
     * llamar al endpoint en bucle e inflar el contador a voluntad; con esto una
     * cuenta solo suma una visita real por portafolio al día.
     *
     * Fail-open ante caída de Redis (mismo criterio que IntentosAutenticacionService):
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
    @Auditable(accion = "PORTAFOLIO_ELIMINAR", modulo = ModuloAuditoria.PORTAFOLIO,
            entidad = "portafolios", idEntidad = "#idPortafolio")
    public void eliminarPortafolio(Long idPortafolio) {
        if (!portafolioRepository.existsById(idPortafolio)) {
            throw new ExcepcionRecursoNoEncontrado("Portafolio no encontrado con ID: " + idPortafolio);
        }
        portafolioRepository.deleteById(idPortafolio);
    }

    private RespuestaPortafolio mapearARespuesta(Portafolio portafolio) {
        return RespuestaPortafolio.builder()
                .idPortafolio(portafolio.getIdPortafolio())
                .idPerfil(portafolio.getPerfil() != null ? portafolio.getPerfil().getIdPerfil() : null)
                .fechaCreacion(portafolio.getFechaCreacion())
                .totalVisitasAcumuladas(portafolio.getTotalVisitasAcumuladas())
                .esPublico(portafolio.getEsPublico())
                .opcionesPersonalizacion(portafolio.getOpcionesPersonalizacion())
                .build();
    }
}

