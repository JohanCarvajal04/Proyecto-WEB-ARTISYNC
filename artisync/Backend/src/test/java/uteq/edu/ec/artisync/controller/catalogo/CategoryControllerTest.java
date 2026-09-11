package uteq.edu.ec.artisync.controller.catalogo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.CategoryResponse;
import uteq.edu.ec.artisync.service.catalogo.ICategoryService;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryControllerTest {

    @Mock
    private ICategoryService categoriaServicio;

    @InjectMocks
    private CategoryController categoriaControlador;

    @Test
    void listarCategoriasActivas_DebeRetornarLista() {
        CategoryResponse cat = new CategoryResponse();
        cat.setNombreCategoria("Test");
        when(categoriaServicio.listarCategoriasActivas()).thenReturn(List.of(cat));

        ResponseEntity<List<CategoryResponse>> result = categoriaControlador.listarCategoriasActivas();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
        assertEquals("Test", result.getBody().get(0).getNombreCategoria());
    }

    @Test
    void eliminarCategoria_DebeRetornarOk() {
        ResponseEntity<RespuestaMensaje> result = categoriaControlador.eliminarCategoria(1L, null);
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}
