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
import uteq.edu.ec.artisync.dto.peticion.respaldo.FiltroRespaldo;
import uteq.edu.ec.artisync.dto.respuesta.respaldo.RespuestaRespaldo;
import uteq.edu.ec.artisync.entity.respaldo.EstadoRespaldo;
import uteq.edu.ec.artisync.entity.respaldo.OrigenRespaldo;
import uteq.edu.ec.artisync.entity.respaldo.Respaldo;
import uteq.edu.ec.artisync.entity.respaldo.RespaldoProgramacion;
import uteq.edu.ec.artisync.entity.respaldo.TipoRespaldo;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.respaldo.RespaldoRepository;
import uteq.edu.ec.artisync.scheduler.RespaldoEjecutorServicio;
import uteq.edu.ec.artisync.scheduler.RespaldoRetencionScheduler;
import uteq.edu.ec.artisync.service.respaldo.ArchivoRespaldo;
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
class RespaldoServicioImplTest {

    @Mock
    private RespaldoRepository respaldoRepository;

    @Mock
    private RespaldoEjecutorServicio respaldoEjecutorServicio;

    @Mock
    private RespaldoRetencionScheduler retencionScheduler;

    @InjectMocks
    private RespaldoServicioImpl servicio;

    @TempDir
    Path tempDir;

    private Respaldo respaldoBase(Long id) {
        Respaldo respaldo = new Respaldo();
        respaldo.setIdRespaldo(id);
        respaldo.setTipoRespaldo(TipoRespaldo.FULL);
        respaldo.setEstadoRespaldo(EstadoRespaldo.COMPLETADO);
        respaldo.setOrigen(OrigenRespaldo.MANUAL);
        respaldo.setCorreoSolicitante("admin@artisync.dev");
        return respaldo;
    }

    @Test
    void solicitarRespaldo_SinRespaldoEnProgreso_DebeIniciarUnoNuevo() {
        when(respaldoRepository.existsByEstadoRespaldo(EstadoRespaldo.EN_PROGRESO)).thenReturn(false);
        Respaldo creado = respaldoBase(1L);
        when(respaldoEjecutorServicio.iniciarManual(TipoRespaldo.FULL, "admin@artisync.dev")).thenReturn(creado);

        RespuestaRespaldo resultado = servicio.solicitarRespaldo(TipoRespaldo.FULL, "admin@artisync.dev");

        assertThat(resultado.getIdRespaldo()).isEqualTo(1L);
    }

    @Test
    void solicitarRespaldo_ConRespaldoEnProgreso_DebeRechazar() {
        when(respaldoRepository.existsByEstadoRespaldo(EstadoRespaldo.EN_PROGRESO)).thenReturn(true);

        assertThatThrownBy(() -> servicio.solicitarRespaldo(TipoRespaldo.FULL, "admin@artisync.dev"))
                .isInstanceOf(ExcepcionReglaNegocio.class);
        verify(respaldoEjecutorServicio, never()).iniciarManual(any(), any());
    }

    @Test
    void listar_DebeMapearLaPaginaDelRepositorio() {
        FiltroRespaldo filtro = new FiltroRespaldo();
        Pageable pageable = PageRequest.of(0, 10);
        when(respaldoRepository.findAll(any(Specification.class), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(respaldoBase(1L))));

        PagedResponse<RespuestaRespaldo> resultado = servicio.listar(filtro, pageable);

        assertThat(resultado.getContent()).hasSize(1);
        assertThat(resultado.getContent().get(0).getIdRespaldo()).isEqualTo(1L);
    }

    @Test
    void obtenerPorId_Existente_DebeRetornarlo() {
        when(respaldoRepository.findById(1L)).thenReturn(Optional.of(respaldoBase(1L)));

        RespuestaRespaldo resultado = servicio.obtenerPorId(1L);

        assertThat(resultado.getIdRespaldo()).isEqualTo(1L);
    }

    @Test
    void obtenerPorId_Inexistente_DebeLanzarNoEncontrado() {
        when(respaldoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtenerPorId(99L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    void obtenerPorId_ConProgramacionAsociada_DebeExponerSuId() {
        Respaldo respaldo = respaldoBase(1L);
        RespaldoProgramacion programacion = new RespaldoProgramacion();
        programacion.setIdProgramacion(7L);
        respaldo.setProgramacion(programacion);
        when(respaldoRepository.findById(1L)).thenReturn(Optional.of(respaldo));

        RespuestaRespaldo resultado = servicio.obtenerPorId(1L);

        assertThat(resultado.getIdProgramacion()).isEqualTo(7L);
    }

    @Test
    void descargar_SinRutaDeArchivo_DebeLanzarNoEncontrado() {
        Respaldo respaldo = respaldoBase(1L);
        respaldo.setRutaArchivo(null);
        when(respaldoRepository.findById(1L)).thenReturn(Optional.of(respaldo));

        assertThatThrownBy(() -> servicio.descargar(1L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    void descargar_ConRutaPeroArchivoInexistenteEnDisco_DebeLanzarNoEncontrado() {
        Respaldo respaldo = respaldoBase(1L);
        respaldo.setRutaArchivo(tempDir.resolve("no-existe.sql").toString());
        when(respaldoRepository.findById(1L)).thenReturn(Optional.of(respaldo));

        assertThatThrownBy(() -> servicio.descargar(1L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    void descargar_ConArchivoReal_DebeRetornarloConSuTamano() throws IOException {
        Path archivo = tempDir.resolve("backup.sql");
        Files.writeString(archivo, "contenido de prueba");
        Respaldo respaldo = respaldoBase(1L);
        respaldo.setRutaArchivo(archivo.toString());
        respaldo.setNombreArchivo("backup.sql");
        when(respaldoRepository.findById(1L)).thenReturn(Optional.of(respaldo));

        ArchivoRespaldo resultado = servicio.descargar(1L);

        assertThat(resultado.nombreArchivo()).isEqualTo("backup.sql");
        assertThat(resultado.tamanoBytes()).isEqualTo(Files.size(archivo));
    }

    @Test
    void eliminar_RespaldoEnProgreso_DebeRechazar() {
        Respaldo respaldo = respaldoBase(1L);
        respaldo.setEstadoRespaldo(EstadoRespaldo.EN_PROGRESO);
        when(respaldoRepository.findById(1L)).thenReturn(Optional.of(respaldo));

        assertThatThrownBy(() -> servicio.eliminar(1L))
                .isInstanceOf(ExcepcionReglaNegocio.class);
        verify(respaldoRepository, never()).delete(any(Respaldo.class));
    }

    @Test
    void eliminar_ConIncrementalesDependientes_DebeRechazar() {
        Respaldo respaldo = respaldoBase(1L);
        when(respaldoRepository.findById(1L)).thenReturn(Optional.of(respaldo));
        when(retencionScheduler.esSeguroEliminar(respaldo)).thenReturn(false);

        assertThatThrownBy(() -> servicio.eliminar(1L))
                .isInstanceOf(ExcepcionReglaNegocio.class);
        verify(respaldoRepository, never()).delete(any(Respaldo.class));
    }

    @Test
    void eliminar_SinArchivoEnDisco_DebeBorrarSoloElRegistro() {
        Respaldo respaldo = respaldoBase(1L);
        respaldo.setRutaArchivo(null);
        when(respaldoRepository.findById(1L)).thenReturn(Optional.of(respaldo));
        when(retencionScheduler.esSeguroEliminar(respaldo)).thenReturn(true);

        servicio.eliminar(1L);

        verify(respaldoRepository, times(1)).delete(respaldo);
    }

    @Test
    void eliminar_ConArchivoReal_DebeBorrarloYElRegistro() throws IOException {
        Path archivo = tempDir.resolve("borrar.sql");
        Files.writeString(archivo, "x");
        Respaldo respaldo = respaldoBase(1L);
        respaldo.setRutaArchivo(archivo.toString());
        when(respaldoRepository.findById(1L)).thenReturn(Optional.of(respaldo));
        when(retencionScheduler.esSeguroEliminar(respaldo)).thenReturn(true);

        servicio.eliminar(1L);

        assertThat(Files.exists(archivo)).isFalse();
        verify(respaldoRepository).delete(respaldo);
    }
}
