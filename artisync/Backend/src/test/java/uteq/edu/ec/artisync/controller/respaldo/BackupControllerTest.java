package uteq.edu.ec.artisync.controller.respaldo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import uteq.edu.ec.artisync.dto.peticion.respaldo.BackupFilter;
import uteq.edu.ec.artisync.dto.peticion.respaldo.UpdateScheduleRequest;
import uteq.edu.ec.artisync.dto.peticion.respaldo.ChangeScheduleStatusRequest;
import uteq.edu.ec.artisync.dto.peticion.respaldo.CreateScheduleRequest;
import uteq.edu.ec.artisync.dto.peticion.respaldo.CreateBackupRequest;
import uteq.edu.ec.artisync.dto.respuesta.respaldo.ScheduleResponse;
import uteq.edu.ec.artisync.dto.respuesta.respaldo.BackupResponse;
import uteq.edu.ec.artisync.entity.respaldo.BackupType;
import uteq.edu.ec.artisync.service.respaldo.BackupFile;
import uteq.edu.ec.artisync.service.respaldo.IBackupScheduleService;
import uteq.edu.ec.artisync.service.respaldo.IBackupService;
import uteq.edu.ec.artisync.util.PagedResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BackupControllerTest {

    @Mock
    private IBackupService respaldoServicio;

    @Mock
    private IBackupScheduleService programacionServicio;

    @InjectMocks
    private BackupController controlador;

    @Test
    void crear_DebeRetornarAceptadoConCorreoDelAutenticado() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("admin@artisync.dev");
        CreateBackupRequest peticion = new CreateBackupRequest();
        peticion.setTipoRespaldo(BackupType.FULL);
        BackupResponse respuesta = BackupResponse.builder().idRespaldo(1L).build();
        when(respaldoServicio.solicitarRespaldo(BackupType.FULL, "admin@artisync.dev")).thenReturn(respuesta);

        ResponseEntity<BackupResponse> result = controlador.crear(peticion, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(result.getBody().getIdRespaldo()).isEqualTo(1L);
    }

    @Test
    void listar_DebeRetornarPaginaDelServicio() {
        BackupFilter filtro = new BackupFilter();
        Pageable pageable = PageRequest.of(0, 10);
        PagedResponse<BackupResponse> pagina = PagedResponse.<BackupResponse>builder()
                .content(List.of(BackupResponse.builder().idRespaldo(1L).build()))
                .totalElements(1)
                .build();
        when(respaldoServicio.listar(filtro, pageable)).thenReturn(pagina);

        ResponseEntity<PagedResponse<BackupResponse>> result = controlador.listar(filtro, pageable);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getContent()).hasSize(1);
    }

    @Test
    void obtenerPorId_DebeRetornarElRespaldo() {
        when(respaldoServicio.obtenerPorId(5L)).thenReturn(BackupResponse.builder().idRespaldo(5L).build());

        ResponseEntity<BackupResponse> result = controlador.obtenerPorId(5L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getIdRespaldo()).isEqualTo(5L);
    }

    @Test
    void descargar_DebeExponerHeadersDeContenido() {
        BackupFile archivo = new BackupFile(new ByteArrayResource("contenido".getBytes()), "backup.sql", 9L);
        when(respaldoServicio.descargar(3L)).thenReturn(archivo);

        ResponseEntity<org.springframework.core.io.Resource> result = controlador.descargar(3L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getHeaders().getContentLength()).isEqualTo(9L);
        assertThat(result.getHeaders().getContentDisposition().getFilename()).isEqualTo("backup.sql");
    }

    @Test
    void eliminar_DebeRetornarSinContenido() {
        ResponseEntity<Void> result = controlador.eliminar(2L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(respaldoServicio).eliminar(2L);
    }

    @Test
    void crearProgramacion_DebeRetornarCreadaConCreadorDelAutenticado() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("admin@artisync.dev");
        CreateScheduleRequest peticion = new CreateScheduleRequest();
        ScheduleResponse respuesta = ScheduleResponse.builder().idProgramacion(1L).build();
        when(programacionServicio.crear(eq(peticion), eq("admin@artisync.dev"))).thenReturn(respuesta);

        ResponseEntity<ScheduleResponse> result = controlador.crearProgramacion(peticion, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getIdProgramacion()).isEqualTo(1L);
    }

    @Test
    void listarProgramaciones_DebeRetornarListaDelServicio() {
        when(programacionServicio.listar()).thenReturn(List.of(ScheduleResponse.builder().idProgramacion(1L).build()));

        ResponseEntity<List<ScheduleResponse>> result = controlador.listarProgramaciones();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
    }

    @Test
    void obtenerProgramacion_DebeRetornarLaProgramacion() {
        when(programacionServicio.obtenerPorId(4L)).thenReturn(ScheduleResponse.builder().idProgramacion(4L).build());

        ResponseEntity<ScheduleResponse> result = controlador.obtenerProgramacion(4L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getIdProgramacion()).isEqualTo(4L);
    }

    @Test
    void actualizarProgramacion_DebeRetornarLaProgramacionActualizada() {
        UpdateScheduleRequest peticion = new UpdateScheduleRequest();
        when(programacionServicio.actualizar(eq(6L), eq(peticion)))
                .thenReturn(ScheduleResponse.builder().idProgramacion(6L).build());

        ResponseEntity<ScheduleResponse> result = controlador.actualizarProgramacion(6L, peticion);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getIdProgramacion()).isEqualTo(6L);
    }

    @Test
    void cambiarEstadoProgramacion_DebeDelegarElBooleanoAlServicio() {
        ChangeScheduleStatusRequest peticion = new ChangeScheduleStatusRequest();
        peticion.setActivo(Boolean.FALSE);
        when(programacionServicio.cambiarEstado(7L, false))
                .thenReturn(ScheduleResponse.builder().idProgramacion(7L).activo(false).build());

        ResponseEntity<ScheduleResponse> result = controlador.cambiarEstadoProgramacion(7L, peticion);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getActivo()).isFalse();
    }

    @Test
    void eliminarProgramacion_DebeRetornarSinContenido() {
        ResponseEntity<Void> result = controlador.eliminarProgramacion(8L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(programacionServicio).eliminar(8L);
    }
}
