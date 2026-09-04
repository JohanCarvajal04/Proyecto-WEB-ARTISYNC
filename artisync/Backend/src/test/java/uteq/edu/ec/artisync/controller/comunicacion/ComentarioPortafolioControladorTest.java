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
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionCrearComentario;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaComentario;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.comunicacion.ComentarioPortafolioService;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ComentarioPortafolioControladorTest {

    @Mock
    private ComentarioPortafolioService comentarioService;

    @InjectMocks
    private ComentarioPortafolioControlador controlador;

    private CustomUserDetails mockUserDetails(boolean isAdmin) {
        List<GrantedAuthority> authorities = isAdmin ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN")) : List.of();
        return new CustomUserDetails(1L, "user@test.dev", "x", true, true, true, true, authorities);
    }

    @Test
    void crearComentario_devuelveCreated() {
        CustomUserDetails user = mockUserDetails(false);
        PeticionCrearComentario peticion = new PeticionCrearComentario();
        RespuestaComentario respuesta = new RespuestaComentario();
        when(comentarioService.crearComentario(10L, peticion, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaComentario> res = controlador.crearComentario(10L, peticion, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void listarComentarios_devuelveOk() {
        Pageable pageable = mock(Pageable.class);
        Page<RespuestaComentario> page = new PageImpl<>(Collections.emptyList());
        when(comentarioService.listarComentarios(10L, pageable)).thenReturn(page);

        ResponseEntity<Page<RespuestaComentario>> res = controlador.listarComentarios(10L, pageable);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(page);
    }

    @Test
    void contarComentarios_devuelveOk() {
        when(comentarioService.contarComentarios(10L)).thenReturn(5L);

        ResponseEntity<Map<String, Object>> res = controlador.contarComentarios(10L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).containsEntry("total", 5L);
    }

    @Test
    void eliminarComentario_comoAdmin_devuelveOk() {
        CustomUserDetails user = mockUserDetails(true);
        controlador.eliminarComentario(10L, user);
        verify(comentarioService).eliminarComentario(10L, 1L, true);
    }

    @Test
    void eliminarComentario_noAdmin_devuelveOk() {
        CustomUserDetails user = mockUserDetails(false);
        controlador.eliminarComentario(10L, user);
        verify(comentarioService).eliminarComentario(10L, 1L, false);
    }
}
