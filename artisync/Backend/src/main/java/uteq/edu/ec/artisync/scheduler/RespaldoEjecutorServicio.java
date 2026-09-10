package uteq.edu.ec.artisync.scheduler;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;
import uteq.edu.ec.artisync.entity.respaldo.EstadoRespaldo;
import uteq.edu.ec.artisync.entity.respaldo.OrigenRespaldo;
import uteq.edu.ec.artisync.entity.respaldo.Respaldo;
import uteq.edu.ec.artisync.entity.respaldo.RespaldoProgramacion;
import uteq.edu.ec.artisync.entity.respaldo.TipoRespaldo;
import uteq.edu.ec.artisync.repository.respaldo.RespaldoProgramacionRepository;
import uteq.edu.ec.artisync.repository.respaldo.RespaldoRepository;

import java.time.LocalDateTime;

/**
 * Orquesta el inicio de un respaldo: crea la fila EN_PROGRESO y dispara el
 * trabajo async. Lo invocan tanto RespaldoProgramacionScheduler (programado)
 * como RespaldoServicioImpl (disparo manual del admin).
 */
@Component
@RequiredArgsConstructor
public class RespaldoEjecutorServicio {

    private final RespaldoRepository respaldoRepository;
    private final RespaldoProgramacionRepository programacionRepository;
    private final RespaldoTrabajoAsincronoServicio trabajoAsincronoServicio;

    /** Disparo manual (admin autenticado, sin programación asociada). */
    public Respaldo iniciarManual(TipoRespaldo tipo, String correoSolicitante) {
        Respaldo respaldo = crearRespaldo(tipo, OrigenRespaldo.MANUAL, null, correoSolicitante);
        trabajoAsincronoServicio.ejecutar(respaldo.getIdRespaldo());
        return respaldo;
    }

    /** Disparo desde RespaldoProgramacionScheduler. */
    public void iniciarDesdeProgramacion(RespaldoProgramacion programacion) {
        Respaldo respaldo = crearRespaldo(
                programacion.getTipoRespaldo(), OrigenRespaldo.PROGRAMADO, programacion, "sistema:scheduler");

        // proxima_ejecucion se recalcula YA, no al terminar: si el respaldo
        // tarda más de 60s no debe volver a dispararse en el siguiente tick
        // del poller.
        LocalDateTime ahora = LocalDateTime.now();
        programacion.setProximaEjecucion(CronExpression.parse(programacion.getExpresionCron()).next(ahora));
        programacion.setUltimaEjecucion(ahora);
        programacion.setActualizadoEn(ahora);
        programacionRepository.save(programacion);

        trabajoAsincronoServicio.ejecutar(respaldo.getIdRespaldo());
    }

    private Respaldo crearRespaldo(TipoRespaldo tipo, OrigenRespaldo origen,
                                    RespaldoProgramacion programacion, String correoSolicitante) {
        Respaldo.RespaldoBuilder builder = Respaldo.builder()
                .tipoRespaldo(tipo)
                .origen(origen)
                .programacion(programacion)
                .correoSolicitante(correoSolicitante)
                .fechaInicio(LocalDateTime.now());

        if (tipo == TipoRespaldo.INCREMENTAL) {
            Respaldo baseFull = respaldoRepository
                    .findTopByTipoRespaldoAndEstadoRespaldoOrderByFechaInicioDesc(TipoRespaldo.FULL, EstadoRespaldo.COMPLETADO)
                    .orElseThrow(() -> new IllegalStateException(
                            "No existe ningún respaldo FULL completado del que partir un INCREMENTAL"));

            // Corte = fecha del último respaldo de LA CADENA de ese FULL (el
            // incremental completado más reciente si existe, si no el FULL
            // mismo) -- no siempre "desde el FULL". Mantiene cada incremental
            // pequeño, el significado estándar de "incremental". Consecuencia:
            // restaurar a un punto en el tiempo requiere el FULL + TODA la
            // cadena de incrementales en orden (ver docs/despliegue/BACKUP.md).
            LocalDateTime corte = respaldoRepository
                    .findTopByIdRespaldoFullBaseOrderByFechaInicioDesc(baseFull.getIdRespaldo())
                    .map(Respaldo::getFechaInicio)
                    .orElse(baseFull.getFechaInicio());

            builder.idRespaldoFullBase(baseFull.getIdRespaldo()).fechaDesdeIncremental(corte);
        }

        return respaldoRepository.save(builder.build());
    }
}
