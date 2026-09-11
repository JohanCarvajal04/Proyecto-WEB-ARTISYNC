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
import uteq.edu.ec.artisync.dto.peticion.respaldo.FiltroRespaldo;
import uteq.edu.ec.artisync.dto.peticion.respaldo.PeticionActualizarProgramacion;
import uteq.edu.ec.artisync.dto.peticion.respaldo.PeticionCambiarEstadoProgramacion;
import uteq.edu.ec.artisync.dto.peticion.respaldo.PeticionCrearProgramacion;
import uteq.edu.ec.artisync.dto.peticion.respaldo.PeticionCrearRespaldo;
import uteq.edu.ec.artisync.dto.respuesta.respaldo.RespuestaProgramacion;
import uteq.edu.ec.artisync.dto.respuesta.respaldo.RespuestaRespaldo;
import uteq.edu.ec.artisync.entity.respaldo.TipoRespaldo;
import uteq.edu.ec.artisync.service.respaldo.ArchivoRespaldo;
import uteq.edu.ec.artisync.service.respaldo.IRespaldoProgramacionServicio;
import uteq.edu.ec.artisync.service.respaldo.IRespaldoServicio;
import uteq.edu.ec.artisync.util.PagedResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RespaldoControladorTest {

    @Mock
    private IRespaldoServicio respaldoServicio;

    @Mock
    private IRespaldoProgramacionServicio programacionServicio;

    @InjectMocks
    private RespaldoControlador controlador;

    @Test
    void crear_DebeRetornarAceptadoConCorreoDelAutenticado() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getName()).thenReturn("admin@artisync.dev");
        PeticionCrearRespaldo peticion = new PeticionCrearRespaldo();
        peticion.setTipoRespaldo(TipoRespaldo.FULL);
        RespuestaRespaldo respuesta = RespuestaRespaldo.builder().idRespaldo(1L).build();
        when(respaldoServicio.solicitarRespaldo(TipoRespaldo.FULL, "admin@artisync.dev")).thenReturn(respuesta);

        ResponseEntity<RespuestaRespaldo> result = controlador.crear(peticion, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(result.getBody().getIdRespaldo()).isEqualTo(1L);
    }

    @Test
    void listar_DebeRetornarPaginaDelServicio() {
        FiltroRespaldo filtro = new FiltroRespaldo();
        Pageable pageable = PageRequest.of(0, 10);
        PagedResponse<RespuestaRespaldo> pagina = PagedResponse.<RespuestaRespaldo>builder()
                .content(List.of(RespuestaRespaldo.builder().idRespaldo(1L).build()))
                .totalElements(1)
                .build();
        when(respaldoServicio.listar(filtro, pageable)).thenReturn(pagina);

        ResponseEntity<PagedResponse<RespuestaRespaldo>> result = controlador.listar(filtro, pageable);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getContent()).hasSize(1);
    }

    @Test
    void obtenerPorId_DebeRetornarElRespaldo() {
        when(respaldoServicio.obtenerPorId(5L)).thenReturn(RespuestaRespaldo.builder().idRespaldo(5L).build());

        ResponseEntity<RespuestaRespaldo> result = controlador.obtenerPorId(5L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getIdRespaldo()).isEqualTo(5L);
    }

    @Test
    void descargar_DebeExponerHeadersDeContenido() {
        ArchivoRespaldo archivo = new ArchivoRespaldo(new ByteArrayResource("contenido".getBytes()), "backup.sql", 9L);
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
        PeticionCrearProgramacion peticion = new PeticionCrearProgramacion();
        RespuestaProgramacion respuesta = RespuestaProgramacion.builder().idProgramacion(1L).build();
        when(programacionServicio.crear(eq(peticion), eq("admin@artisync.dev"))).thenReturn(respuesta);

        ResponseEntity<RespuestaProgramacion> result = controlador.crearProgramacion(peticion, authentication);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getIdProgramacion()).isEqualTo(1L);
    }

    @Test
    void listarProgramaciones_DebeRetornarListaDelServicio() {
        when(programacionServicio.listar()).thenReturn(List.of(RespuestaProgramacion.builder().idProgramacion(1L).build()));

        ResponseEntity<List<RespuestaProgramacion>> result = controlador.listarProgramaciones();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
    }

    @Test
    void obtenerProgramacion_DebeRetornarLaProgramacion() {
        when(programacionServicio.obtenerPorId(4L)).thenReturn(RespuestaProgramacion.builder().idProgramacion(4L).build());

        ResponseEntity<RespuestaProgramacion> result = controlador.obtenerProgramacion(4L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getIdProgramacion()).isEqualTo(4L);
    }

    @Test
    void actualizarProgramacion_DebeRetornarLaProgramacionActualizada() {
        PeticionActualizarProgramacion peticion = new PeticionActualizarProgramacion();
        when(programacionServicio.actualizar(eq(6L), eq(peticion)))
                .thenReturn(RespuestaProgramacion.builder().idProgramacion(6L).build());

        ResponseEntity<RespuestaProgramacion> result = controlador.actualizarProgramacion(6L, peticion);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getIdProgramacion()).isEqualTo(6L);
    }

    @Test
    void cambiarEstadoProgramacion_DebeDelegarElBooleanoAlServicio() {
        PeticionCambiarEstadoProgramacion peticion = new PeticionCambiarEstadoProgramacion();
        peticion.setActivo(Boolean.FALSE);
        when(programacionServicio.cambiarEstado(7L, false))
                .thenReturn(RespuestaProgramacion.builder().idProgramacion(7L).activo(false).build());

        ResponseEntity<RespuestaProgramacion> result = controlador.cambiarEstadoProgramacion(7L, peticion);

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
