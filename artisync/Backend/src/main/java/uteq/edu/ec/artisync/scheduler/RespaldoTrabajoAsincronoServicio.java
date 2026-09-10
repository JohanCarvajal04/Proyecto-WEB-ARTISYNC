package uteq.edu.ec.artisync.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import uteq.edu.ec.artisync.entity.respaldo.EstadoRespaldo;
import uteq.edu.ec.artisync.entity.respaldo.Respaldo;
import uteq.edu.ec.artisync.entity.respaldo.TipoRespaldo;
import uteq.edu.ec.artisync.repository.respaldo.RespaldoRepository;
import uteq.edu.ec.artisync.service.respaldo.impl.IncrementalRespaldoExportador;
import uteq.edu.ec.artisync.service.respaldo.impl.PgDumpEjecutor;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

/**
 * Trabajo @Async real de un respaldo (FULL vía PgDumpEjecutor, INCREMENTAL
 * vía IncrementalRespaldoExportador). Bean separado de
 * RespaldoEjecutorServicio a propósito: @Async no aplica en auto-invocación
 * (this.metodo()), así que el método anotado debe vivir en un bean distinto
 * del que lo invoca — mismo motivo por el que SorteoScheduler delega en
 * SorteoEjecutorServicio en vez de hacerlo todo en una clase.
 *
 * Sin @Transactional de clase/método a propósito: envolver todo el
 * pg_dump/COPY (que puede tardar minutos) en una única transacción Spring
 * dejaría una conexión del pool ociosa todo ese tiempo. El save() final hace
 * su propio commit implícito.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RespaldoTrabajoAsincronoServicio {

    private final RespaldoRepository respaldoRepository;
    private final PgDumpEjecutor pgDumpEjecutor;
    private final IncrementalRespaldoExportador incrementalExportador;

    @Async("respaldoTaskExecutor")
    public void ejecutar(Long idRespaldo) {
        Respaldo respaldo = respaldoRepository.findById(idRespaldo).orElse(null);
        if (respaldo == null) {
            log.error("[RespaldoTrabajoAsincronoServicio] Respaldo {} no encontrado para ejecución async", idRespaldo);
            return;
        }

        long inicioNanos = System.nanoTime();
        try {
            Path archivo = respaldo.getTipoRespaldo() == TipoRespaldo.FULL
                    ? pgDumpEjecutor.ejecutar(respaldo)
                    : incrementalExportador.ejecutar(respaldo);

            respaldo.setEstadoRespaldo(EstadoRespaldo.COMPLETADO);
            respaldo.setNombreArchivo(archivo.getFileName().toString());
            respaldo.setRutaArchivo(archivo.toString());
            respaldo.setTamanoBytes(Files.size(archivo));
        } catch (Exception e) {
            log.error("[RespaldoTrabajoAsincronoServicio] Falló el respaldo {}: {}", idRespaldo, e.getMessage(), e);
            respaldo.setEstadoRespaldo(EstadoRespaldo.FALLIDO);
            respaldo.setMensajeError(truncar(e.getMessage(), 500));
        } finally {
            respaldo.setFechaFin(LocalDateTime.now());
            respaldo.setDuracionMs((int) ((System.nanoTime() - inicioNanos) / 1_000_000));
            respaldoRepository.save(respaldo);
        }
    }

    private String truncar(String mensaje, int maximo) {
        if (mensaje == null) {
            return "Error desconocido";
        }
        return mensaje.length() > maximo ? mensaje.substring(0, maximo) : mensaje;
    }
}
