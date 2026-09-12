package uteq.edu.ec.artisync.controller.comunicacion;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.CommentResponse;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.service.comunicacion.PortfolioCommentService;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * REQ-F-010: el hueco que la matriz de trazabilidad señalaba no era
 * PortfolioCommentServiceImpl/Controlador (ya cubiertos), sino este
 * controlador administrativo, sin ninguna prueba hasta ahora.
 */
@ExtendWith(MockitoExtension.class)
class AdminCommentControllerTest {

    @Mock
    private PortfolioCommentService comentarioService;

    @InjectMocks
    private AdminCommentController controlador;

    @Test
    void listarParaModeracion_devuelveOk_conTodosLosComentarios() {
        Pageable pageable = mock(Pageable.class);
        Page<CommentResponse> page = new PageImpl<>(Collections.emptyList());
        when(comentarioService.listForModeration(pageable)).thenReturn(page);

        ResponseEntity<Page<CommentResponse>> res = controlador.listForModeration(pageable);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(page);
        verify(comentarioService).listForModeration(pageable);
    }

    @Test
    void ocultarComentario_devuelveOk_conElComentarioActualizado() {
        CommentResponse respuesta = new CommentResponse();
        when(comentarioService.hideComment(10L)).thenReturn(respuesta);

        ResponseEntity<CommentResponse> res = controlador.hideComment(10L);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void ocultarComentario_propagaExcepcion_siNoExiste() {
        when(comentarioService.hideComment(99L))
                .thenThrow(new ResourceNotFoundException("Comentario no encontrado: 99"));

        assertThrows(ResourceNotFoundException.class, () -> controlador.hideComment(99L));
    }

    @Test
    void reactivarComentario_devuelveOk_conElComentarioActualizado() {
        CommentResponse respuesta = new CommentResponse();
        when(comentarioService.reactivateComment(10L)).thenReturn(respuesta);

        ResponseEntity<CommentResponse> res = controlador.reactivateComment(10L);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void reactivarComentario_propagaExcepcion_siNoExiste() {
        when(comentarioService.reactivateComment(99L))
                .thenThrow(new ResourceNotFoundException("Comentario no encontrado: 99"));

        assertThrows(ResourceNotFoundException.class, () -> controlador.reactivateComment(99L));
    }

    @Test
    void eliminarComentario_devuelveNoContent_yBorraComoAdmin() {
        ResponseEntity<Void> res = controlador.deleteComment(10L);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        // esAdmin=true: borrado fisico definitivo, distinto del borrado logico
        // que usa la ruta de autor/dueno en PortfolioCommentController.
        verify(comentarioService).deleteComment(10L, null, true);
    }
}
