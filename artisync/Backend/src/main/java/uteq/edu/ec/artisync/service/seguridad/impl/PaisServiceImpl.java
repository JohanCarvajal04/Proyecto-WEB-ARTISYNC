package uteq.edu.ec.artisync.service.seguridad.impl;
import uteq.edu.ec.artisync.service.seguridad.*;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
import uteq.edu.ec.artisync.dto.seguridad.request.PaisRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.response.PaisResponse;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoDuplicado;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.entity.seguridad.Pais;
import uteq.edu.ec.artisync.repository.seguridad.PaisRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.service.seguridad.PaisService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaisServiceImpl implements PaisService {

    private final PaisRepository paisRepository;
    private final UsuarioRepository usuarioRepository;

    @Override
    @Transactional(readOnly = true)
    public List<PaisResponse> getAllPaises() {
        return paisRepository.findAll(Sort.by(Sort.Direction.ASC, "nombrePais")).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaisResponse> getPaisesActivos() {
        return paisRepository.findByEstadoTrue(Sort.by(Sort.Direction.ASC, "nombrePais")).stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PaisResponse getPaisById(Long id) {
        Pais pais = paisRepository.findById(id)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("País no encontrado con ID: " + id));
        return toResponse(pais);
    }

    @Override
    @Transactional
    @Auditable(accion = "PAIS_CREAR", modulo = ModuloAuditoria.SISTEMA,
            entidad = "pais", idEntidad = "#resultado.idPais",
            detalle = "{nombrePais: #request.nombrePais}")
    public PaisResponse createPais(PaisRequest request) {
        String nombreTrimmed = request.getNombrePais().trim();
        if (paisRepository.findByNombrePais(nombreTrimmed).isPresent()) {
            throw new ExcepcionRecursoDuplicado("Ya existe un país registrado con el nombre: " + nombreTrimmed);
        }

        Pais pais = Pais.builder()
                .nombrePais(nombreTrimmed)
                .build();

        return toResponse(paisRepository.save(pais));
    }

    @Override
    @Transactional
    @Auditable(accion = "PAIS_EDITAR", modulo = ModuloAuditoria.SISTEMA,
            entidad = "pais", idEntidad = "#id",
            detalle = "{nombrePais: #request.nombrePais}")
    public PaisResponse updatePais(Long id, PaisRequest request) {
        Pais pais = paisRepository.findById(id)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("País no encontrado con ID: " + id));

        String nombreTrimmed = request.getNombrePais().trim();
        paisRepository.findByNombrePais(nombreTrimmed).ifPresent(p -> {
            if (!p.getIdPais().equals(id)) {
                throw new ExcepcionRecursoDuplicado("Ya existe otro país registrado con el nombre: " + nombreTrimmed);
            }
        });

        pais.setNombrePais(nombreTrimmed);
        return toResponse(paisRepository.save(pais));
    }

    @Override
    @Transactional
    @Auditable(accion = "PAIS_CAMBIAR_ESTADO", modulo = ModuloAuditoria.SISTEMA, entidad = "pais", idEntidad = "#id")
    public RespuestaMensaje deletePais(Long id) {
        Pais pais = paisRepository.findById(id)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("País no encontrado con ID: " + id));

        boolean nuevoEstado = !pais.getEstado();
        pais.setEstado(nuevoEstado);
        paisRepository.save(pais);

        String accionStr = nuevoEstado ? "reactivado" : "desactivado";
        return new RespuestaMensaje("País " + accionStr + " exitosamente");
    }

    private PaisResponse toResponse(Pais pais) {
        return PaisResponse.builder()
                .idPais(pais.getIdPais())
                .nombrePais(pais.getNombrePais())
                .estado(pais.getEstado())
                .build();
    }
}


