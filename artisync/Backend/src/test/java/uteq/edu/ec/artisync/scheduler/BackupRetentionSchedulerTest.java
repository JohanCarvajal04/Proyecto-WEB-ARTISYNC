package uteq.edu.ec.artisync.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.repository.CrudRepository;
import uteq.edu.ec.artisync.config.BackupProperties;
import uteq.edu.ec.artisync.entity.backup.Backup;
import uteq.edu.ec.artisync.entity.backup.BackupSchedule;
import uteq.edu.ec.artisync.entity.backup.BackupStatus;
import uteq.edu.ec.artisync.entity.backup.BackupType;
import uteq.edu.ec.artisync.repository.backup.BackupRepository;
import uteq.edu.ec.artisync.service.backup.impl.BackupFileStorage;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BackupRetentionSchedulerTest {

    @Mock private BackupRepository respaldoRepository;
    @Mock private BackupProperties respaldoProperties;
    @Mock private BackupFileStorage storage;

    @InjectMocks
    private BackupRetentionScheduler scheduler;

    @Test
    void respaldoIncremental_siempreEsSeguroDeBorrar() {
        Backup incremental = Backup.builder().idRespaldo(1L).tipoRespaldo(BackupType.INCREMENTAL).build();

        assertThat(scheduler.isSafeToDelete(incremental)).isTrue();
    }

    @Test
    void respaldoFull_conIncrementalesVivos_noEsSeguroDeBorrar() {
        Backup full = Backup.builder().idRespaldo(2L).tipoRespaldo(BackupType.FULL).build();
        given(respaldoRepository.existsByIdRespaldoFullBaseAndEstadoRespaldo(2L, BackupStatus.COMPLETADO))
                .willReturn(true);

        assertThat(scheduler.isSafeToDelete(full)).isFalse();
    }

    @Test
    void respaldoFull_sinIncrementalesVivos_esSeguroDeBorrar() {
        Backup full = Backup.builder().idRespaldo(3L).tipoRespaldo(BackupType.FULL).build();
        given(respaldoRepository.existsByIdRespaldoFullBaseAndEstadoRespaldo(3L, BackupStatus.COMPLETADO))
                .willReturn(false);

        assertThat(scheduler.isSafeToDelete(full)).isTrue();
    }

    @Test
    void purgarRespaldosVencidos_eliminaArchivoYRegistroDelVencidoYSeguro() throws Exception {
        Backup vencido = Backup.builder().idRespaldo(4L).tipoRespaldo(BackupType.INCREMENTAL)
                .fechaInicio(LocalDateTime.now().minusDays(400)).rutaArchivo("/respaldos/r4.dump").build();
        given(respaldoRepository.findByEstadoRespaldo(BackupStatus.COMPLETADO)).willReturn(List.of(vencido));
        given(respaldoProperties.getRetencionDiasManual()).willReturn(90);

        scheduler.purgarRespaldosVencidos();

        verify(storage).delete(eq(Path.of("/respaldos/r4.dump")));
        verify((CrudRepository<Backup, Long>) respaldoRepository).delete(vencido);
    }

    @Test
    void purgarRespaldosVencidos_noTocaElQueAunNoVence() {
        Backup reciente = Backup.builder().idRespaldo(5L).tipoRespaldo(BackupType.INCREMENTAL)
                .fechaInicio(LocalDateTime.now()).build();
        given(respaldoRepository.findByEstadoRespaldo(BackupStatus.COMPLETADO)).willReturn(List.of(reciente));
        given(respaldoProperties.getRetencionDiasManual()).willReturn(90);

        scheduler.purgarRespaldosVencidos();

        verify(respaldoRepository, never()).delete(any(Backup.class));
    }

    @Test
    void purgarRespaldosVencidos_usaLaRetencionDeLaProgramacionSiExiste() {
        BackupSchedule programacion = BackupSchedule.builder().idProgramacion(1L).retencionDias(5).build();
        Backup vencido = Backup.builder().idRespaldo(6L).tipoRespaldo(BackupType.INCREMENTAL)
                .programacion(programacion).fechaInicio(LocalDateTime.now().minusDays(10)).build();
        given(respaldoRepository.findByEstadoRespaldo(BackupStatus.COMPLETADO)).willReturn(List.of(vencido));

        scheduler.purgarRespaldosVencidos();

        verify((CrudRepository<Backup, Long>) respaldoRepository).delete(vencido);
    }
}
