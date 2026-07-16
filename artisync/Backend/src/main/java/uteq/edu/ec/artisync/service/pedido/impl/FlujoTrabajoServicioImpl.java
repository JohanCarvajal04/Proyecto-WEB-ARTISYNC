package uteq.edu.ec.artisync.service.pedido.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionCrearFlujoTrabajo;
import uteq.edu.ec.artisync.dto.peticion.pedido.PeticionEtapaConfig;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaEtapaConfig;
import uteq.edu.ec.artisync.dto.respuesta.pedido.RespuestaFlujoTrabajo;
import uteq.edu.ec.artisync.entity.catalogo.FlujoTrabajo;
import uteq.edu.ec.artisync.entity.pedido.EtapaFlujo;
import uteq.edu.ec.artisync.entity.pedido.FlujoEtapaConfig;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoDuplicado;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.catalogo.FlujoTrabajoRepository;
import uteq.edu.ec.artisync.repository.pedido.EtapaFlujoRepository;
import uteq.edu.ec.artisync.repository.pedido.FlujoEtapaConfigRepository;
import uteq.edu.ec.artisync.service.pedido.IFlujoTrabajoServicio;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class FlujoTrabajoServicioImpl implements IFlujoTrabajoServicio {

    private final FlujoTrabajoRepository flujoTrabajoRepository;
    private final EtapaFlujoRepository etapaFlujoRepository;
    private final FlujoEtapaConfigRepository flujoEtapaConfigRepository;

    @Override
    @Transactional
    public RespuestaFlujoTrabajo crearFlujoTrabajo(PeticionCrearFlujoTrabajo peticion) {
        if (flujoTrabajoRepository.existsByNombreFlujo(peticion.getNombreFlujo())) {
            throw new ExcepcionRecursoDuplicado("Ya existe un flujo de trabajo con el nombre: " + peticion.getNombreFlujo());
        }

        FlujoTrabajo flujo = FlujoTrabajo.builder()
                .nombreFlujo(peticion.getNombreFlujo())
                .descripcionFlujo(peticion.getDescripcionFlujo())
                .build();

        flujo = flujoTrabajoRepository.save(flujo);

        // Crear etapas si se proporcionaron
        if (peticion.getEtapas() != null && !peticion.getEtapas().isEmpty()) {
            for (PeticionEtapaConfig etapaReq : peticion.getEtapas()) {
                EtapaFlujo etapa = obtenerOCrearEtapa(etapaReq.getNombreEtapa());

                FlujoEtapaConfig config = FlujoEtapaConfig.builder()
                        .flujo(flujo)
                        .etapa(etapa)
                        .numeroOrden(etapaReq.getNumeroOrden())
                        .esEtapaFinal(etapaReq.isEsEtapaFinal())
                        .build();

                flujoEtapaConfigRepository.save(config);
            }
        }

        log.info("Flujo de trabajo '{}' creado con ID {}", flujo.getNombreFlujo(), flujo.getIdFlujo());
        return mapToRespuesta(flujo);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RespuestaFlujoTrabajo> listarFlujosTrabajo() {
        return flujoTrabajoRepository.findAll()
                .stream()
                .map(this::mapToRespuesta)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaFlujoTrabajo obtenerFlujoPorId(Long idFlujo) {
        FlujoTrabajo flujo = flujoTrabajoRepository.findById(idFlujo)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Flujo de trabajo no encontrado con ID: " + idFlujo));
        return mapToRespuesta(flujo);
    }

    @Override
    @Transactional
    public RespuestaFlujoTrabajo actualizarFlujoTrabajo(Long idFlujo, PeticionCrearFlujoTrabajo peticion) {
        FlujoTrabajo flujo = flujoTrabajoRepository.findById(idFlujo)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Flujo de trabajo no encontrado con ID: " + idFlujo));

        flujo.setNombreFlujo(peticion.getNombreFlujo());
        flujo.setDescripcionFlujo(peticion.getDescripcionFlujo());
        flujoTrabajoRepository.save(flujo);

        log.info("Flujo de trabajo '{}' actualizado", flujo.getNombreFlujo());
        return mapToRespuesta(flujo);
    }

    @Override
    @Transactional
    public RespuestaFlujoTrabajo agregarEtapa(Long idFlujo, PeticionEtapaConfig peticion) {
        FlujoTrabajo flujo = flujoTrabajoRepository.findById(idFlujo)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Flujo de trabajo no encontrado con ID: " + idFlujo));

        EtapaFlujo etapa = obtenerOCrearEtapa(peticion.getNombreEtapa());

        if (flujoEtapaConfigRepository.existsByFlujoIdFlujoAndEtapaIdEtapa(idFlujo, etapa.getIdEtapa())) {
            throw new ExcepcionRecursoDuplicado("La etapa '" + peticion.getNombreEtapa() + "' ya existe en este flujo");
        }

        FlujoEtapaConfig config = FlujoEtapaConfig.builder()
                .flujo(flujo)
                .etapa(etapa)
                .numeroOrden(peticion.getNumeroOrden())
                .esEtapaFinal(peticion.isEsEtapaFinal())
                .build();

        flujoEtapaConfigRepository.save(config);
        log.info("Etapa '{}' agregada al flujo '{}'", etapa.getNombreEtapa(), flujo.getNombreFlujo());

        return mapToRespuesta(flujo);
    }

    @Override
    @Transactional
    public RespuestaFlujoTrabajo actualizarEtapa(Long idFlujo, Long idFlujoEtapa, PeticionEtapaConfig peticion) {
        FlujoTrabajo flujo = flujoTrabajoRepository.findById(idFlujo)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Flujo de trabajo no encontrado"));

        FlujoEtapaConfig config = flujoEtapaConfigRepository.findById(idFlujoEtapa)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Configuracion de etapa no encontrada"));

        if (!config.getFlujo().getIdFlujo().equals(idFlujo)) {
            throw new ExcepcionReglaNegocio("La etapa no pertenece al flujo especificado");
        }

        config.setNumeroOrden(peticion.getNumeroOrden());
        config.setEsEtapaFinal(peticion.isEsEtapaFinal());

        flujoEtapaConfigRepository.save(config);
        log.info("Etapa {} actualizada en flujo {}", idFlujoEtapa, idFlujo);

        return mapToRespuesta(flujo);
    }

    @Override
    @Transactional
    public void eliminarEtapa(Long idFlujo, Long idFlujoEtapa) {
        FlujoEtapaConfig config = flujoEtapaConfigRepository.findById(idFlujoEtapa)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Configuracion de etapa no encontrada"));

        if (!config.getFlujo().getIdFlujo().equals(idFlujo)) {
            throw new ExcepcionReglaNegocio("La etapa no pertenece al flujo especificado");
        }

        flujoEtapaConfigRepository.delete(config);
        log.info("Etapa {} eliminada del flujo {}", idFlujoEtapa, idFlujo);
    }

    // ── Métodos auxiliares ───────────────────────────────────────────────────

    private EtapaFlujo obtenerOCrearEtapa(String nombreEtapa) {
        return etapaFlujoRepository.findByNombreEtapa(nombreEtapa)
                .orElseGet(() -> {
                    EtapaFlujo nueva = EtapaFlujo.builder()
                            .nombreEtapa(nombreEtapa)
                            .build();
                    return etapaFlujoRepository.save(nueva);
                });
    }

    private RespuestaFlujoTrabajo mapToRespuesta(FlujoTrabajo flujo) {
        List<FlujoEtapaConfig> etapas = flujoEtapaConfigRepository
                .findByFlujoIdFlujoOrderByNumeroOrdenAsc(flujo.getIdFlujo());

        return RespuestaFlujoTrabajo.builder()
                .idFlujo(flujo.getIdFlujo())
                .nombreFlujo(flujo.getNombreFlujo())
                .descripcionFlujo(flujo.getDescripcionFlujo())
                .etapas(etapas.stream().map(this::mapEtapaConfig).collect(Collectors.toList()))
                .build();
    }

    private RespuestaEtapaConfig mapEtapaConfig(FlujoEtapaConfig config) {
        return RespuestaEtapaConfig.builder()
                .idFlujoEtapa(config.getIdFlujoEtapa())
                .idEtapa(config.getEtapa().getIdEtapa())
                .nombreEtapa(config.getEtapa().getNombreEtapa())
                .numeroOrden(config.getNumeroOrden())
                .esEtapaFinal(config.getEsEtapaFinal())
                .build();
    }
}
