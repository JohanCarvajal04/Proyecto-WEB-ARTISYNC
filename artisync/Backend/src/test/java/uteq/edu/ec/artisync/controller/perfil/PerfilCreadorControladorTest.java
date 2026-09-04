package uteq.edu.ec.artisync.controller.perfil;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionActualizarPerfil;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionCrearPerfil;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaPerfil;
import uteq.edu.ec.artisync.service.perfil.IPerfilCreadorServicio;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PerfilCreadorControladorTest {

    @Mock
    private IPerfilCreadorServicio perfilServicio;

    @InjectMocks
    private PerfilCreadorControlador controlador;

    private Authentication mockAuthentication(boolean admin) {
        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test@test.com");
        if (admin) {
            org.mockito.Mockito.lenient().when(auth.getAuthorities())
                    .thenAnswer(inv -> List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        } else {
            org.mockito.Mockito.lenient().when(auth.getAuthorities())
                    .thenAnswer(inv -> Collections.emptyList());
        }
        return auth;
    }

    @Test
    void crearPerfil_devuelveCreated() {
        Authentication auth = mockAuthentication(false);
        PeticionCrearPerfil peticion = new PeticionCrearPerfil(1L, "a", "b", "c");
        RespuestaPerfil respuesta = new RespuestaPerfil(1L, 1L, "a", "b", "c", "d", "e", "f", false);
        when(perfilServicio.crearPerfil(peticion, "test@test.com", false)).thenReturn(respuesta);

        ResponseEntity<RespuestaPerfil> res = controlador.crearPerfil(peticion, auth);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerPerfilPorId_devuelveOk() {
        RespuestaPerfil respuesta = new RespuestaPerfil(1L, 1L, "a", "b", "c", "d", "e", "f", false);
        when(perfilServicio.obtenerPerfilPorId(10L)).thenReturn(respuesta);

        ResponseEntity<RespuestaPerfil> res = controlador.obtenerPerfilPorId(10L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerPerfilPorUsuario_devuelveOk() {
        RespuestaPerfil respuesta = new RespuestaPerfil(1L, 1L, "a", "b", "c", "d", "e", "f", false);
        when(perfilServicio.obtenerPerfilPorUsuario(1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaPerfil> res = controlador.obtenerPerfilPorUsuario(1L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void listarPerfiles_devuelveOk() {
        List<RespuestaPerfil> lista = Collections.emptyList();
        when(perfilServicio.listarPerfiles()).thenReturn(lista);

        ResponseEntity<List<RespuestaPerfil>> res = controlador.listarPerfiles();
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void listarPerfilesActivos_devuelveOk() {
        List<RespuestaPerfil> lista = Collections.emptyList();
        when(perfilServicio.listarPerfilesActivos()).thenReturn(lista);

        ResponseEntity<List<RespuestaPerfil>> res = controlador.listarPerfilesActivos();
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(lista);
    }

    @Test
    void actualizarPerfil_admin_devuelveOk() {
        Authentication auth = mockAuthentication(true);
        PeticionActualizarPerfil peticion = new PeticionActualizarPerfil("a", "b", "c");
        RespuestaPerfil respuesta = new RespuestaPerfil(1L, 1L, "a", "b", "c", "d", "e", "f", false);
        when(perfilServicio.actualizarPerfil(10L, peticion, "test@test.com", true)).thenReturn(respuesta);

        ResponseEntity<RespuestaPerfil> res = controlador.actualizarPerfil(10L, peticion, auth);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void eliminarPerfil_devuelveOk() {
        ResponseEntity<RespuestaMensaje> res = controlador.eliminarPerfil(10L);
        verify(perfilServicio).eliminarPerfil(10L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getMensaje()).contains("eliminado exitosamente");
    }
}
