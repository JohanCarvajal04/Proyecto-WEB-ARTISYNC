package uteq.edu.ec.artisync.service.seguridad.impl;
import uteq.edu.ec.artisync.service.seguridad.*;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
import uteq.edu.ec.artisync.dto.seguridad.request.PaisRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.response.PaisResponse;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoDuplicado;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.entity.seguridad.Pais;
import uteq.edu.ec.artisync.repository.seguridad.PaisRepository;
import uteq.edu.ec.artisync.service.seguridad.PaisService;
import uteq.edu.ec.artisync.service.shared.StoredProcedureExceptionTranslator;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PaisServiceImpl implements PaisService {

    private final PaisRepository paisRepository;

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
    // Fase 3 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §4): delega
    // en fn_guardar_pais, que captura unique_violation sobre el nombre en vez
    // de la comprobacion findByNombrePais previa a esta version, que no era
    // atomica respecto al save() (lectura fantasma, misma clase de anomalia
    // que A9 en updatePais). El tipo de excepcion de negocio se preserva
    // (ExcepcionRecursoDuplicado) para no romper el contrato ya establecido
    // de este servicio con su capa de presentacion.
    public PaisResponse createPais(PaisRequest request) {
        Long idPais;
        try {
            idPais = paisRepository.guardarPais(null, request.getNombrePais());
        } catch (RuntimeException e) {
            throw traducirExcepcionDuplicado(e, request.getNombrePais());
        }

        // Camino inalcanzable en operacion normal (la fila que se acaba de
        // insertar en la MISMA transaccion siempre deberia ser legible aqui):
        // ResponseStatusException(500), no una excepcion de negocio -- esto
        // senalaria un fallo del servidor, no un error de entrada del cliente.
        // (IllegalStateException se descarto: ManejadorGlobalExcepciones la
        // mapea a 400, la semantica HTTP incorrecta para un fallo interno.)
        Pais pais = paisRepository.findById(idPais)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Error al crear el país"));
        return toResponse(pais);
    }

    @Override
    @Transactional
    @Auditable(accion = "PAIS_EDITAR", modulo = ModuloAuditoria.SISTEMA,
            entidad = "pais", idEntidad = "#id",
            detalle = "{nombrePais: #request.nombrePais}")
    // Fase 3 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §4): corrige
    // la anomalia A9. fn_guardar_pais captura unique_violation sobre el
    // nombre en vez de la comprobacion findByNombrePais previa a esta
    // version, que no era atomica respecto al save(): entre comprobar "el
    // nombre no pertenece a otro pais" y guardar, otra transaccion podia
    // tomar ese mismo nombre (lectura fantasma).
    public PaisResponse updatePais(Long id, PaisRequest request) {
        if (!paisRepository.existsById(id)) {
            throw new ExcepcionRecursoNoEncontrado("País no encontrado con ID: " + id);
        }

        try {
            paisRepository.guardarPais(id, request.getNombrePais());
        } catch (RuntimeException e) {
            throw traducirExcepcionDuplicado(e, request.getNombrePais());
        }

        Pais pais = paisRepository.findById(id)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("País no encontrado con ID: " + id));
        return toResponse(pais);
    }

    /**
     * Traduce la excepcion nativa de fn_guardar_pais al vocabulario de
     * excepciones de negocio ya establecido en este servicio
     * (ExcepcionRecursoDuplicado/ExcepcionRecursoNoEncontrado), reutilizando
     * StoredProcedureExceptionTranslator solo para el trabajo de desenvolver
     * la SQLException y limpiar el mensaje.
     */
    private RuntimeException traducirExcepcionDuplicado(RuntimeException origen, String nombrePais) {
        ResponseStatusException traducido = StoredProcedureExceptionTranslator.traducir(origen, HttpStatus.BAD_REQUEST);
        if (traducido.getStatusCode() == HttpStatus.CONFLICT) {
            return new ExcepcionRecursoDuplicado(traducido.getReason());
        }
        if (traducido.getStatusCode() == HttpStatus.NOT_FOUND) {
            return new ExcepcionRecursoNoEncontrado(traducido.getReason());
        }
        return traducido;
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


