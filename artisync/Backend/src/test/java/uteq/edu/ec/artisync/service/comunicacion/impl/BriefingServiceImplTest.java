package uteq.edu.ec.artisync.service.comunicacion.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionCrearBriefingPlantilla;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionResponderBriefing;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaBriefing;
import uteq.edu.ec.artisync.entity.catalogo.Servicio;
import uteq.edu.ec.artisync.entity.comunicacion.*;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.comunicacion.*;
import uteq.edu.ec.artisync.repository.pedido.PedidoRepository;
import uteq.edu.ec.artisync.repository.perfil.PerfilCreadorRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para BriefingServiceImpl.
 * Verifica la creación de plantillas, envío de briefing e inmutabilidad de respuestas (RF-16).
 */
@ExtendWith(MockitoExtension.class)
class BriefingServiceImplTest {

    @Mock private BriefingPlantillaRepository plantillaRepo;
    @Mock private BriefingPreguntaRepository  preguntaRepo;
    @Mock private BriefingEnviadoRepository   enviadoRepo;
    @Mock private BriefingRespuestaRepository respuestaRepo;
    @Mock private PerfilCreadorRepository     perfilRepo;
    @Mock private PedidoRepository            pedidoRepo;

    @InjectMocks
    private BriefingServiceImpl briefingService;

    private PerfilCreador perfilCreador;
    private Usuario       usuarioCreador;
    private Pedido        pedido;

    @BeforeEach
    void setUp() {
        usuarioCreador = Usuario.builder()
                .idUsuario(1L)
                .nombres("Ana")
                .apellidos("Creadora")
                .correo("ana@example.com")
                .build();

        perfilCreador = PerfilCreador.builder()
                .idPerfil(5L)
                .usuario(usuarioCreador)
                .build();

        // ValidadorPertenenciaPedido evalúa cliente y creador sin cortocircuito,
        // así que el pedido necesita un servicio/perfil/usuario completos aunque
        // el caso bajo prueba solo ejercite la ruta del cliente.
        Servicio servicioPedido = Servicio.builder().idServicio(1L).perfil(perfilCreador).build();
        pedido = Pedido.builder()
                .idPedido(10L)
                .usuarioCliente(Usuario.builder().idUsuario(2L).build())
                .servicio(servicioPedido)
                .build();
    }

    // =========================================================================
    // crearPlantilla
    // =========================================================================

    @Test
    @DisplayName("crearPlantilla — crea plantilla con preguntas correctamente")
    void crearPlantilla_valida_creaExitosamente() {
        PeticionCrearBriefingPlantilla peticion = PeticionCrearBriefingPlantilla.builder()
                .nombrePlantilla("Briefing Logo")
                .preguntas(List.of(
                        new PeticionCrearBriefingPlantilla.PreguntaRequest("¿Colores preferidos?", 1),
                        new PeticionCrearBriefingPlantilla.PreguntaRequest("¿Estilo de diseño?", 2)
                ))
                .build();

        BriefingPlantilla plantilla = BriefingPlantilla.builder()
                .idBriefingPlantilla(1L)
                .perfilCreador(perfilCreador)
                .nombrePlantilla("Briefing Logo")
                .preguntas(new ArrayList<>())
                .build();

        when(perfilRepo.findById(5L)).thenReturn(Optional.of(perfilCreador));
        when(plantillaRepo.save(any(BriefingPlantilla.class))).thenReturn(plantilla);

        RespuestaBriefing respuesta = briefingService.crearPlantilla(5L, peticion);

        assertThat(respuesta.getNombrePlantilla()).isEqualTo("Briefing Logo");
        verify(plantillaRepo, times(2)).save(any(BriefingPlantilla.class));
    }

    @Test
    @DisplayName("crearPlantilla — más de 10 preguntas lanza excepción")
    void crearPlantilla_masDeMaxPreguntas_lanzaExcepcion() {
        List<PeticionCrearBriefingPlantilla.PreguntaRequest> muchasPreguntas = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            muchasPreguntas.add(new PeticionCrearBriefingPlantilla.PreguntaRequest("Pregunta " + i, i));
        }

        PeticionCrearBriefingPlantilla peticion = PeticionCrearBriefingPlantilla.builder()
                .nombrePlantilla("Demasiadas preguntas")
                .preguntas(muchasPreguntas)
                .build();

        when(perfilRepo.findById(5L)).thenReturn(Optional.of(perfilCreador));

        assertThatThrownBy(() -> briefingService.crearPlantilla(5L, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("10 preguntas");
    }

    // =========================================================================
    // responderBriefing — RF-16 (inmutabilidad)
    // =========================================================================

    @Test
    @DisplayName("RF-16: responderBriefing por segunda vez lanza ExcepcionReglaNegocio")
    void responderBriefing_yaCompletado_lanzaExcepcion() {
        BriefingEnviado enviadoCompletado = BriefingEnviado.builder()
                .idBriefingEnviado(20L)
                .pedido(pedido)
                .completado(true)  // Ya fue respondido
                .build();

        when(enviadoRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(enviadoCompletado));

        PeticionResponderBriefing peticion = PeticionResponderBriefing.builder()
                .respuestas(List.of())
                .build();

        assertThatThrownBy(() -> briefingService.responderBriefing(10L, peticion, 2L))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("no pueden modificarse");
    }

    @Test
    @DisplayName("RF-16: responderBriefing — cliente equivocado lanza excepción")
    void responderBriefing_clienteIncorrecto_lanzaExcepcion() {
        BriefingEnviado enviado = BriefingEnviado.builder()
                .idBriefingEnviado(20L)
                .pedido(pedido) // cliente es idUsuario=2
                .completado(false)
                .build();

        when(enviadoRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(enviado));

        PeticionResponderBriefing peticion = PeticionResponderBriefing.builder()
                .respuestas(List.of())
                .build();

        // Intento de responder con usuario 99 (no es el cliente del pedido)
        assertThatThrownBy(() -> briefingService.responderBriefing(10L, peticion, 99L))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("permiso");
    }

    @Test
    @DisplayName("obtenerBriefing — briefing inexistente lanza ExcepcionRecursoNoEncontrado")
    void obtenerBriefing_noExiste_lanzaExcepcion() {
        when(enviadoRepo.findByPedidoIdPedido(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> briefingService.obtenerBriefing(99L, 2L))
                .isInstanceOf(uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("obtenerBriefing — el cliente del pedido puede consultarlo")
    void obtenerBriefing_cliente_puedeConsultar() {
        BriefingEnviado enviado = BriefingEnviado.builder()
                .idBriefingEnviado(20L)
                .pedido(pedido) // cliente idUsuario=2
                .plantilla(BriefingPlantilla.builder().preguntas(new ArrayList<>()).build())
                .completado(false)
                .build();
        when(enviadoRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(enviado));

        assertThat(briefingService.obtenerBriefing(10L, 2L)).isNotNull();
    }

    @Test
    @DisplayName("obtenerBriefing — el creador del servicio puede consultarlo")
    void obtenerBriefing_creador_puedeConsultar() {
        BriefingEnviado enviado = BriefingEnviado.builder()
                .idBriefingEnviado(20L)
                .pedido(pedido) // servicio.perfil.usuario idUsuario=1 (usuarioCreador)
                .plantilla(BriefingPlantilla.builder().preguntas(new ArrayList<>()).build())
                .completado(false)
                .build();
        when(enviadoRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(enviado));

        assertThat(briefingService.obtenerBriefing(10L, 1L)).isNotNull();
    }

    @Test
    @DisplayName("obtenerBriefing — usuario ajeno al pedido recibe AccessDenied")
    void obtenerBriefing_usuarioAjeno_lanzaAccessDenied() {
        BriefingEnviado enviado = BriefingEnviado.builder()
                .idBriefingEnviado(20L)
                .pedido(pedido) // cliente=2, creador=1
                .completado(false)
                .build();
        when(enviadoRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(enviado));

        assertThatThrownBy(() -> briefingService.obtenerBriefing(10L, 999L))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

    @Test
    @DisplayName("enviarBriefing — un solo briefing por pedido (no duplicados)")
    void enviarBriefing_yaExiste_lanzaExcepcion() {
        when(enviadoRepo.existsByPedidoIdPedido(10L)).thenReturn(true);

        uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionEnviarBriefing peticion =
                new uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionEnviarBriefing(1L);

        assertThatThrownBy(() -> briefingService.enviarBriefing(10L, peticion, 1L))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("Ya se envió");
    }

    @Test
    @DisplayName("enviarBriefing — pedido inexistente lanza excepcion")
    void enviarBriefing_pedidoInexistente_lanzaExcepcion() {
        when(enviadoRepo.existsByPedidoIdPedido(10L)).thenReturn(false);
        when(pedidoRepo.findById(10L)).thenReturn(Optional.empty());
        var peticion = new uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionEnviarBriefing(1L);

        assertThatThrownBy(() -> briefingService.enviarBriefing(10L, peticion, 1L))
                .isInstanceOf(uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("enviarBriefing — plantilla inexistente lanza excepcion")
    void enviarBriefing_plantillaInexistente_lanzaExcepcion() {
        when(enviadoRepo.existsByPedidoIdPedido(10L)).thenReturn(false);
        when(pedidoRepo.findById(10L)).thenReturn(Optional.of(pedido));
        when(plantillaRepo.findById(1L)).thenReturn(Optional.empty());
        var peticion = new uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionEnviarBriefing(1L);

        assertThatThrownBy(() -> briefingService.enviarBriefing(10L, peticion, 1L))
                .isInstanceOf(uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("enviarBriefing — creador ajeno a la plantilla no puede usarla")
    void enviarBriefing_creadorAjeno_lanzaExcepcion() {
        BriefingPlantilla plantilla = BriefingPlantilla.builder()
                .idBriefingPlantilla(1L).perfilCreador(perfilCreador).nombrePlantilla("X").build();
        when(enviadoRepo.existsByPedidoIdPedido(10L)).thenReturn(false);
        when(pedidoRepo.findById(10L)).thenReturn(Optional.of(pedido));
        when(plantillaRepo.findById(1L)).thenReturn(Optional.of(plantilla));
        var peticion = new uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionEnviarBriefing(1L);

        assertThatThrownBy(() -> briefingService.enviarBriefing(10L, peticion, 999L))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("permiso");
    }

    @Test
    @DisplayName("enviarBriefing — exito cuando el creador es el dueno de la plantilla")
    void enviarBriefing_exito() {
        BriefingPlantilla plantilla = BriefingPlantilla.builder()
                .idBriefingPlantilla(1L).perfilCreador(perfilCreador).nombrePlantilla("X")
                .preguntas(new ArrayList<>()).build();
        BriefingEnviado guardado = BriefingEnviado.builder()
                .idBriefingEnviado(30L).pedido(pedido).plantilla(plantilla).completado(false).build();
        when(enviadoRepo.existsByPedidoIdPedido(10L)).thenReturn(false);
        when(pedidoRepo.findById(10L)).thenReturn(Optional.of(pedido));
        when(plantillaRepo.findById(1L)).thenReturn(Optional.of(plantilla));
        when(enviadoRepo.save(any(BriefingEnviado.class))).thenReturn(guardado);
        when(respuestaRepo.findByBriefingEnviadoIdBriefingEnviado(30L)).thenReturn(List.of());
        var peticion = new uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionEnviarBriefing(1L);

        RespuestaBriefing respuesta = briefingService.enviarBriefing(10L, peticion, 1L);

        assertThat(respuesta.getIdBriefingEnviado()).isEqualTo(30L);
    }

    @Test
    @DisplayName("responderBriefing — persiste las respuestas y marca el briefing como completado")
    void responderBriefing_exito_persisteRespuestasYCompleta() {
        BriefingPregunta pregunta = BriefingPregunta.builder()
                .idPregunta(1L).textoPregunta("¿Colores?").numeroOrden(1).build();
        BriefingPlantilla plantilla = BriefingPlantilla.builder()
                .idBriefingPlantilla(1L).perfilCreador(perfilCreador)
                .preguntas(List.of(pregunta)).build();
        BriefingEnviado enviado = BriefingEnviado.builder()
                .idBriefingEnviado(20L).pedido(pedido).plantilla(plantilla).completado(false).build();
        PeticionResponderBriefing peticion = PeticionResponderBriefing.builder()
                .respuestas(List.of(PeticionResponderBriefing.RespuestaItem.builder()
                        .idPregunta(1L).textoRespuesta("Azul y blanco").build()))
                .build();

        when(enviadoRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(enviado));
        when(preguntaRepo.findById(1L)).thenReturn(Optional.of(pregunta));
        when(enviadoRepo.save(any(BriefingEnviado.class))).thenReturn(enviado);
        when(respuestaRepo.findByBriefingEnviadoIdBriefingEnviado(20L)).thenReturn(List.of());

        briefingService.responderBriefing(10L, peticion, 2L);

        verify(respuestaRepo).save(any(BriefingRespuesta.class));
        assertThat(enviado.getCompletado()).isTrue();
    }

    @Test
    @DisplayName("responderBriefing — pregunta inexistente lanza excepcion")
    void responderBriefing_preguntaInexistente_lanzaExcepcion() {
        BriefingEnviado enviado = BriefingEnviado.builder()
                .idBriefingEnviado(20L).pedido(pedido).completado(false).build();
        PeticionResponderBriefing peticion = PeticionResponderBriefing.builder()
                .respuestas(List.of(PeticionResponderBriefing.RespuestaItem.builder()
                        .idPregunta(99L).textoRespuesta("x").build()))
                .build();
        when(enviadoRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(enviado));
        when(preguntaRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> briefingService.responderBriefing(10L, peticion, 2L))
                .isInstanceOf(uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("obtenerBriefing — incluye las respuestas ya registradas")
    void obtenerBriefing_incluyeRespuestasExistentes() {
        BriefingPregunta pregunta = BriefingPregunta.builder()
                .idPregunta(1L).textoPregunta("¿Colores?").numeroOrden(1).build();
        BriefingPlantilla plantilla = BriefingPlantilla.builder()
                .idBriefingPlantilla(1L).preguntas(List.of(pregunta)).build();
        BriefingEnviado enviado = BriefingEnviado.builder()
                .idBriefingEnviado(20L).pedido(pedido).plantilla(plantilla).completado(true).build();
        BriefingRespuesta respuestaGuardada = BriefingRespuesta.builder()
                .idRespuesta(1L).pregunta(pregunta).textoRespuesta("Azul").build();

        when(enviadoRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(enviado));
        when(respuestaRepo.findByBriefingEnviadoIdBriefingEnviado(20L)).thenReturn(List.of(respuestaGuardada));

        RespuestaBriefing respuesta = briefingService.obtenerBriefing(10L, 2L);

        assertThat(respuesta.getPreguntas()).hasSize(1);
        assertThat(respuesta.getPreguntas().get(0).getTextoRespuesta()).isEqualTo("Azul");
    }

    // =========================================================================
    // obtenerMisPlantillas / editarPlantilla / eliminarPlantilla
    // =========================================================================

    @Test
    @DisplayName("obtenerMisPlantillas — lista las plantillas del creador")
    void obtenerMisPlantillas_devuelveLista() {
        BriefingPlantilla plantilla = BriefingPlantilla.builder()
                .idBriefingPlantilla(1L).nombrePlantilla("Logo").preguntas(new ArrayList<>()).build();
        when(plantillaRepo.findByPerfilCreadorIdPerfil(5L)).thenReturn(List.of(plantilla));

        assertThat(briefingService.obtenerMisPlantillas(5L)).hasSize(1);
    }

    @Test
    @DisplayName("editarPlantilla — actualiza nombre y preguntas cuando el creador es el dueno")
    void editarPlantilla_exito() {
        BriefingPlantilla plantilla = BriefingPlantilla.builder()
                .idBriefingPlantilla(1L).perfilCreador(perfilCreador).nombrePlantilla("Viejo")
                .preguntas(new ArrayList<>(List.of(BriefingPregunta.builder().idPregunta(9L).build())))
                .build();
        PeticionCrearBriefingPlantilla peticion = PeticionCrearBriefingPlantilla.builder()
                .nombrePlantilla("Nuevo")
                .preguntas(List.of(new PeticionCrearBriefingPlantilla.PreguntaRequest("¿Estilo?", 1)))
                .build();
        when(plantillaRepo.findById(1L)).thenReturn(Optional.of(plantilla));
        when(plantillaRepo.save(any(BriefingPlantilla.class))).thenReturn(plantilla);

        RespuestaBriefing respuesta = briefingService.editarPlantilla(1L, 5L, peticion);

        assertThat(respuesta.getNombrePlantilla()).isEqualTo("Nuevo");
        verify(plantillaRepo).flush();
    }

    @Test
    @DisplayName("editarPlantilla — rechaza a un creador que no es dueno de la plantilla")
    void editarPlantilla_rechazaNoPropietario() {
        BriefingPlantilla plantilla = BriefingPlantilla.builder()
                .idBriefingPlantilla(1L).perfilCreador(perfilCreador).nombrePlantilla("Viejo").build();
        PeticionCrearBriefingPlantilla peticion = PeticionCrearBriefingPlantilla.builder()
                .nombrePlantilla("Nuevo").preguntas(List.of()).build();
        when(plantillaRepo.findById(1L)).thenReturn(Optional.of(plantilla));

        assertThatThrownBy(() -> briefingService.editarPlantilla(1L, 999L, peticion))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("permiso");
    }

    @Test
    @DisplayName("editarPlantilla — plantilla inexistente lanza excepcion")
    void editarPlantilla_inexistente_lanzaExcepcion() {
        when(plantillaRepo.findById(99L)).thenReturn(Optional.empty());
        PeticionCrearBriefingPlantilla peticion = PeticionCrearBriefingPlantilla.builder()
                .nombrePlantilla("Nuevo").preguntas(List.of()).build();

        assertThatThrownBy(() -> briefingService.editarPlantilla(99L, 5L, peticion))
                .isInstanceOf(uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("eliminarPlantilla — elimina cuando el creador es el dueno")
    void eliminarPlantilla_exito() {
        BriefingPlantilla plantilla = BriefingPlantilla.builder()
                .idBriefingPlantilla(1L).perfilCreador(perfilCreador).build();
        when(plantillaRepo.findById(1L)).thenReturn(Optional.of(plantilla));

        briefingService.eliminarPlantilla(1L, 5L);

        verify(plantillaRepo).delete(plantilla);
    }

    @Test
    @DisplayName("eliminarPlantilla — rechaza a un creador que no es dueno")
    void eliminarPlantilla_rechazaNoPropietario() {
        BriefingPlantilla plantilla = BriefingPlantilla.builder()
                .idBriefingPlantilla(1L).perfilCreador(perfilCreador).build();
        when(plantillaRepo.findById(1L)).thenReturn(Optional.of(plantilla));

        assertThatThrownBy(() -> briefingService.eliminarPlantilla(1L, 999L))
                .isInstanceOf(ExcepcionReglaNegocio.class);
        verify(plantillaRepo, never()).delete(any());
    }

    @Test
    @DisplayName("eliminarPlantilla — plantilla inexistente lanza excepcion")
    void eliminarPlantilla_inexistente_lanzaExcepcion() {
        when(plantillaRepo.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> briefingService.eliminarPlantilla(99L, 5L))
                .isInstanceOf(uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado.class);
    }
}
