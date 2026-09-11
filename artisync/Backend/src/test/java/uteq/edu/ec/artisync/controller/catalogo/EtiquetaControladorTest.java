package uteq.edu.ec.artisync.controller.catalogo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearEtiqueta;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaEtiqueta;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.service.catalogo.IEtiquetaServicio;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EtiquetaControladorTest {

    @Mock
    private IEtiquetaServicio etiquetaServicio;

    @InjectMocks
    private EtiquetaControlador controlador;

    @Test
    void listarEtiquetas_DebeRetornarLaListaDelServicio() {
        when(etiquetaServicio.listarEtiquetas())
                .thenReturn(List.of(RespuestaEtiqueta.builder().idEtiqueta(1L).nombreEtiqueta("3D").build()));

        ResponseEntity<List<RespuestaEtiqueta>> result = controlador.listarEtiquetas();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
    }

    @Test
    void obtenerPorId_DebeRetornarLaEtiqueta() {
        when(etiquetaServicio.obtenerPorId(1L))
                .thenReturn(RespuestaEtiqueta.builder().idEtiqueta(1L).nombreEtiqueta("3D").build());

        ResponseEntity<RespuestaEtiqueta> result = controlador.obtenerPorId(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getNombreEtiqueta()).isEqualTo("3D");
    }

    @Test
    void crearEtiqueta_DebeRetornarCreada() {
        PeticionCrearEtiqueta peticion = PeticionCrearEtiqueta.builder().nombreEtiqueta("Pixel art").build();
        when(etiquetaServicio.crearEtiqueta(peticion))
                .thenReturn(RespuestaEtiqueta.builder().idEtiqueta(2L).nombreEtiqueta("Pixel art").build());

        ResponseEntity<RespuestaEtiqueta> result = controlador.crearEtiqueta(peticion);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getIdEtiqueta()).isEqualTo(2L);
    }

    @Test
    void eliminarEtiqueta_DebeRetornarMensajeDeConfirmacion() {
        ResponseEntity<RespuestaMensaje> result = controlador.eliminarEtiqueta(2L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getMensaje()).contains("eliminada");
    }
}
