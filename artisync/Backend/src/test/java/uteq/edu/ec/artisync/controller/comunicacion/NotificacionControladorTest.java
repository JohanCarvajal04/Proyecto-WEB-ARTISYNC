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
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaNotificacion;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.comunicacion.NotificacionService;

import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificacionControladorTest {

    @Mock
    private NotificacionService notificacionService;

    @InjectMocks
    private NotificacionControlador controlador;

    private CustomUserDetails mockUserDetails() {
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getIdUsuario()).thenReturn(1L);
        return user;
    }

    @Test
    void listarMisNotificaciones_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        Pageable pageable = mock(Pageable.class);
        Page<RespuestaNotificacion> page = new PageImpl<>(Collections.emptyList());
        when(notificacionService.listarMisNotificaciones(1L, pageable)).thenReturn(page);

        ResponseEntity<Page<RespuestaNotificacion>> res = controlador.listarMisNotificaciones(user, pageable);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(page);
    }

    @Test
    void marcarComoLeida_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        RespuestaNotificacion respuesta = new RespuestaNotificacion();
        when(notificacionService.marcarComoLeida(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaNotificacion> res = controlador.marcarComoLeida(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void marcarTodasLeidas_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        when(notificacionService.marcarTodasLeidas(1L)).thenReturn(5);

        ResponseEntity<RespuestaMensaje> res = controlador.marcarTodasLeidas(user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().mensaje()).isEqualTo("5 notificaciones marcadas como leídas");
    }

    @Test
    void contarNoLeidas_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        when(notificacionService.contarNoLeidas(1L)).thenReturn(10L);

        ResponseEntity<Map<String, Long>> res = controlador.contarNoLeidas(user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).containsEntry("noLeidas", 10L);
    }
}
