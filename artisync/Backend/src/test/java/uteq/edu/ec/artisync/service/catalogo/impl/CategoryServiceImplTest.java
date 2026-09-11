package uteq.edu.ec.artisync.service.catalogo.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.dto.peticion.catalogo.UpdateCategoryRequest;
import uteq.edu.ec.artisync.dto.peticion.catalogo.CreateCategoryRequest;
import uteq.edu.ec.artisync.dto.peticion.catalogo.CreateSubcategoryRequest;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.CategoryResponse;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.SubcategoryResponse;
import uteq.edu.ec.artisync.entity.catalogo.Category;
import uteq.edu.ec.artisync.entity.catalogo.Subcategory;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.catalogo.CategoryRepository;
import uteq.edu.ec.artisync.repository.catalogo.OfferingSubcategoryRepository;
import uteq.edu.ec.artisync.repository.catalogo.SubcategoryRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;
import uteq.edu.ec.artisync.service.comunicacion.NotificacionService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CategoryServiceImplTest {

    @Mock private CategoryRepository categoriaRepository;
    @Mock private SubcategoryRepository subcategoriaRepository;
    @Mock private OfferingSubcategoryRepository servicioSubcategoriaRepository;
    @Mock private UserRepository usuarioRepository;
    @Mock private NotificacionService notificacionService;

    @InjectMocks
    private CategoryServiceImpl categoriaServicio;

    private Category categoria;
    private User creador;

    @BeforeEach
    void setUp() {
        categoria = Category.builder().idCategoria(1L).nombreCategoria("Arte").estadoActiva(true).build();
        creador = User.builder().idUsuario(9L).nombres("Ana").apellidos("Perez").build();
    }

    @Test
    @DisplayName("crearCategoria guarda cuando el nombre no esta repetido")
    void crearCategoria_guardaCuandoNoRepetida() {
        CreateCategoryRequest peticion = CreateCategoryRequest.builder().nombreCategoria("Musica").estadoActiva(true).build();
        given(categoriaRepository.existsByNombreCategoriaIgnoreCase("Musica")).willReturn(false);
        given(categoriaRepository.save(any(Category.class))).willAnswer(inv -> inv.getArgument(0));

        CategoryResponse respuesta = categoriaServicio.crearCategoria(null, peticion);

        assertThat(respuesta.getNombreCategoria()).isEqualTo("Musica");
        assertThat(respuesta.getRevisado()).isTrue();
        assertThat(respuesta.getIdUsuarioCreador()).isNull();
    }

    @Test
    @DisplayName("crearCategoria rechaza un nombre duplicado")
    void crearCategoria_rechazaDuplicado() {
        CreateCategoryRequest peticion = CreateCategoryRequest.builder().nombreCategoria("Arte").build();
        given(categoriaRepository.existsByNombreCategoriaIgnoreCase("Arte")).willReturn(true);

        assertThatThrownBy(() -> categoriaServicio.crearCategoria(null, peticion))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("crearCategoria creada por un creador queda sin revisar y con dueño")
    void crearCategoria_deUnCreador_quedaSinRevisar() {
        CreateCategoryRequest peticion = CreateCategoryRequest.builder().nombreCategoria("Ceramica").build();
        given(categoriaRepository.existsByNombreCategoriaIgnoreCase("Ceramica")).willReturn(false);
        given(usuarioRepository.findById(9L)).willReturn(Optional.of(creador));
        given(categoriaRepository.save(any(Category.class))).willAnswer(inv -> inv.getArgument(0));

        CategoryResponse respuesta = categoriaServicio.crearCategoria(9L, peticion);

        assertThat(respuesta.getRevisado()).isFalse();
        assertThat(respuesta.getIdUsuarioCreador()).isEqualTo(9L);
    }

    @Test
    @DisplayName("actualizarCategoria cambia el nombre cuando no colisiona")
    void actualizarCategoria_cambiaNombre() {
        UpdateCategoryRequest peticion = UpdateCategoryRequest.builder().nombreCategoria("Arte Digital").build();
        given(categoriaRepository.findById(1L)).willReturn(Optional.of(categoria));
        given(categoriaRepository.existsByNombreCategoriaIgnoreCase("Arte Digital")).willReturn(false);
        given(categoriaRepository.save(any(Category.class))).willAnswer(inv -> inv.getArgument(0));

        CategoryResponse respuesta = categoriaServicio.actualizarCategoria(1L, peticion);

        assertThat(respuesta.getNombreCategoria()).isEqualTo("Arte Digital");
    }

    @Test
    @DisplayName("actualizarCategoria no valida duplicado si el nombre no cambia")
    void actualizarCategoria_mismoNombre() {
        UpdateCategoryRequest peticion = UpdateCategoryRequest.builder().nombreCategoria("Arte").build();
        given(categoriaRepository.findById(1L)).willReturn(Optional.of(categoria));
        given(categoriaRepository.save(any(Category.class))).willAnswer(inv -> inv.getArgument(0));

        assertThat(categoriaServicio.actualizarCategoria(1L, peticion)).isNotNull();
    }

    @Test
    @DisplayName("actualizarCategoria rechaza el nombre si ya existe en otra categoria")
    void actualizarCategoria_rechazaDuplicado() {
        UpdateCategoryRequest peticion = UpdateCategoryRequest.builder().nombreCategoria("Musica").build();
        given(categoriaRepository.findById(1L)).willReturn(Optional.of(categoria));
        given(categoriaRepository.existsByNombreCategoriaIgnoreCase("Musica")).willReturn(true);

        assertThatThrownBy(() -> categoriaServicio.actualizarCategoria(1L, peticion))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("actualizarCategoria lanza recurso no encontrado si la categoria no existe")
    void actualizarCategoria_inexistente() {
        given(categoriaRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaServicio.actualizarCategoria(1L, UpdateCategoryRequest.builder().build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("eliminarCategoria borra cuando existe y no tiene dueño, sin exigir motivo")
    void eliminarCategoria_borraCuandoExiste() {
        given(categoriaRepository.findById(1L)).willReturn(Optional.of(categoria));

        categoriaServicio.eliminarCategoria(1L, null);

        verify(categoriaRepository).deleteById(1L);
        verify(notificacionService, never()).notificar(any(), any(), any());
    }

    @Test
    @DisplayName("eliminarCategoria lanza recurso no encontrado si no existe")
    void eliminarCategoria_inexistente() {
        given(categoriaRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaServicio.eliminarCategoria(1L, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("eliminarCategoria rechaza si alguna subcategoria tiene servicios publicados")
    void eliminarCategoria_rechazaConServiciosDependientes() {
        given(categoriaRepository.findById(1L)).willReturn(Optional.of(categoria));
        given(servicioSubcategoriaRepository.existsBySubcategoriaCategoriaIdCategoria(1L)).willReturn(true);

        assertThatThrownBy(() -> categoriaServicio.eliminarCategoria(1L, null))
                .isInstanceOf(BusinessRuleException.class);
        verify(categoriaRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("eliminarCategoria de un creador exige motivo y lo notifica al borrar")
    void eliminarCategoria_deUnCreador_exigeMotivoYNotifica() {
        Category deCreador = Category.builder().idCategoria(2L).nombreCategoria("Ceramica").creador(creador).revisado(false).build();
        given(categoriaRepository.findById(2L)).willReturn(Optional.of(deCreador));

        assertThatThrownBy(() -> categoriaServicio.eliminarCategoria(2L, null))
                .isInstanceOf(BusinessRuleException.class);
        verify(categoriaRepository, never()).deleteById(any());

        categoriaServicio.eliminarCategoria(2L, "Rubro duplicado con Arte");

        verify(categoriaRepository).deleteById(2L);
        verify(notificacionService, times(1)).notificar(
                org.mockito.ArgumentMatchers.eq(creador),
                org.mockito.ArgumentMatchers.eq("CATEGORIA_ELIMINADA"),
                org.mockito.ArgumentMatchers.contains("Rubro duplicado con Arte"));
    }

    @Test
    @DisplayName("marcarCategoriaRevisada pone revisado en true")
    void marcarCategoriaRevisada_poneTrue() {
        Category sinRevisar = Category.builder().idCategoria(3L).nombreCategoria("Ceramica").creador(creador).revisado(false).build();
        given(categoriaRepository.findById(3L)).willReturn(Optional.of(sinRevisar));
        given(categoriaRepository.save(any(Category.class))).willAnswer(inv -> inv.getArgument(0));

        CategoryResponse respuesta = categoriaServicio.marcarCategoriaRevisada(3L);

        assertThat(respuesta.getRevisado()).isTrue();
        verify(notificacionService, never()).notificar(any(), any(), any());
    }

    @Test
    @DisplayName("listarCategoriasActivas mapea las categorias activas")
    void listarCategoriasActivas_mapea() {
        given(categoriaRepository.findByEstadoActivaTrueOrderByNombreCategoriaAsc()).willReturn(List.of(categoria));

        assertThat(categoriaServicio.listarCategoriasActivas()).hasSize(1);
    }

    @Test
    @DisplayName("listarTodasLasCategorias mapea todas las categorias")
    void listarTodasLasCategorias_mapea() {
        given(categoriaRepository.findAllByOrderByNombreCategoriaAsc()).willReturn(List.of(categoria));

        assertThat(categoriaServicio.listarTodasLasCategorias()).hasSize(1);
    }

    @Test
    @DisplayName("listarCategoriasPendientesRevision devuelve solo las no revisadas")
    void listarCategoriasPendientesRevision_devuelveLista() {
        Category sinRevisar = Category.builder().idCategoria(4L).nombreCategoria("Ceramica").creador(creador).revisado(false).build();
        given(categoriaRepository.findByRevisadoFalseOrderByActualizadoEnDesc()).willReturn(List.of(sinRevisar));

        assertThat(categoriaServicio.listarCategoriasPendientesRevision()).hasSize(1);
    }

    @Test
    @DisplayName("obtenerCategoriaPorId lanza recurso no encontrado si no existe")
    void obtenerCategoriaPorId_inexistente() {
        given(categoriaRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaServicio.obtenerCategoriaPorId(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("listarSubcategoriasPorCategoria devuelve las subcategorias de la categoria")
    void listarSubcategoriasPorCategoria_devuelveLista() {
        Subcategory sub = Subcategory.builder().idSubcategoria(1L).categoria(categoria).nombreSubcategoria("Ilustracion").build();
        given(categoriaRepository.existsById(1L)).willReturn(true);
        given(subcategoriaRepository.findByCategoriaIdCategoriaOrderByNombreSubcategoriaAsc(1L)).willReturn(List.of(sub));

        List<SubcategoryResponse> resultado = categoriaServicio.listarSubcategoriasPorCategoria(1L);

        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("listarSubcategoriasPorCategoria lanza recurso no encontrado si la categoria no existe")
    void listarSubcategoriasPorCategoria_categoriaInexistente() {
        given(categoriaRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> categoriaServicio.listarSubcategoriasPorCategoria(1L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("listarTodasLasSubcategorias devuelve todas ordenadas")
    void listarTodasLasSubcategorias_devuelveLista() {
        Subcategory sub = Subcategory.builder().idSubcategoria(1L).categoria(categoria).nombreSubcategoria("Ilustracion").build();
        given(subcategoriaRepository.findAllByOrderByNombreSubcategoriaAsc()).willReturn(List.of(sub));

        assertThat(categoriaServicio.listarTodasLasSubcategorias()).hasSize(1);
    }

    @Test
    @DisplayName("crearSubcategoria guarda cuando la categoria existe y el nombre no esta repetido")
    void crearSubcategoria_guarda() {
        CreateSubcategoryRequest peticion = CreateSubcategoryRequest.builder().idCategoria(1L).nombreSubcategoria("Fotografia").build();
        given(categoriaRepository.findById(1L)).willReturn(Optional.of(categoria));
        given(subcategoriaRepository.existsByCategoriaIdCategoriaAndNombreSubcategoriaIgnoreCase(1L, "Fotografia")).willReturn(false);
        given(subcategoriaRepository.save(any(Subcategory.class))).willAnswer(inv -> inv.getArgument(0));

        SubcategoryResponse respuesta = categoriaServicio.crearSubcategoria(null, peticion);

        assertThat(respuesta.getNombreSubcategoria()).isEqualTo("Fotografia");
        assertThat(respuesta.getRevisado()).isTrue();
    }

    @Test
    @DisplayName("crearSubcategoria de un creador queda sin revisar y con dueño")
    void crearSubcategoria_deUnCreador_quedaSinRevisar() {
        CreateSubcategoryRequest peticion = CreateSubcategoryRequest.builder().idCategoria(1L).nombreSubcategoria("Fotografia").build();
        given(categoriaRepository.findById(1L)).willReturn(Optional.of(categoria));
        given(subcategoriaRepository.existsByCategoriaIdCategoriaAndNombreSubcategoriaIgnoreCase(1L, "Fotografia")).willReturn(false);
        given(usuarioRepository.findById(9L)).willReturn(Optional.of(creador));
        given(subcategoriaRepository.save(any(Subcategory.class))).willAnswer(inv -> inv.getArgument(0));

        SubcategoryResponse respuesta = categoriaServicio.crearSubcategoria(9L, peticion);

        assertThat(respuesta.getRevisado()).isFalse();
        assertThat(respuesta.getIdUsuarioCreador()).isEqualTo(9L);
    }

    @Test
    @DisplayName("crearSubcategoria lanza recurso no encontrado si la categoria no existe")
    void crearSubcategoria_categoriaInexistente() {
        CreateSubcategoryRequest peticion = CreateSubcategoryRequest.builder().idCategoria(1L).nombreSubcategoria("Fotografia").build();
        given(categoriaRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaServicio.crearSubcategoria(null, peticion))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("crearSubcategoria rechaza un nombre repetido dentro de la misma categoria")
    void crearSubcategoria_rechazaDuplicado() {
        CreateSubcategoryRequest peticion = CreateSubcategoryRequest.builder().idCategoria(1L).nombreSubcategoria("Ilustracion").build();
        given(categoriaRepository.findById(1L)).willReturn(Optional.of(categoria));
        given(subcategoriaRepository.existsByCategoriaIdCategoriaAndNombreSubcategoriaIgnoreCase(1L, "Ilustracion")).willReturn(true);

        assertThatThrownBy(() -> categoriaServicio.crearSubcategoria(null, peticion))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    @DisplayName("eliminarSubcategoria borra cuando existe y no tiene dueño, sin exigir motivo")
    void eliminarSubcategoria_borraCuandoExiste() {
        given(subcategoriaRepository.findById(1L)).willReturn(Optional.of(
                Subcategory.builder().idSubcategoria(1L).categoria(categoria).nombreSubcategoria("Ilustracion").build()));

        categoriaServicio.eliminarSubcategoria(1L, null);

        verify(subcategoriaRepository).deleteById(1L);
        verify(notificacionService, never()).notificar(any(), any(), any());
    }

    @Test
    @DisplayName("eliminarSubcategoria lanza recurso no encontrado si no existe")
    void eliminarSubcategoria_inexistente() {
        given(subcategoriaRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaServicio.eliminarSubcategoria(1L, null))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("eliminarSubcategoria rechaza si tiene servicios publicados")
    void eliminarSubcategoria_rechazaConServiciosDependientes() {
        given(subcategoriaRepository.findById(1L)).willReturn(Optional.of(
                Subcategory.builder().idSubcategoria(1L).categoria(categoria).nombreSubcategoria("Ilustracion").build()));
        given(servicioSubcategoriaRepository.existsBySubcategoriaIdSubcategoria(1L)).willReturn(true);

        assertThatThrownBy(() -> categoriaServicio.eliminarSubcategoria(1L, null))
                .isInstanceOf(BusinessRuleException.class);
        verify(subcategoriaRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("eliminarSubcategoria de un creador exige motivo y lo notifica al borrar")
    void eliminarSubcategoria_deUnCreador_exigeMotivoYNotifica() {
        Subcategory deCreador = Subcategory.builder().idSubcategoria(5L).categoria(categoria)
                .nombreSubcategoria("Retratos").creador(creador).revisado(false).build();
        given(subcategoriaRepository.findById(5L)).willReturn(Optional.of(deCreador));

        assertThatThrownBy(() -> categoriaServicio.eliminarSubcategoria(5L, " "))
                .isInstanceOf(BusinessRuleException.class);
        verify(subcategoriaRepository, never()).deleteById(any());

        categoriaServicio.eliminarSubcategoria(5L, "No corresponde a esta categoria");

        verify(subcategoriaRepository).deleteById(5L);
        verify(notificacionService, times(1)).notificar(
                org.mockito.ArgumentMatchers.eq(creador),
                org.mockito.ArgumentMatchers.eq("SUBCATEGORIA_ELIMINADA"),
                org.mockito.ArgumentMatchers.contains("No corresponde a esta categoria"));
    }
}
