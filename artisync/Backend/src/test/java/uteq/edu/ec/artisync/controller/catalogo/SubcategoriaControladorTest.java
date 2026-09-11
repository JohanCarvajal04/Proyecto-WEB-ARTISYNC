package uteq.edu.ec.artisync.controller.catalogo;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import uteq.edu.ec.artisync.dto.peticion.catalogo.PeticionCrearSubcategoria;
import uteq.edu.ec.artisync.dto.respuesta.catalogo.RespuestaSubcategoria;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.catalogo.ICategoriaServicio;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SubcategoriaControladorTest {

    @Mock
    private ICategoriaServicio categoriaServicio;

    @InjectMocks
    private SubcategoriaControlador controlador;

    private CustomUserDetails usuario(Long idUsuario, String... authorities) {
        List<GrantedAuthority> concedidas = List.of(authorities).stream()
                .map(a -> (GrantedAuthority) new SimpleGrantedAuthority(a))
                .toList();
        return new CustomUserDetails(idUsuario, "usuario", "x", true, true, true, true, concedidas);
    }

    @Test
    void listarTodasLasSubcategorias_DebeRetornarLista() {
        RespuestaSubcategoria sub = RespuestaSubcategoria.builder().idSubcategoria(1L).build();
        when(categoriaServicio.listarTodasLasSubcategorias()).thenReturn(List.of(sub));

        ResponseEntity<List<RespuestaSubcategoria>> result = controlador.listarTodasLasSubcategorias();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
    }

    @Test
    void crearSubcategoria_ModeradorConCategoriaGestionar_QuedaSinCreadorAsignado() {
        PeticionCrearSubcategoria peticion = PeticionCrearSubcategoria.builder()
                .idCategoria(1L).nombreSubcategoria("Nueva").build();
        CustomUserDetails moderador = usuario(9L, "CATEGORIA_GESTIONAR");
        when(categoriaServicio.crearSubcategoria(isNull(), eq(peticion)))
                .thenReturn(RespuestaSubcategoria.builder().idSubcategoria(2L).idUsuarioCreador(null).build());

        ResponseEntity<RespuestaSubcategoria> result = controlador.crearSubcategoria(peticion, moderador);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getIdUsuarioCreador()).isNull();
    }

    @Test
    void crearSubcategoria_CreadorSinPermisoDeGestion_QuedaAsociadaASuUsuario() {
        PeticionCrearSubcategoria peticion = PeticionCrearSubcategoria.builder()
                .idCategoria(1L).nombreSubcategoria("Nueva").build();
        CustomUserDetails creador = usuario(9L, "CATEGORIA_CREAR");
        when(categoriaServicio.crearSubcategoria(eq(9L), eq(peticion)))
                .thenReturn(RespuestaSubcategoria.builder().idSubcategoria(3L).idUsuarioCreador(9L).build());

        ResponseEntity<RespuestaSubcategoria> result = controlador.crearSubcategoria(peticion, creador);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody().getIdUsuarioCreador()).isEqualTo(9L);
    }

    @Test
    void eliminarSubcategoria_ConMotivo_DebeDelegarAlServicio() {
        ResponseEntity<RespuestaMensaje> result = controlador.eliminarSubcategoria(1L, "duplicada");

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getMensaje()).contains("eliminada");
    }

    @Test
    void eliminarSubcategoria_SinMotivo_DebeDelegarAlServicio() {
        ResponseEntity<RespuestaMensaje> result = controlador.eliminarSubcategoria(1L, null);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void listarPendientesRevision_DebeRetornarLista() {
        when(categoriaServicio.listarSubcategoriasPendientesRevision())
                .thenReturn(List.of(RespuestaSubcategoria.builder().idSubcategoria(4L).revisado(false).build()));

        ResponseEntity<List<RespuestaSubcategoria>> result = controlador.listarPendientesRevision();

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
    }

    @Test
    void marcarRevisada_DebeRetornarLaSubcategoriaActualizada() {
        when(categoriaServicio.marcarSubcategoriaRevisada(5L))
                .thenReturn(RespuestaSubcategoria.builder().idSubcategoria(5L).revisado(true).build());

        ResponseEntity<RespuestaSubcategoria> result = controlador.marcarRevisada(5L);

        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody().getRevisado()).isTrue();
    }
}
