package uteq.edu.ec.artisync.service.perfil.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortafolioServicioImpl implements IPortafolioServicio {

    private final PortafolioRepository portafolioRepository;
    private final PerfilCreadorRepository perfilRepository;

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
                .colorPlantilla(peticion.colorPlantilla() != null ? peticion.colorPlantilla() : "#FFFFFF")
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
    public RespuestaPortafolio actualizarPortafolio(Long idPortafolio, PeticionActualizarPortafolio peticion) {
        Portafolio portafolio = portafolioRepository.findById(idPortafolio)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Portafolio no encontrado con ID: " + idPortafolio));

        if (peticion.esPublico() != null) {
            portafolio.setEsPublico(peticion.esPublico());
        }
        if (peticion.colorPlantilla() != null) {
            portafolio.setColorPlantilla(peticion.colorPlantilla());
        }

        Portafolio actualizado = portafolioRepository.save(portafolio);
        return mapearARespuesta(actualizado);
    }

    @Override
    @Transactional
    public void incrementarVisitas(Long idPortafolio) {
        Portafolio portafolio = portafolioRepository.findById(idPortafolio)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Portafolio no encontrado con ID: " + idPortafolio));
        portafolio.setTotalVisitasAcumuladas(portafolio.getTotalVisitasAcumuladas() + 1);
        portafolioRepository.save(portafolio);
    }

    @Override
    @Transactional
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
                .colorPlantilla(portafolio.getColorPlantilla())
                .build();
    }
}

