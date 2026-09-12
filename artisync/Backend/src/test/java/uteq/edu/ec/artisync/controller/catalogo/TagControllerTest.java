package uteq.edu.ec.artisync.controller.catalogo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.peticion.catalogo.CreateTagRequest;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.TagResponse;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.service.catalogo.ITagService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TagControllerTest {

    @Mock
    private ITagService etiquetaServicio;

    @InjectMocks
    private TagController controlador;

    @Test
    void listarEtiquetas_DebeRetornarLaListaDelServicio() {
        when(etiquetaServicio.listTags())
                .thenReturn(List.of(TagResponse.builder().idEtiqueta(1L).nombreEtiqueta("3D").build()));

        ResponseEntity<List<TagResponse>> result = controlador.listTags();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
    }

    @Test
    void obtenerPorId_DebeRetornarLaEtiqueta() {
        when(etiquetaServicio.getById(1L))
                .thenReturn(TagResponse.builder().idEtiqueta(1L).nombreEtiqueta("3D").build());

        ResponseEntity<TagResponse> result = controlador.getById(1L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getNombreEtiqueta()).isEqualTo("3D");
    }

    @Test
    void crearEtiqueta_DebeRetornarCreada() {
        CreateTagRequest peticion = CreateTagRequest.builder().nombreEtiqueta("Pixel art").build();
        when(etiquetaServicio.createTag(peticion))
                .thenReturn(TagResponse.builder().idEtiqueta(2L).nombreEtiqueta("Pixel art").build());

        ResponseEntity<TagResponse> result = controlador.createTag(peticion);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getIdEtiqueta()).isEqualTo(2L);
    }

    @Test
    void eliminarEtiqueta_DebeRetornarMensajeDeConfirmacion() {
        ResponseEntity<RespuestaMensaje> result = controlador.deleteTag(2L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getMensaje()).contains("eliminada");
    }
}
