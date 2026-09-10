package uteq.edu.ec.artisync.service.respaldo.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
import uteq.edu.ec.artisync.dto.peticion.respaldo.FiltroRespaldo;
import uteq.edu.ec.artisync.dto.respuesta.respaldo.RespuestaRespaldo;
import uteq.edu.ec.artisync.entity.respaldo.EstadoRespaldo;
import uteq.edu.ec.artisync.entity.respaldo.Respaldo;
import uteq.edu.ec.artisync.entity.respaldo.TipoRespaldo;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.respaldo.RespaldoRepository;
import uteq.edu.ec.artisync.scheduler.RespaldoEjecutorServicio;
import uteq.edu.ec.artisync.scheduler.RespaldoRetencionScheduler;
import uteq.edu.ec.artisync.service.respaldo.ArchivoRespaldo;
import uteq.edu.ec.artisync.service.respaldo.IRespaldoServicio;
import uteq.edu.ec.artisync.specification.respaldo.RespaldoSpecification;
import uteq.edu.ec.artisync.util.PagedResponse;
import uteq.edu.ec.artisync.util.PagedResponseBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@RequiredArgsConstructor
public class RespaldoServicioImpl implements IRespaldoServicio {

    private final RespaldoRepository respaldoRepository;
    private final RespaldoEjecutorServicio respaldoEjecutorServicio;
    private final RespaldoRetencionScheduler retencionScheduler;

    /**
     * Sin @Transactional a propósito: el INSERT que hace
     * RespaldoEjecutorServicio.iniciarManual debe quedar comprometido antes
     * de que el hilo @Async (con su propia conexión) intente leer esa misma
     * fila -- si este método llevara @Transactional, el hilo async podría
     * arrancar antes de que el commit sea visible.
     */
    @Auditable(accion = "RESPALDO_CREAR", modulo = ModuloAuditoria.SISTEMA,
            entidad = "respaldos", idEntidad = "#resultado.idRespaldo",
            detalle = "{tipoRespaldo: #tipo}")
    @Override
    public RespuestaRespaldo solicitarRespaldo(TipoRespaldo tipo, String correoSolicitante) {
        if (respaldoRepository.existsByEstadoRespaldo(EstadoRespaldo.EN_PROGRESO)) {
            throw new ExcepcionReglaNegocio("Ya hay un respaldo en progreso. Espere a que termine antes de iniciar otro.");
        }
        Respaldo respaldo = respaldoEjecutorServicio.iniciarManual(tipo, correoSolicitante);
        return aRespuesta(respaldo);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<RespuestaRespaldo> listar(FiltroRespaldo filtro, Pageable pageable) {
        Page<Respaldo> pagina = respaldoRepository.findAll(RespaldoSpecification.conFiltro(filtro), pageable);
        return PagedResponseBuilder.buildAndMap(pagina, this::aRespuesta);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaRespaldo obtenerPorId(Long idRespaldo) {
        return aRespuesta(obtenerOFallar(idRespaldo));
    }

    @Override
    @Transactional(readOnly = true)
    public ArchivoRespaldo descargar(Long idRespaldo) {
        Respaldo respaldo = obtenerOFallar(idRespaldo);
        if (respaldo.getRutaArchivo() == null) {
            throw new ExcepcionRecursoNoEncontrado("El respaldo " + idRespaldo + " todavía no tiene un archivo generado");
        }
        Path ruta = Path.of(respaldo.getRutaArchivo());
        if (!Files.exists(ruta)) {
            throw new ExcepcionRecursoNoEncontrado("El archivo del respaldo " + idRespaldo + " no existe en disco");
        }
        // FileSystemResource -> Spring transmite el InputStream a la respuesta
        // en streaming, nunca un byte[] completo en memoria (a diferencia de
        // DocumentoGenerado/RespuestaDocumento, pensados para reportes
        // pequeños; un dump de BD puede ser mucho más grande).
        return new ArchivoRespaldo(new FileSystemResource(ruta), respaldo.getNombreArchivo(), tamano(ruta));
    }

    @Auditable(accion = "RESPALDO_ELIMINAR", modulo = ModuloAuditoria.SISTEMA,
            entidad = "respaldos", idEntidad = "#idRespaldo")
    @Override
    @Transactional
    public void eliminar(Long idRespaldo) {
        Respaldo respaldo = obtenerOFallar(idRespaldo);
        if (respaldo.getEstadoRespaldo() == EstadoRespaldo.EN_PROGRESO) {
            throw new ExcepcionReglaNegocio("No se puede eliminar un respaldo en progreso.");
        }
        if (!retencionScheduler.esSeguroEliminar(respaldo)) {
            throw new ExcepcionReglaNegocio(
                    "No se puede eliminar: existen respaldos incrementales que dependen de este FULL. Elimínelos primero.");
        }
        if (respaldo.getRutaArchivo() != null) {
            try {
                Files.deleteIfExists(Path.of(respaldo.getRutaArchivo()));
            } catch (IOException e) {
                throw new IllegalStateException("No se pudo borrar el archivo del respaldo: " + e.getMessage(), e);
            }
        }
        respaldoRepository.delete(respaldo);
    }

    private Respaldo obtenerOFallar(Long idRespaldo) {
        return respaldoRepository.findById(idRespaldo)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Respaldo no encontrado con ID: " + idRespaldo));
    }

    private long tamano(Path ruta) {
        try {
            return Files.size(ruta);
        } catch (IOException e) {
            return 0L;
        }
    }

    private RespuestaRespaldo aRespuesta(Respaldo respaldo) {
        return RespuestaRespaldo.builder()
                .idRespaldo(respaldo.getIdRespaldo())
                .tipoRespaldo(respaldo.getTipoRespaldo())
                .estadoRespaldo(respaldo.getEstadoRespaldo())
                .origen(respaldo.getOrigen())
                .idProgramacion(respaldo.getProgramacion() != null ? respaldo.getProgramacion().getIdProgramacion() : null)
                .idRespaldoFullBase(respaldo.getIdRespaldoFullBase())
                .nombreArchivo(respaldo.getNombreArchivo())
                .tamanoBytes(respaldo.getTamanoBytes())
                .fechaInicio(respaldo.getFechaInicio())
                .fechaFin(respaldo.getFechaFin())
                .duracionMs(respaldo.getDuracionMs())
                .mensajeError(respaldo.getMensajeError())
                .correoSolicitante(respaldo.getCorreoSolicitante())
                .build();
    }
}
