package uteq.edu.ec.artisync.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.entity.backup.BackupSchedule;
import uteq.edu.ec.artisync.repository.backup.BackupScheduleRepository;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BackupSchedulerTest {

    @Mock private BackupScheduleRepository programacionRepository;
    @Mock private BackupExecutorService respaldoEjecutorServicio;

    @InjectMocks
    private BackupScheduler scheduler;

    @Test
    void sinProgramacionesVencidas_noDisparaNada() {
        given(programacionRepository.findByActivoTrueAndProximaEjecucionLessThanEqual(any()))
                .willReturn(List.of());

        scheduler.processPendingSchedules();

        verify(respaldoEjecutorServicio, never()).startFromSchedule(any());
    }

    @Test
    void conProgramacionesVencidas_disparaCadaUna() {
        BackupSchedule p1 = BackupSchedule.builder().idProgramacion(1L).build();
        BackupSchedule p2 = BackupSchedule.builder().idProgramacion(2L).build();
        given(programacionRepository.findByActivoTrueAndProximaEjecucionLessThanEqual(any()))
                .willReturn(List.of(p1, p2));

        scheduler.processPendingSchedules();

        verify(respaldoEjecutorServicio).startFromSchedule(p1);
        verify(respaldoEjecutorServicio).startFromSchedule(p2);
    }

    @Test
    void unaProgramacionFalla_lasSiguientesSeSiguenProcesando() {
        BackupSchedule conError = BackupSchedule.builder().idProgramacion(1L).build();
        BackupSchedule ok = BackupSchedule.builder().idProgramacion(2L).build();
        given(programacionRepository.findByActivoTrueAndProximaEjecucionLessThanEqual(any()))
                .willReturn(List.of(conError, ok));
        doThrow(new RuntimeException("fallo simulado")).when(respaldoEjecutorServicio).startFromSchedule(conError);

        scheduler.processPendingSchedules();

        verify(respaldoEjecutorServicio).startFromSchedule(ok);
    }
}
