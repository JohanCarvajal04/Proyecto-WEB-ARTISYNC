package uteq.edu.ec.artisync.controller.perfil;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaPortafolio;
import uteq.edu.ec.artisync.dto.respuesta.perfil.RespuestaPortafolioItem;
import uteq.edu.ec.artisync.service.perfil.IPortafolioItemServicio;
import uteq.edu.ec.artisync.service.perfil.IPortafolioServicio;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Las rutas de items cuelgan del mismo prefijo que PortafolioControlador, que
 * ya expone "/{id}". Un choque entre "/items/{idItem}" y "/{id}" haría que
 * Spring intentara convertir "items" a Long y respondiera 400 en tiempo de
 * ejecución: los tests unitarios de controlador, que llaman al método directo,
 * no lo detectarían. Por eso aquí se levanta el despachador real.
 */
@ExtendWith(MockitoExtension.class)
class PortafolioItemControladorRutasTest {

    @Mock private IPortafolioItemServicio itemServicio;
    @Mock private IPortafolioServicio portafolioServicio;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(
                        new PortafolioItemControlador(itemServicio),
                        new PortafolioControlador(portafolioServicio))
                // standaloneSetup no trae la integración con Spring Security: sin
                // esto, @AuthenticationPrincipal se intentaría enlazar como
                // atributo de modelo y la petición fallaría con 400.
                .setCustomArgumentResolvers(new AuthenticationPrincipalArgumentResolver())
                .build();
    }

    @Test
    void obtenerItem_noLoCapturaLaRutaDePortafolioPorId() throws Exception {
        when(itemServicio.obtenerItem(eq(11L), any()))
                .thenReturn(RespuestaPortafolioItem.builder().idItemPortafolio(11L).build());

        mockMvc.perform(get("/api/v1/portafolios/items/11"))
                .andExpect(status().isOk());

        verify(itemServicio).obtenerItem(eq(11L), any());
        verifyNoInteractions(portafolioServicio);
    }

    @Test
    void listarItems_resuelveALaRutaDeItemsYNoALaDeDetalle() throws Exception {
        when(itemServicio.listarItems(eq(3L), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/portafolios/3/items"))
                .andExpect(status().isOk());

        verify(itemServicio).listarItems(eq(3L), any());
    }

    @Test
    void descargarArchivo_resuelveYDevuelveElContentTypeDelArchivo() throws Exception {
        when(itemServicio.descargarArchivo(eq(11L), any()))
                .thenReturn(new IPortafolioItemServicio.ArchivoItem(
                        "video".getBytes(), "obra-11.mp4", "video/mp4"));

        mockMvc.perform(get("/api/v1/portafolios/items/11/archivo"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String tipo = result.getResponse().getContentType();
                    if (tipo == null || !tipo.startsWith("video/mp4")) {
                        throw new AssertionError("Content-Type inesperado: " + tipo);
                    }
                });
    }

    /**
     * Reproduce exactamente lo que envía el frontend: la parte "datos" como
     * application/json y "archivo" como binario. Si la parte JSON llegara como
     * text/plain —que es lo que produce un FormData.append con un string—
     * @RequestPart no encontraría convertidor y respondería 415.
     */
    @Test
    void subirItem_aceptaElMultipartConLaParteJsonQueEnviaElFrontend() throws Exception {
        autenticarComo(1L);
        try {
            when(itemServicio.subirItem(eq(3L), eq(1L), any(), any()))
                    .thenReturn(RespuestaPortafolioItem.builder().idItemPortafolio(11L).build());

            MockMultipartFile datos = new MockMultipartFile(
                    "datos", "", "application/json",
                    "{\"tituloObra\":\"Mi obra\",\"descripcionObra\":\"Una descripcion\"}".getBytes());
            MockMultipartFile archivo = new MockMultipartFile(
                    "archivo", "obra.png", "image/png", "bytes".getBytes());

            mockMvc.perform(multipart("/api/v1/portafolios/3/items").file(datos).file(archivo))
                    .andExpect(status().isCreated());

            // Que los metadatos lleguen deserializados prueba que la parte JSON
            // se resolvió: con text/plain la petición ni habría entrado al método.
            verify(itemServicio).subirItem(eq(3L), eq(1L),
                    argThat(datosRecibidos -> "Mi obra".equals(datosRecibidos.tituloObra())), any());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    /** El resolutor de @AuthenticationPrincipal lee del SecurityContextHolder. */
    private void autenticarComo(Long idUsuario) {
        CustomUserDetails usuario = new CustomUserDetails(
                idUsuario, "creador@test.dev", "x", true, true, true, true, List.of());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(usuario, "x", usuario.getAuthorities()));
    }

    /** La ruta preexistente debe seguir funcionando igual. */
    @Test
    void obtenerPortafolioPorId_sigueResolviendoASuControladorOriginal() throws Exception {
        when(portafolioServicio.obtenerPortafolioPorId(3L))
                .thenReturn(RespuestaPortafolio.builder().idPortafolio(3L).build());

        mockMvc.perform(get("/api/v1/portafolios/3"))
                .andExpect(status().isOk());

        verify(portafolioServicio).obtenerPortafolioPorId(3L);
        verifyNoInteractions(itemServicio);
    }
}
