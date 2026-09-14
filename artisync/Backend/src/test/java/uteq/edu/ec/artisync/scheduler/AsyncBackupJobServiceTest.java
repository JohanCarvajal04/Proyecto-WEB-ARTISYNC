package uteq.edu.ec.artisync.scheduler;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.entity.backup.Backup;
import uteq.edu.ec.artisync.entity.backup.BackupStatus;
import uteq.edu.ec.artisync.entity.backup.BackupType;
import uteq.edu.ec.artisync.repository.backup.BackupRepository;
import uteq.edu.ec.artisync.service.backup.impl.IncrementalBackupExporter;
import uteq.edu.ec.artisync.service.backup.impl.PgDumpExecutor;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AsyncBackupJobServiceTest {

    @Mock private BackupRepository respaldoRepository;
    @Mock private PgDumpExecutor pgDumpEjecutor;
    @Mock private IncrementalBackupExporter incrementalExportador;

    @InjectMocks
    private AsyncBackupJobService servicio;

    @Test
    void backupInexistente_noHaceNadaMasQueLoguear() {
        given(respaldoRepository.findById(1L)).willReturn(Optional.empty());

        servicio.execute(1L);

        verifyNoInteractions(pgDumpEjecutor, incrementalExportador);
        verify(respaldoRepository, never()).save(any());
    }

    @Test
    void backupFull_usaPgDumpExecutorYQuedaCompletado(@org.junit.jupiter.api.io.TempDir Path tmp) throws Exception {
        Backup respaldo = Backup.builder().idRespaldo(1L).tipoRespaldo(BackupType.FULL).build();
        given(respaldoRepository.findById(1L)).willReturn(Optional.of(respaldo));
        Path archivo = tmp.resolve("respaldo-full.dump");
        Files.writeString(archivo, "contenido de prueba");
        given(pgDumpEjecutor.execute(respaldo)).willReturn(archivo);

        servicio.execute(1L);

        assertThat(respaldo.getEstadoRespaldo()).isEqualTo(BackupStatus.COMPLETADO);
        assertThat(respaldo.getNombreArchivo()).isEqualTo(archivo.getFileName().toString());
        assertThat(respaldo.getTamanoBytes()).isEqualTo(Files.size(archivo));
        verifyNoInteractions(incrementalExportador);
        verify(respaldoRepository).save(respaldo);
    }

    @Test
    void backupIncremental_usaIncrementalBackupExporter(@org.junit.jupiter.api.io.TempDir Path tmp) throws Exception {
        Backup respaldo = Backup.builder().idRespaldo(2L).tipoRespaldo(BackupType.INCREMENTAL).build();
        given(respaldoRepository.findById(2L)).willReturn(Optional.of(respaldo));
        Path archivo = tmp.resolve("respaldo-incremental.dump");
        Files.writeString(archivo, "contenido incremental");
        given(incrementalExportador.execute(respaldo)).willReturn(archivo);

        servicio.execute(2L);

        assertThat(respaldo.getEstadoRespaldo()).isEqualTo(BackupStatus.COMPLETADO);
        verifyNoInteractions(pgDumpEjecutor);
        verify(respaldoRepository).save(respaldo);
    }

    @Test
    void fallaElVolcado_marcaFallidoConMensajeTruncado() throws Exception {
        Backup respaldo = Backup.builder().idRespaldo(3L).tipoRespaldo(BackupType.FULL).build();
        given(respaldoRepository.findById(3L)).willReturn(Optional.of(respaldo));
        given(pgDumpEjecutor.execute(respaldo)).willThrow(new RuntimeException("disco lleno"));

        servicio.execute(3L);

        assertThat(respaldo.getEstadoRespaldo()).isEqualTo(BackupStatus.FALLIDO);
        assertThat(respaldo.getMensajeError()).isEqualTo("disco lleno");
        assertThat(respaldo.getFechaFin()).isNotNull();
        verify(respaldoRepository).save(respaldo);
    }
}
