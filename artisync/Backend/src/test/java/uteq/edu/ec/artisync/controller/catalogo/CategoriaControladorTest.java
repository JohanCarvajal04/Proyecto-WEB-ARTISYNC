package uteq.edu.ec.artisync.controller.catalogo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaCategoria;
import uteq.edu.ec.artisync.service.catalogo.ICategoriaServicio;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoriaControladorTest {

    @Mock
    private ICategoriaServicio categoriaServicio;

    @InjectMocks
    private CategoriaControlador categoriaControlador;

    @Test
    void listarCategoriasActivas_DebeRetornarLista() {
        RespuestaCategoria cat = new RespuestaCategoria();
        cat.setNombreCategoria("Test");
        when(categoriaServicio.listarCategoriasActivas()).thenReturn(List.of(cat));

        ResponseEntity<List<RespuestaCategoria>> result = categoriaControlador.listarCategoriasActivas();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
        assertEquals("Test", result.getBody().get(0).getNombreCategoria());
    }

    @Test
    void eliminarCategoria_DebeRetornarOk() {
        ResponseEntity<RespuestaMensaje> result = categoriaControlador.eliminarCategoria(1L);
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}
