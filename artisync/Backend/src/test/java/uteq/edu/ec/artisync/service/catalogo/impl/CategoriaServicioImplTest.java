package uteq.edu.ec.artisync.service.catalogo.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionActualizarCategoria;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearCategoria;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearSubcategoria;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaCategoria;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaSubcategoria;
import uteq.edu.ec.artisync.entity.catalogo.Categoria;
import uteq.edu.ec.artisync.entity.catalogo.FlujoTrabajo;
import uteq.edu.ec.artisync.entity.catalogo.Subcategoria;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.catalogo.CategoriaRepository;
import uteq.edu.ec.artisync.repository.catalogo.FlujoTrabajoRepository;
import uteq.edu.ec.artisync.repository.catalogo.SubcategoriaRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CategoriaServicioImplTest {

    @Mock private CategoriaRepository categoriaRepository;
    @Mock private SubcategoriaRepository subcategoriaRepository;
    @Mock private FlujoTrabajoRepository flujoTrabajoRepository;

    @InjectMocks
    private CategoriaServicioImpl categoriaServicio;

    private Categoria categoria;

    @BeforeEach
    void setUp() {
        categoria = Categoria.builder().idCategoria(1L).nombreCategoria("Arte").estadoActiva(true).build();
    }

    @Test
    @DisplayName("crearCategoria guarda cuando el nombre no esta repetido")
    void crearCategoria_guardaCuandoNoRepetida() {
        PeticionCrearCategoria peticion = PeticionCrearCategoria.builder().nombreCategoria("Musica").estadoActiva(true).build();
        given(categoriaRepository.existsByNombreCategoriaIgnoreCase("Musica")).willReturn(false);
        given(categoriaRepository.save(any(Categoria.class))).willAnswer(inv -> inv.getArgument(0));

        RespuestaCategoria respuesta = categoriaServicio.crearCategoria(peticion);

        assertThat(respuesta.getNombreCategoria()).isEqualTo("Musica");
    }

    @Test
    @DisplayName("crearCategoria rechaza un nombre duplicado")
    void crearCategoria_rechazaDuplicado() {
        PeticionCrearCategoria peticion = PeticionCrearCategoria.builder().nombreCategoria("Arte").build();
        given(categoriaRepository.existsByNombreCategoriaIgnoreCase("Arte")).willReturn(true);

        assertThatThrownBy(() -> categoriaServicio.crearCategoria(peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class);
    }

    @Test
    @DisplayName("crearCategoria asigna el flujo cuando se indica un id valido")
    void crearCategoria_asignaFlujo() {
        FlujoTrabajo flujo = FlujoTrabajo.builder().idFlujo(2L).nombreFlujo("Flujo estandar").build();
        PeticionCrearCategoria peticion = PeticionCrearCategoria.builder().nombreCategoria("Diseno").idFlujo(2L).build();
        given(categoriaRepository.existsByNombreCategoriaIgnoreCase("Diseno")).willReturn(false);
        given(flujoTrabajoRepository.findById(2L)).willReturn(Optional.of(flujo));
        given(categoriaRepository.save(any(Categoria.class))).willAnswer(inv -> inv.getArgument(0));

        RespuestaCategoria respuesta = categoriaServicio.crearCategoria(peticion);

        assertThat(respuesta.getIdFlujo()).isEqualTo(2L);
    }

    @Test
    @DisplayName("crearCategoria lanza recurso no encontrado si el flujo indicado no existe")
    void crearCategoria_flujoInexistente() {
        PeticionCrearCategoria peticion = PeticionCrearCategoria.builder().nombreCategoria("Diseno").idFlujo(99L).build();
        given(categoriaRepository.existsByNombreCategoriaIgnoreCase("Diseno")).willReturn(false);
        given(flujoTrabajoRepository.findById(99L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaServicio.crearCategoria(peticion))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("actualizarCategoria cambia el nombre cuando no colisiona")
    void actualizarCategoria_cambiaNombre() {
        PeticionActualizarCategoria peticion = PeticionActualizarCategoria.builder().nombreCategoria("Arte Digital").build();
        given(categoriaRepository.findById(1L)).willReturn(Optional.of(categoria));
        given(categoriaRepository.existsByNombreCategoriaIgnoreCase("Arte Digital")).willReturn(false);
        given(categoriaRepository.save(any(Categoria.class))).willAnswer(inv -> inv.getArgument(0));

        RespuestaCategoria respuesta = categoriaServicio.actualizarCategoria(1L, peticion);

        assertThat(respuesta.getNombreCategoria()).isEqualTo("Arte Digital");
    }

    @Test
    @DisplayName("actualizarCategoria no valida duplicado si el nombre no cambia")
    void actualizarCategoria_mismoNombre() {
        PeticionActualizarCategoria peticion = PeticionActualizarCategoria.builder().nombreCategoria("Arte").build();
        given(categoriaRepository.findById(1L)).willReturn(Optional.of(categoria));
        given(categoriaRepository.save(any(Categoria.class))).willAnswer(inv -> inv.getArgument(0));

        assertThat(categoriaServicio.actualizarCategoria(1L, peticion)).isNotNull();
    }

    @Test
    @DisplayName("actualizarCategoria rechaza el nombre si ya existe en otra categoria")
    void actualizarCategoria_rechazaDuplicado() {
        PeticionActualizarCategoria peticion = PeticionActualizarCategoria.builder().nombreCategoria("Musica").build();
        given(categoriaRepository.findById(1L)).willReturn(Optional.of(categoria));
        given(categoriaRepository.existsByNombreCategoriaIgnoreCase("Musica")).willReturn(true);

        assertThatThrownBy(() -> categoriaServicio.actualizarCategoria(1L, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class);
    }

    @Test
    @DisplayName("actualizarCategoria lanza recurso no encontrado si la categoria no existe")
    void actualizarCategoria_inexistente() {
        given(categoriaRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaServicio.actualizarCategoria(1L, PeticionActualizarCategoria.builder().build()))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("eliminarCategoria borra cuando existe")
    void eliminarCategoria_borraCuandoExiste() {
        given(categoriaRepository.existsById(1L)).willReturn(true);

        categoriaServicio.eliminarCategoria(1L);

        org.mockito.Mockito.verify(categoriaRepository).deleteById(1L);
    }

    @Test
    @DisplayName("eliminarCategoria lanza recurso no encontrado si no existe")
    void eliminarCategoria_inexistente() {
        given(categoriaRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> categoriaServicio.eliminarCategoria(1L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
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
    @DisplayName("obtenerCategoriaPorId lanza recurso no encontrado si no existe")
    void obtenerCategoriaPorId_inexistente() {
        given(categoriaRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaServicio.obtenerCategoriaPorId(1L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("listarSubcategoriasPorCategoria devuelve las subcategorias de la categoria")
    void listarSubcategoriasPorCategoria_devuelveLista() {
        Subcategoria sub = Subcategoria.builder().idSubcategoria(1L).categoria(categoria).nombreSubcategoria("Ilustracion").build();
        given(categoriaRepository.existsById(1L)).willReturn(true);
        given(subcategoriaRepository.findByCategoriaIdCategoriaOrderByNombreSubcategoriaAsc(1L)).willReturn(List.of(sub));

        List<RespuestaSubcategoria> resultado = categoriaServicio.listarSubcategoriasPorCategoria(1L);

        assertThat(resultado).hasSize(1);
    }

    @Test
    @DisplayName("listarSubcategoriasPorCategoria lanza recurso no encontrado si la categoria no existe")
    void listarSubcategoriasPorCategoria_categoriaInexistente() {
        given(categoriaRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> categoriaServicio.listarSubcategoriasPorCategoria(1L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("listarTodasLasSubcategorias devuelve todas ordenadas")
    void listarTodasLasSubcategorias_devuelveLista() {
        Subcategoria sub = Subcategoria.builder().idSubcategoria(1L).categoria(categoria).nombreSubcategoria("Ilustracion").build();
        given(subcategoriaRepository.findAllByOrderByNombreSubcategoriaAsc()).willReturn(List.of(sub));

        assertThat(categoriaServicio.listarTodasLasSubcategorias()).hasSize(1);
    }

    @Test
    @DisplayName("crearSubcategoria guarda cuando la categoria existe y el nombre no esta repetido")
    void crearSubcategoria_guarda() {
        PeticionCrearSubcategoria peticion = PeticionCrearSubcategoria.builder().idCategoria(1L).nombreSubcategoria("Fotografia").build();
        given(categoriaRepository.findById(1L)).willReturn(Optional.of(categoria));
        given(subcategoriaRepository.existsByCategoriaIdCategoriaAndNombreSubcategoriaIgnoreCase(1L, "Fotografia")).willReturn(false);
        given(subcategoriaRepository.save(any(Subcategoria.class))).willAnswer(inv -> inv.getArgument(0));

        RespuestaSubcategoria respuesta = categoriaServicio.crearSubcategoria(peticion);

        assertThat(respuesta.getNombreSubcategoria()).isEqualTo("Fotografia");
    }

    @Test
    @DisplayName("crearSubcategoria lanza recurso no encontrado si la categoria no existe")
    void crearSubcategoria_categoriaInexistente() {
        PeticionCrearSubcategoria peticion = PeticionCrearSubcategoria.builder().idCategoria(1L).nombreSubcategoria("Fotografia").build();
        given(categoriaRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaServicio.crearSubcategoria(peticion))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("crearSubcategoria rechaza un nombre repetido dentro de la misma categoria")
    void crearSubcategoria_rechazaDuplicado() {
        PeticionCrearSubcategoria peticion = PeticionCrearSubcategoria.builder().idCategoria(1L).nombreSubcategoria("Ilustracion").build();
        given(categoriaRepository.findById(1L)).willReturn(Optional.of(categoria));
        given(subcategoriaRepository.existsByCategoriaIdCategoriaAndNombreSubcategoriaIgnoreCase(1L, "Ilustracion")).willReturn(true);

        assertThatThrownBy(() -> categoriaServicio.crearSubcategoria(peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class);
    }

    @Test
    @DisplayName("eliminarSubcategoria borra cuando existe")
    void eliminarSubcategoria_borraCuandoExiste() {
        given(subcategoriaRepository.existsById(1L)).willReturn(true);

        categoriaServicio.eliminarSubcategoria(1L);

        org.mockito.Mockito.verify(subcategoriaRepository).deleteById(1L);
    }

    @Test
    @DisplayName("eliminarSubcategoria lanza recurso no encontrado si no existe")
    void eliminarSubcategoria_inexistente() {
        given(subcategoriaRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> categoriaServicio.eliminarSubcategoria(1L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }
}
