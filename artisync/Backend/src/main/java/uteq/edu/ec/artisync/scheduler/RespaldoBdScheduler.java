package uteq.edu.ec.artisync.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;
import uteq.edu.ec.artisync.entity.sistema.RespaldoBd;
import uteq.edu.ec.artisync.entity.sistema.RespaldoPolitica;
import uteq.edu.ec.artisync.service.sistema.IRespaldoBdServicio;

import java.time.Instant;

@Configuration
@EnableScheduling
@RequiredArgsConstructor
@Slf4j
public class RespaldoBdScheduler implements SchedulingConfigurer {

    /** Crons de reserva si la política guardada resulta inválida en tiempo de
     *  ejecución, uno por categoría. */
    private static final String CRON_FULL_POR_DEFECTO = "0 0 3 * * SUN";
    private static final String CRON_DIARIO_POR_DEFECTO = "0 0 2 * * ?";

    private final IRespaldoBdServicio respaldoServicio;

    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {

        taskRegistrar.addTriggerTask(
            () -> ejecutarConLog("FULL", RespaldoBd.CategoriaRespaldo.FULL),
            ctx -> calcularSiguienteEjecucion(ctx, RespaldoBd.CategoriaRespaldo.FULL)
        );

        taskRegistrar.addTriggerTask(
            () -> ejecutarConLog("DIARIO", RespaldoBd.CategoriaRespaldo.DIARIO),
            ctx -> calcularSiguienteEjecucion(ctx, RespaldoBd.CategoriaRespaldo.DIARIO)
        );

        taskRegistrar.addCronTask(() -> {
            log.info("[RESPALDO] Purgando respaldos expirados");
            try {
                respaldoServicio.purgarExpirados();
            } catch (Exception e) {
                log.error("[RESPALDO] Error inesperado purgando respaldos expirados", e);
            }
        }, "0 30 2 * * ?");
    }

    private void ejecutarConLog(String etiqueta, RespaldoBd.CategoriaRespaldo categoria) {
        log.info("[RESPALDO] Iniciando respaldo automático {} programado", etiqueta);
        try {
            respaldoServicio.ejecutarRespaldoAutomatico(categoria);
            log.info("[RESPALDO] Respaldo automático {} completado", etiqueta);
        } catch (Exception e) {
            log.error("[RESPALDO] Error inesperado ejecutando el respaldo automático {}", etiqueta, e);
        }
    }

    private Instant calcularSiguienteEjecucion(TriggerContext triggerContext, RespaldoBd.CategoriaRespaldo categoria) {
        String cronDefault = categoria == RespaldoBd.CategoriaRespaldo.FULL ? CRON_FULL_POR_DEFECTO : CRON_DIARIO_POR_DEFECTO;

        String cron;
        try {
            RespaldoPolitica pol = respaldoServicio.obtenerPolitica();
            cron = categoria == RespaldoBd.CategoriaRespaldo.FULL ? pol.getCronFull() : pol.getCronDiario();
        } catch (Exception e) {
            log.error("[RESPALDO] No se pudo leer la política de respaldos ({}), usando cron por defecto '{}'", categoria, cronDefault, e);
            cron = cronDefault;
        }

        Trigger trigger;
        try {
            trigger = new CronTrigger(cron);
        } catch (IllegalArgumentException e) {
            log.error("[RESPALDO] Cron {} de política inválido ('{}'), usando cron por defecto '{}'. Corrija la política desde el panel de administración.", categoria, cron, cronDefault, e);
            trigger = new CronTrigger(cronDefault);
        }

        Instant next = trigger.nextExecution(triggerContext);
        return next != null ? next : new CronTrigger(cronDefault).nextExecution(triggerContext);
    }
}
