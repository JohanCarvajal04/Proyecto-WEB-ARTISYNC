package uteq.edu.ec.artisync.controller.legal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaEntregable;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.legal.IEntregableServicio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EntregableControladorTest {

    @Mock
    private IEntregableServicio entregableServicio;

    @InjectMocks
    private EntregableControlador controlador;

    private CustomUserDetails mockUserDetails() {
        CustomUserDetails user = mock(CustomUserDetails.class);
        when(user.getIdUsuario()).thenReturn(1L);
        return user;
    }

    @Test
    void subirEntregable_devuelveCreated() {
        CustomUserDetails user = mockUserDetails();
        MultipartFile marcaAgua = mock(MultipartFile.class);
        MultipartFile limpia = mock(MultipartFile.class);
        RespuestaEntregable respuesta = new RespuestaEntregable();

        when(entregableServicio.subirEntregable(10L, 1L, marcaAgua, limpia)).thenReturn(respuesta);

        ResponseEntity<RespuestaEntregable> res = controlador.subirEntregable(10L, user, marcaAgua, limpia);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void obtenerEntregable_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        RespuestaEntregable respuesta = new RespuestaEntregable();

        when(entregableServicio.obtenerEntregable(10L, 1L)).thenReturn(respuesta);

        ResponseEntity<RespuestaEntregable> res = controlador.obtenerEntregable(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEqualTo(respuesta);
    }

    @Test
    void aprobarEntrega_devuelveOk() {
        CustomUserDetails user = mockUserDetails();

        ResponseEntity<RespuestaMensaje> res = controlador.aprobarEntrega(10L, user);
        verify(entregableServicio).aprobarEntrega(10L, 1L);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody().getMensaje()).contains("aprobada exitosamente");
    }

    @Test
    void descargarVersionLimpia_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        IEntregableServicio.ArchivoDescargado archivo = new IEntregableServicio.ArchivoDescargado(
                new byte[]{1, 2}, "limpia.png", "image/png"
        );
        when(entregableServicio.descargarVersionLimpia(10L, 1L)).thenReturn(archivo);

        ResponseEntity<byte[]> res = controlador.descargarVersionLimpia(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).contains("attachment");
        assertThat(res.getBody()).isEqualTo(archivo.contenido());
    }

    @Test
    void descargarVersionMarcaAgua_devuelveOk() {
        CustomUserDetails user = mockUserDetails();
        IEntregableServicio.ArchivoDescargado archivo = new IEntregableServicio.ArchivoDescargado(
                new byte[]{3, 4}, "marca.jpg", "image/jpeg"
        );
        when(entregableServicio.descargarVersionMarcaAgua(10L, 1L)).thenReturn(archivo);

        ResponseEntity<byte[]> res = controlador.descargarVersionMarcaAgua(10L, user);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION)).contains("attachment");
        assertThat(res.getBody()).isEqualTo(archivo.contenido());
    }
}
