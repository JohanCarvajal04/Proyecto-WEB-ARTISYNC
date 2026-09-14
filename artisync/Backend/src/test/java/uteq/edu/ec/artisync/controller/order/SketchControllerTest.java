package uteq.edu.ec.artisync.controller.order;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import uteq.edu.ec.artisync.dto.response.order.SketchResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.order.ISketchService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SketchControllerTest {

    @Mock private ISketchService bocetoServicio;

    @InjectMocks
    private SketchController controller;

    private final CustomUserDetails usuario = new CustomUserDetails(
            1L, "cliente@test.dev", "x", true, true, true, true, List.of());

    @Test
    void uploadSketch_devuelve201ConElBocetoGuardado() {
        SketchResponse guardado = SketchResponse.builder().idBoceto(1L).idPedido(10L).build();
        var imagen = new MockMultipartFile("imagen", "boceto.png", "image/png", new byte[]{1, 2, 3});
        given(bocetoServicio.uploadSketch(10L, 1L, imagen)).willReturn(guardado);

        ResponseEntity<SketchResponse> respuesta = controller.uploadSketch(10L, usuario, imagen);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(respuesta.getBody()).isEqualTo(guardado);
    }

    @Test
    void getSketch_devuelveElBocetoVigente() {
        SketchResponse vigente = SketchResponse.builder().idBoceto(2L).idPedido(10L).build();
        given(bocetoServicio.getSketch(10L, 1L)).willReturn(vigente);

        ResponseEntity<SketchResponse> respuesta = controller.getSketch(10L, usuario);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(vigente);
    }

    @Test
    void downloadSketch_devuelveElArchivoComoAdjunto() {
        ISketchService.DownloadedFile archivo =
                new ISketchService.DownloadedFile("contenido".getBytes(), "boceto-pedido-10.png", "image/png");
        given(bocetoServicio.downloadSketch(10L, 1L)).willReturn(archivo);

        ResponseEntity<byte[]> respuesta = controller.downloadSketch(10L, usuario);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo("contenido".getBytes());
        assertThat(respuesta.getHeaders().getContentDisposition().getFilename()).isEqualTo("boceto-pedido-10.png");
    }
}
