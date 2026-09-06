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
import uteq.edu.ec.artisync.entity.catalogo.Subcategoria;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.catalogo.CategoriaRepository;
import uteq.edu.ec.artisync.repository.catalogo.ServicioSubcategoriaRepository;
import uteq.edu.ec.artisync.repository.catalogo.SubcategoriaRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
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
class CategoriaServicioImplTest {

    @Mock private CategoriaRepository categoriaRepository;
    @Mock private SubcategoriaRepository subcategoriaRepository;
    @Mock private ServicioSubcategoriaRepository servicioSubcategoriaRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private NotificacionService notificacionService;

    @InjectMocks
    private CategoriaServicioImpl categoriaServicio;

    private Categoria categoria;
    private Usuario creador;

    @BeforeEach
    void setUp() {
        categoria = Categoria.builder().idCategoria(1L).nombreCategoria("Arte").estadoActiva(true).build();
        creador = Usuario.builder().idUsuario(9L).nombres("Ana").apellidos("Perez").build();
    }

    @Test
    @DisplayName("crearCategoria guarda cuando el nombre no esta repetido")
    void crearCategoria_guardaCuandoNoRepetida() {
        PeticionCrearCategoria peticion = PeticionCrearCategoria.builder().nombreCategoria("Musica").estadoActiva(true).build();
        given(categoriaRepository.existsByNombreCategoriaIgnoreCase("Musica")).willReturn(false);
        given(categoriaRepository.save(any(Categoria.class))).willAnswer(inv -> inv.getArgument(0));

        RespuestaCategoria respuesta = categoriaServicio.crearCategoria(null, peticion);

        assertThat(respuesta.getNombreCategoria()).isEqualTo("Musica");
        assertThat(respuesta.getRevisado()).isTrue();
        assertThat(respuesta.getIdUsuarioCreador()).isNull();
    }

    @Test
    @DisplayName("crearCategoria rechaza un nombre duplicado")
    void crearCategoria_rechazaDuplicado() {
        PeticionCrearCategoria peticion = PeticionCrearCategoria.builder().nombreCategoria("Arte").build();
        given(categoriaRepository.existsByNombreCategoriaIgnoreCase("Arte")).willReturn(true);

        assertThatThrownBy(() -> categoriaServicio.crearCategoria(null, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class);
    }

    @Test
    @DisplayName("crearCategoria creada por un creador queda sin revisar y con dueño")
    void crearCategoria_deUnCreador_quedaSinRevisar() {
        PeticionCrearCategoria peticion = PeticionCrearCategoria.builder().nombreCategoria("Ceramica").build();
        given(categoriaRepository.existsByNombreCategoriaIgnoreCase("Ceramica")).willReturn(false);
        given(usuarioRepository.findById(9L)).willReturn(Optional.of(creador));
        given(categoriaRepository.save(any(Categoria.class))).willAnswer(inv -> inv.getArgument(0));

        RespuestaCategoria respuesta = categoriaServicio.crearCategoria(9L, peticion);

        assertThat(respuesta.getRevisado()).isFalse();
        assertThat(respuesta.getIdUsuarioCreador()).isEqualTo(9L);
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
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("eliminarCategoria rechaza si alguna subcategoria tiene servicios publicados")
    void eliminarCategoria_rechazaConServiciosDependientes() {
        given(categoriaRepository.findById(1L)).willReturn(Optional.of(categoria));
        given(servicioSubcategoriaRepository.existsBySubcategoriaCategoriaIdCategoria(1L)).willReturn(true);

        assertThatThrownBy(() -> categoriaServicio.eliminarCategoria(1L, null))
                .isInstanceOf(ExcepcionReglaNegocio.class);
        verify(categoriaRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("eliminarCategoria de un creador exige motivo y lo notifica al borrar")
    void eliminarCategoria_deUnCreador_exigeMotivoYNotifica() {
        Categoria deCreador = Categoria.builder().idCategoria(2L).nombreCategoria("Ceramica").creador(creador).revisado(false).build();
        given(categoriaRepository.findById(2L)).willReturn(Optional.of(deCreador));

        assertThatThrownBy(() -> categoriaServicio.eliminarCategoria(2L, null))
                .isInstanceOf(ExcepcionReglaNegocio.class);
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
        Categoria sinRevisar = Categoria.builder().idCategoria(3L).nombreCategoria("Ceramica").creador(creador).revisado(false).build();
        given(categoriaRepository.findById(3L)).willReturn(Optional.of(sinRevisar));
        given(categoriaRepository.save(any(Categoria.class))).willAnswer(inv -> inv.getArgument(0));

        RespuestaCategoria respuesta = categoriaServicio.marcarCategoriaRevisada(3L);

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
        Categoria sinRevisar = Categoria.builder().idCategoria(4L).nombreCategoria("Ceramica").creador(creador).revisado(false).build();
        given(categoriaRepository.findByRevisadoFalseOrderByActualizadoEnDesc()).willReturn(List.of(sinRevisar));

        assertThat(categoriaServicio.listarCategoriasPendientesRevision()).hasSize(1);
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

        RespuestaSubcategoria respuesta = categoriaServicio.crearSubcategoria(null, peticion);

        assertThat(respuesta.getNombreSubcategoria()).isEqualTo("Fotografia");
        assertThat(respuesta.getRevisado()).isTrue();
    }

    @Test
    @DisplayName("crearSubcategoria de un creador queda sin revisar y con dueño")
    void crearSubcategoria_deUnCreador_quedaSinRevisar() {
        PeticionCrearSubcategoria peticion = PeticionCrearSubcategoria.builder().idCategoria(1L).nombreSubcategoria("Fotografia").build();
        given(categoriaRepository.findById(1L)).willReturn(Optional.of(categoria));
        given(subcategoriaRepository.existsByCategoriaIdCategoriaAndNombreSubcategoriaIgnoreCase(1L, "Fotografia")).willReturn(false);
        given(usuarioRepository.findById(9L)).willReturn(Optional.of(creador));
        given(subcategoriaRepository.save(any(Subcategoria.class))).willAnswer(inv -> inv.getArgument(0));

        RespuestaSubcategoria respuesta = categoriaServicio.crearSubcategoria(9L, peticion);

        assertThat(respuesta.getRevisado()).isFalse();
        assertThat(respuesta.getIdUsuarioCreador()).isEqualTo(9L);
    }

    @Test
    @DisplayName("crearSubcategoria lanza recurso no encontrado si la categoria no existe")
    void crearSubcategoria_categoriaInexistente() {
        PeticionCrearSubcategoria peticion = PeticionCrearSubcategoria.builder().idCategoria(1L).nombreSubcategoria("Fotografia").build();
        given(categoriaRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaServicio.crearSubcategoria(null, peticion))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("crearSubcategoria rechaza un nombre repetido dentro de la misma categoria")
    void crearSubcategoria_rechazaDuplicado() {
        PeticionCrearSubcategoria peticion = PeticionCrearSubcategoria.builder().idCategoria(1L).nombreSubcategoria("Ilustracion").build();
        given(categoriaRepository.findById(1L)).willReturn(Optional.of(categoria));
        given(subcategoriaRepository.existsByCategoriaIdCategoriaAndNombreSubcategoriaIgnoreCase(1L, "Ilustracion")).willReturn(true);

        assertThatThrownBy(() -> categoriaServicio.crearSubcategoria(null, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class);
    }

    @Test
    @DisplayName("eliminarSubcategoria borra cuando existe y no tiene dueño, sin exigir motivo")
    void eliminarSubcategoria_borraCuandoExiste() {
        given(subcategoriaRepository.findById(1L)).willReturn(Optional.of(
                Subcategoria.builder().idSubcategoria(1L).categoria(categoria).nombreSubcategoria("Ilustracion").build()));

        categoriaServicio.eliminarSubcategoria(1L, null);

        verify(subcategoriaRepository).deleteById(1L);
        verify(notificacionService, never()).notificar(any(), any(), any());
    }

    @Test
    @DisplayName("eliminarSubcategoria lanza recurso no encontrado si no existe")
    void eliminarSubcategoria_inexistente() {
        given(subcategoriaRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> categoriaServicio.eliminarSubcategoria(1L, null))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("eliminarSubcategoria rechaza si tiene servicios publicados")
    void eliminarSubcategoria_rechazaConServiciosDependientes() {
        given(subcategoriaRepository.findById(1L)).willReturn(Optional.of(
                Subcategoria.builder().idSubcategoria(1L).categoria(categoria).nombreSubcategoria("Ilustracion").build()));
        given(servicioSubcategoriaRepository.existsBySubcategoriaIdSubcategoria(1L)).willReturn(true);

        assertThatThrownBy(() -> categoriaServicio.eliminarSubcategoria(1L, null))
                .isInstanceOf(ExcepcionReglaNegocio.class);
        verify(subcategoriaRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("eliminarSubcategoria de un creador exige motivo y lo notifica al borrar")
    void eliminarSubcategoria_deUnCreador_exigeMotivoYNotifica() {
        Subcategoria deCreador = Subcategoria.builder().idSubcategoria(5L).categoria(categoria)
                .nombreSubcategoria("Retratos").creador(creador).revisado(false).build();
        given(subcategoriaRepository.findById(5L)).willReturn(Optional.of(deCreador));

        assertThatThrownBy(() -> categoriaServicio.eliminarSubcategoria(5L, " "))
                .isInstanceOf(ExcepcionReglaNegocio.class);
        verify(subcategoriaRepository, never()).deleteById(any());

        categoriaServicio.eliminarSubcategoria(5L, "No corresponde a esta categoria");

        verify(subcategoriaRepository).deleteById(5L);
        verify(notificacionService, times(1)).notificar(
                org.mockito.ArgumentMatchers.eq(creador),
                org.mockito.ArgumentMatchers.eq("SUBCATEGORIA_ELIMINADA"),
                org.mockito.ArgumentMatchers.contains("No corresponde a esta categoria"));
    }
}
