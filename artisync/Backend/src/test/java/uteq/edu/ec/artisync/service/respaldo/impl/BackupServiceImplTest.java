package uteq.edu.ec.artisync.service.respaldo.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import uteq.edu.ec.artisync.dto.peticion.respaldo.BackupFilter;
import uteq.edu.ec.artisync.dto.respuesta.respaldo.BackupResponse;
import uteq.edu.ec.artisync.entity.respaldo.BackupStatus;
import uteq.edu.ec.artisync.entity.respaldo.BackupOrigin;
import uteq.edu.ec.artisync.entity.respaldo.Backup;
import uteq.edu.ec.artisync.entity.respaldo.BackupSchedule;
import uteq.edu.ec.artisync.entity.respaldo.BackupType;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.respaldo.BackupRepository;
import uteq.edu.ec.artisync.scheduler.BackupExecutorService;
import uteq.edu.ec.artisync.scheduler.BackupRetentionScheduler;
import uteq.edu.ec.artisync.service.respaldo.BackupFile;
import uteq.edu.ec.artisync.util.PagedResponse;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackupServiceImplTest {

    @Mock
    private BackupRepository respaldoRepository;

    @Mock
    private BackupExecutorService respaldoEjecutorServicio;

    @Mock
    private BackupRetentionScheduler retencionScheduler;

    @InjectMocks
    private BackupServiceImpl servicio;

    @TempDir
    Path tempDir;

    private Backup respaldoBase(Long id) {
        Backup respaldo = new Backup();
        respaldo.setIdRespaldo(id);
        respaldo.setTipoRespaldo(BackupType.FULL);
        respaldo.setEstadoRespaldo(BackupStatus.COMPLETADO);
        respaldo.setOrigen(BackupOrigin.MANUAL);
        respaldo.setCorreoSolicitante("admin@artisync.dev");
        return respaldo;
    }

    @Test
    void solicitarRespaldo_SinRespaldoEnProgreso_DebeIniciarUnoNuevo() {
        when(respaldoRepository.existsByEstadoRespaldo(BackupStatus.EN_PROGRESO)).thenReturn(false);
        Backup creado = respaldoBase(1L);
        when(respaldoEjecutorServicio.iniciarManual(BackupType.FULL, "admin@artisync.dev")).thenReturn(creado);

        BackupResponse resultado = servicio.requestBackup(BackupType.FULL, "admin@artisync.dev");

        assertThat(resultado.getIdRespaldo()).isEqualTo(1L);
    }

    @Test
    void solicitarRespaldo_ConRespaldoEnProgreso_DebeRechazar() {
        when(respaldoRepository.existsByEstadoRespaldo(BackupStatus.EN_PROGRESO)).thenReturn(true);

        assertThatThrownBy(() -> servicio.requestBackup(BackupType.FULL, "admin@artisync.dev"))
                .isInstanceOf(BusinessRuleException.class);
        verify(respaldoEjecutorServicio, never()).iniciarManual(any(), any());
    }

    @Test
    void listar_DebeMapearLaPaginaDelRepositorio() {
        BackupFilter filtro = new BackupFilter();
        Pageable pageable = PageRequest.of(0, 10);
        when(respaldoRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(respaldoBase(1L))));

        PagedResponse<BackupResponse> resultado = servicio.list(filtro, pageable);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getIdRespaldo()).isEqualTo(1L);
    }

    @Test
    void obtenerPorId_Existente_DebeRetornarlo() {
        when(respaldoRepository.findById(1L)).thenReturn(Optional.of(respaldoBase(1L)));

        BackupResponse resultado = servicio.getById(1L);

        assertThat(resultado.getIdRespaldo()).isEqualTo(1L);
    }

    @Test
    void obtenerPorId_Inexistente_DebeLanzarNoEncontrado() {
        when(respaldoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerPorId_ConProgramacionAsociada_DebeExponerSuId() {
        Backup respaldo = respaldoBase(1L);
        BackupSchedule programacion = new BackupSchedule();
        programacion.setIdProgramacion(7L);
        respaldo.setProgramacion(programacion);
        when(respaldoRepository.findById(1L)).thenReturn(Optional.of(respaldo));

        BackupResponse resultado = servicio.getById(1L);

        assertThat(resultado.getIdProgramacion()).isEqualTo(7L);
    }

    @Test
    void descargar_SinRutaDeArchivo_DebeLanzarNoEncontrado() {
        Backup respaldo = respaldoBase(1L);
        respaldo.setRutaArchivo(null);
        when(respaldoRepository.findById(1L)).thenReturn(Optional.of(respaldo));

        assertThatThrownBy(() -> servicio.download(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void descargar_ConRutaPeroArchivoInexistenteEnDisco_DebeLanzarNoEncontrado() {
        Backup respaldo = respaldoBase(1L);
        respaldo.setRutaArchivo(tempDir.resolve("no-existe.sql").toString());
        when(respaldoRepository.findById(1L)).thenReturn(Optional.of(respaldo));

        assertThatThrownBy(() -> servicio.download(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void descargar_ConArchivoReal_DebeRetornarloConSuTamano() throws IOException {
        Path archivo = tempDir.resolve("backup.sql");
        Files.writeString(archivo, "contenido de prueba");
        Backup respaldo = respaldoBase(1L);
        respaldo.setRutaArchivo(archivo.toString());
        respaldo.setNombreArchivo("backup.sql");
        when(respaldoRepository.findById(1L)).thenReturn(Optional.of(respaldo));

        BackupFile resultado = servicio.download(1L);

        assertThat(resultado.nombreArchivo()).isEqualTo("backup.sql");
        assertThat(resultado.tamanoBytes()).isEqualTo(Files.size(archivo));
    }

    @Test
    void eliminar_RespaldoEnProgreso_DebeRechazar() {
        Backup respaldo = respaldoBase(1L);
        respaldo.setEstadoRespaldo(BackupStatus.EN_PROGRESO);
        when(respaldoRepository.findById(1L)).thenReturn(Optional.of(respaldo));

        assertThatThrownBy(() -> servicio.delete(1L))
                .isInstanceOf(BusinessRuleException.class);
        verify(respaldoRepository, never()).delete(any(Backup.class));
    }

    @Test
    void eliminar_ConIncrementalesDependientes_DebeRechazar() {
        Backup respaldo = respaldoBase(1L);
        when(respaldoRepository.findById(1L)).thenReturn(Optional.of(respaldo));
        when(retencionScheduler.esSeguroEliminar(respaldo)).thenReturn(false);

        assertThatThrownBy(() -> servicio.delete(1L))
                .isInstanceOf(BusinessRuleException.class);
        verify(respaldoRepository, never()).delete(any(Backup.class));
    }

    @Test
    void eliminar_SinArchivoEnDisco_DebeBorrarSoloElRegistro() {
        Backup respaldo = respaldoBase(1L);
        respaldo.setRutaArchivo(null);
        when(respaldoRepository.findById(1L)).thenReturn(Optional.of(respaldo));
        when(retencionScheduler.esSeguroEliminar(respaldo)).thenReturn(true);

        servicio.delete(1L);

        verify(respaldoRepository, times(1)).delete(respaldo);
    }

    @Test
    void eliminar_ConArchivoReal_DebeBorrarloYElRegistro() throws IOException {
        Path archivo = tempDir.resolve("borrar.sql");
        Files.writeString(archivo, "x");
        Backup respaldo = respaldoBase(1L);
        respaldo.setRutaArchivo(archivo.toString());
        when(respaldoRepository.findById(1L)).thenReturn(Optional.of(respaldo));
        when(retencionScheduler.esSeguroEliminar(respaldo)).thenReturn(true);

        servicio.delete(1L);

        assertThat(Files.exists(archivo)).isFalse();
        verify(respaldoRepository).delete(respaldo);
    }
}
