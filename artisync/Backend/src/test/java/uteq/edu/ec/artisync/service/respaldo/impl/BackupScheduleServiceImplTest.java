package uteq.edu.ec.artisync.service.respaldo.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.dto.peticion.respaldo.CreateScheduleRequest;
import uteq.edu.ec.artisync.dto.peticion.respaldo.UpdateScheduleRequest;
import uteq.edu.ec.artisync.dto.respuesta.respaldo.ScheduleResponse;
import uteq.edu.ec.artisync.entity.respaldo.BackupSchedule;
import uteq.edu.ec.artisync.entity.respaldo.BackupType;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.repository.respaldo.BackupScheduleRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Prueba de caracterización del comportamiento actual de BackupScheduleServiceImpl,
 * escrita ANTES de renombrar sus métodos en español (create/update/changeStatus/
 * delete/list/getById), ya que la clase estaba en 0% de cobertura real. Mismo
 * estilo de mocks que BackupServiceImplTest.
 */
@ExtendWith(MockitoExtension.class)
class BackupScheduleServiceImplTest {

    @Mock
    private BackupScheduleRepository programacionRepository;

    @InjectMocks
    private BackupScheduleServiceImpl servicio;

    private static final String CRON_CADA_MINUTO = "0 * * * * *";

    private BackupSchedule programacionBase(Long id) {
        BackupSchedule programacion = new BackupSchedule();
        programacion.setIdProgramacion(id);
        programacion.setNombre("Respaldo diario");
        programacion.setTipoRespaldo(BackupType.FULL);
        programacion.setExpresionCron(CRON_CADA_MINUTO);
        programacion.setRetencionDias(30);
        programacion.setActivo(true);
        return programacion;
    }

    @Test
    void crear_ConCronValido_DebeCalcularProximaEjecucionYGuardar() {
        CreateScheduleRequest peticion = new CreateScheduleRequest();
        peticion.setNombre("Respaldo diario");
        peticion.setTipoRespaldo(BackupType.FULL);
        peticion.setExpresionCron(CRON_CADA_MINUTO);
        peticion.setRetencionDias(30);

        ArgumentCaptor<BackupSchedule> captor = ArgumentCaptor.forClass(BackupSchedule.class);
        when(programacionRepository.save(captor.capture())).thenAnswer(inv -> {
            BackupSchedule guardada = captor.getValue();
            guardada.setIdProgramacion(1L);
            return guardada;
        });

        ScheduleResponse resultado = servicio.create(peticion, "admin@artisync.dev");

        assertThat(resultado.getIdProgramacion()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Respaldo diario");
        assertThat(resultado.getActivo()).isTrue();
        assertThat(resultado.getCreadoPor()).isEqualTo("admin@artisync.dev");
        assertThat(resultado.getProximaEjecucion()).isNotNull();
        assertThat(resultado.getProximaEjecucion()).isAfter(LocalDateTime.now());
    }

    @Test
    void crear_ConCronInvalido_DebeRechazar() {
        CreateScheduleRequest peticion = new CreateScheduleRequest();
        peticion.setNombre("Respaldo diario");
        peticion.setTipoRespaldo(BackupType.FULL);
        peticion.setExpresionCron("no-es-un-cron-valido");
        peticion.setRetencionDias(30);

        assertThatThrownBy(() -> servicio.create(peticion, "admin@artisync.dev"))
                .isInstanceOf(BusinessRuleException.class);
        verify(programacionRepository, never()).save(any());
    }

    @Test
    void crear_ConCronSinEjecucionFutura_DebeRechazar() {
        // "0 0 0 31 2 *": el 31 de febrero nunca existe -> CronExpression.next() devuelve null.
        CreateScheduleRequest peticion = new CreateScheduleRequest();
        peticion.setNombre("Respaldo imposible");
        peticion.setTipoRespaldo(BackupType.FULL);
        peticion.setExpresionCron("0 0 0 31 2 *");
        peticion.setRetencionDias(30);

        assertThatThrownBy(() -> servicio.create(peticion, "admin@artisync.dev"))
                .isInstanceOf(BusinessRuleException.class);
        verify(programacionRepository, never()).save(any());
    }

    @Test
    void actualizar_Existente_DebeActualizarCamposYRecalcularProximaEjecucion() {
        BackupSchedule existente = programacionBase(1L);
        when(programacionRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(programacionRepository.save(any(BackupSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateScheduleRequest peticion = new UpdateScheduleRequest();
        peticion.setNombre("Respaldo semanal");
        peticion.setTipoRespaldo(BackupType.INCREMENTAL);
        peticion.setExpresionCron(CRON_CADA_MINUTO);
        peticion.setRetencionDias(7);

        ScheduleResponse resultado = servicio.update(1L, peticion);

        assertThat(resultado.getNombre()).isEqualTo("Respaldo semanal");
        assertThat(resultado.getTipoRespaldo()).isEqualTo(BackupType.INCREMENTAL);
        assertThat(resultado.getRetencionDias()).isEqualTo(7);
        assertThat(resultado.getProximaEjecucion()).isNotNull();
    }

    @Test
    void actualizar_Inexistente_DebeLanzarNoEncontrado() {
        when(programacionRepository.findById(99L)).thenReturn(Optional.empty());

        UpdateScheduleRequest peticion = new UpdateScheduleRequest();
        peticion.setNombre("x");
        peticion.setTipoRespaldo(BackupType.FULL);
        peticion.setExpresionCron(CRON_CADA_MINUTO);
        peticion.setRetencionDias(1);

        assertThatThrownBy(() -> servicio.update(99L, peticion))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(programacionRepository, never()).save(any());
    }

    @Test
    void cambiarEstado_Existente_DebeCambiarElFlagActivo() {
        BackupSchedule existente = programacionBase(1L);
        existente.setActivo(true);
        when(programacionRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(programacionRepository.save(any(BackupSchedule.class))).thenAnswer(inv -> inv.getArgument(0));

        ScheduleResponse resultado = servicio.changeStatus(1L, false);

        assertThat(resultado.getActivo()).isFalse();
    }

    @Test
    void cambiarEstado_Inexistente_DebeLanzarNoEncontrado() {
        when(programacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.changeStatus(99L, true))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(programacionRepository, never()).save(any());
    }

    @Test
    void eliminar_Existente_DebeBorrarla() {
        BackupSchedule existente = programacionBase(1L);
        when(programacionRepository.findById(1L)).thenReturn(Optional.of(existente));

        servicio.delete(1L);

        verify(programacionRepository, times(1)).delete(existente);
    }

    @Test
    void eliminar_Inexistente_DebeLanzarNoEncontrado() {
        when(programacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(programacionRepository, never()).delete(any(BackupSchedule.class));
    }

    @Test
    void listar_DebeMapearTodasLasProgramaciones() {
        when(programacionRepository.findAll()).thenReturn(List.of(programacionBase(1L), programacionBase(2L)));

        List<ScheduleResponse> resultado = servicio.list();

        assertThat(resultado).hasSize(2);
        assertThat(resultado.get(0).getIdProgramacion()).isEqualTo(1L);
        assertThat(resultado.get(1).getIdProgramacion()).isEqualTo(2L);
    }

    @Test
    void obtenerPorId_Existente_DebeRetornarla() {
        when(programacionRepository.findById(1L)).thenReturn(Optional.of(programacionBase(1L)));

        ScheduleResponse resultado = servicio.getById(1L);

        assertThat(resultado.getIdProgramacion()).isEqualTo(1L);
        assertThat(resultado.getNombre()).isEqualTo("Respaldo diario");
    }

    @Test
    void obtenerPorId_Inexistente_DebeLanzarNoEncontrado() {
        when(programacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
