package uteq.edu.ec.artisync.controller.perfil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import uteq.edu.ec.artisync.dto.peticion.perfil.PeticionCrearPortafolioItem;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaPortafolioItem;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.perfil.IPortafolioItemServicio;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;

import java.util.Collections;
import java.util.List;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortafolioItemControladorTest {

    @Mock
    private IPortafolioItemServicio itemServicio;

    @InjectMocks
    private PortafolioItemControlador portafolioItemControlador;

    private CustomUserDetails mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new CustomUserDetails(1L, "user@test.com", "pass", true, true, true, true, Collections.emptyList());
    }

    @Test
    void subirItem_DebeRetornarCreated() {
        PeticionCrearPortafolioItem peticion = new PeticionCrearPortafolioItem("Titulo", "Desc");
        MockMultipartFile archivo = new MockMultipartFile("archivo", "test.png", "image/png", "fake".getBytes());
        RespuestaPortafolioItem respuesta = new RespuestaPortafolioItem(20L, 1L, "Titulo", "Desc", "url", LocalDateTime.now());

        when(itemServicio.subirItem(eq(1L), eq(1L), any(), any())).thenReturn(respuesta);

        ResponseEntity<RespuestaPortafolioItem> result = portafolioItemControlador.subirItem(1L, mockUser, peticion, archivo);

        assertEquals(HttpStatus.CREATED, result.getStatusCode());
        assertEquals(20L, result.getBody().idItemPortafolio());
    }

    @Test
    void listarItems_DebeRetornarLista() {
        RespuestaPortafolioItem respuesta = new RespuestaPortafolioItem(20L, 1L, "Titulo", "Desc", "url", LocalDateTime.now());
        when(itemServicio.listarItems(eq(1L), eq(1L))).thenReturn(List.of(respuesta));

        ResponseEntity<List<RespuestaPortafolioItem>> result = portafolioItemControlador.listarItems(1L, mockUser);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(1, result.getBody().size());
    }

    @Test
    void obtenerItem_DebeRetornarItem() {
        RespuestaPortafolioItem respuesta = new RespuestaPortafolioItem(20L, 1L, "Titulo", "Desc", "url", LocalDateTime.now());
        when(itemServicio.obtenerItem(eq(10L), eq(1L))).thenReturn(respuesta);

        ResponseEntity<RespuestaPortafolioItem> result = portafolioItemControlador.obtenerItem(10L, mockUser);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertNotNull(result.getBody());
    }

    @Test
    void descargarArchivo_DebeRetornarBytesYHeaderAttachment() {
        IPortafolioItemServicio.ArchivoItem archivoFalso = new IPortafolioItemServicio.ArchivoItem(
                "fake_content".getBytes(),
                "mi_obra.png",
                "image/png"
        );
        when(itemServicio.descargarArchivo(eq(1L), eq(1L))).thenReturn(archivoFalso);

        ResponseEntity<byte[]> result = portafolioItemControlador.descargarArchivo(1L, mockUser);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(MediaType.IMAGE_PNG, result.getHeaders().getContentType());
        String contentDisposition = result.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
        assertNotNull(contentDisposition);
        
        
        assertArrayEquals("fake_content".getBytes(), result.getBody());
    }

    @Test
    void eliminarItem_DebeRetornarOk() {
        ResponseEntity<RespuestaMensaje> result = portafolioItemControlador.eliminarItem(1L, mockUser);
        assertEquals(HttpStatus.OK, result.getStatusCode());
    }
}

