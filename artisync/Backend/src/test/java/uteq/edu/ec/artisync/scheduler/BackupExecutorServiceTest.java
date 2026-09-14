package uteq.edu.ec.artisync.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.entity.backup.Backup;
import uteq.edu.ec.artisync.entity.backup.BackupSchedule;
import uteq.edu.ec.artisync.entity.backup.BackupStatus;
import uteq.edu.ec.artisync.entity.backup.BackupType;
import uteq.edu.ec.artisync.repository.backup.BackupRepository;
import uteq.edu.ec.artisync.repository.backup.BackupScheduleRepository;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BackupExecutorServiceTest {

    @Mock private BackupRepository respaldoRepository;
    @Mock private BackupScheduleRepository programacionRepository;
    @Mock private AsyncBackupJobService trabajoAsincronoServicio;

    @InjectMocks
    private BackupExecutorService ejecutor;

    @Test
    void startManual_full_creaRespaldoYDisparaElJobAsincrono() {
        given(respaldoRepository.save(any())).willAnswer(invocacion -> {
            Backup b = invocacion.getArgument(0);
            b.setIdRespaldo(1L);
            return b;
        });

        Backup respaldo = ejecutor.startManual(BackupType.FULL, "admin@artisync.com");

        assertThat(respaldo.getIdRespaldo()).isEqualTo(1L);
        assertThat(respaldo.getCorreoSolicitante()).isEqualTo("admin@artisync.com");
        verify(trabajoAsincronoServicio).execute(1L);
    }

    @Test
    void startManual_incremental_sinFullPrevio_lanzaExcepcion() {
        given(respaldoRepository.findTopByTipoRespaldoAndEstadoRespaldoOrderByFechaInicioDesc(
                BackupType.FULL, BackupStatus.COMPLETADO)).willReturn(Optional.empty());

        assertThatThrownBy(() -> ejecutor.startManual(BackupType.INCREMENTAL, "admin@artisync.com"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("FULL completado");
    }

    @Test
    void startManual_incremental_conFullPrevio_calculaElCorteDesdeElUltimoIncremental() {
        Backup full = Backup.builder().idRespaldo(10L).fechaInicio(LocalDateTime.now().minusDays(5)).build();
        Backup ultimoIncremental = Backup.builder().idRespaldo(11L).fechaInicio(LocalDateTime.now().minusDays(1)).build();
        given(respaldoRepository.findTopByTipoRespaldoAndEstadoRespaldoOrderByFechaInicioDesc(
                BackupType.FULL, BackupStatus.COMPLETADO)).willReturn(Optional.of(full));
        given(respaldoRepository.findTopByIdRespaldoFullBaseOrderByFechaInicioDesc(10L))
                .willReturn(Optional.of(ultimoIncremental));
        ArgumentCaptor<Backup> captor = ArgumentCaptor.forClass(Backup.class);
        given(respaldoRepository.save(captor.capture())).willAnswer(invocacion -> invocacion.getArgument(0));

        ejecutor.startManual(BackupType.INCREMENTAL, "admin@artisync.com");

        assertThat(captor.getValue().getIdRespaldoFullBase()).isEqualTo(10L);
        assertThat(captor.getValue().getFechaDesdeIncremental()).isEqualTo(ultimoIncremental.getFechaInicio());
    }

    @Test
    void startFromSchedule_recalculaProximaEjecucionYDisparaElJob() {
        BackupSchedule programacion = BackupSchedule.builder()
                .idProgramacion(5L).tipoRespaldo(BackupType.FULL).expresionCron("0 0 3 * * *").build();
        given(respaldoRepository.save(any())).willAnswer(invocacion -> {
            Backup b = invocacion.getArgument(0);
            b.setIdRespaldo(20L);
            return b;
        });

        ejecutor.startFromSchedule(programacion);

        assertThat(programacion.getProximaEjecucion()).isNotNull();
        assertThat(programacion.getUltimaEjecucion()).isNotNull();
        verify(programacionRepository).save(programacion);
        verify(trabajoAsincronoServicio).execute(20L);
    }
}
