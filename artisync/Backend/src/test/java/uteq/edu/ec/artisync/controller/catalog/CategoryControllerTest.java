package uteq.edu.ec.artisync.controller.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.response.catalog.CategoryResponse;
import uteq.edu.ec.artisync.dto.response.catalog.SubcategoryResponse;
import uteq.edu.ec.artisync.service.catalog.ICategoryService;
import uteq.edu.ec.artisync.dto.response.comun.MessageResponse;

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
        when(categoriaServicio.listActiveCategories()).thenReturn(List.of(cat));

        ResponseEntity<List<CategoryResponse>> result = categoriaControlador.listActiveCategories();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
        assertEquals("Test", result.getBody().get(0).getNombreCategoria());
    }

    @Test
    void eliminarCategoria_DebeRetornarOk() {
        ResponseEntity<MessageResponse> result = categoriaControlador.deleteCategory(1L, null);
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void obtenerCategoriaPorId_DebeRetornarOk() {
        CategoryResponse cat = new CategoryResponse();
        cat.setNombreCategoria("Test");
        when(categoriaServicio.getCategoryById(1L)).thenReturn(cat);

        ResponseEntity<CategoryResponse> result = categoriaControlador.getCategoryById(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("Test", result.getBody().getNombreCategoria());
    }

    @Test
    void listarSubcategoriasPorCategoria_DebeRetornarLista() {
        SubcategoryResponse sub = new SubcategoryResponse();
        when(categoriaServicio.listSubcategoriesByCategory(1L)).thenReturn(List.of(sub));

        ResponseEntity<List<SubcategoryResponse>> result = categoriaControlador.listSubcategoriesByCategory(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void listarPendientesDeRevision_DebeRetornarLista() {
        CategoryResponse cat = new CategoryResponse();
        when(categoriaServicio.listCategoriesPendingReview()).thenReturn(List.of(cat));

        ResponseEntity<List<CategoryResponse>> result = categoriaControlador.listPendingReview();

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void marcarRevisada_DebeRetornarOk() {
        CategoryResponse cat = new CategoryResponse();
        when(categoriaServicio.markCategoryReviewed(1L)).thenReturn(cat);

        ResponseEntity<CategoryResponse> result = categoriaControlador.markReviewed(1L);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}
