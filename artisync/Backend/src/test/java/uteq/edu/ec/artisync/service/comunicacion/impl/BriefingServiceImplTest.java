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

        pedido = Pedido.builder()
                .idPedido(10L)
                .usuarioCliente(Usuario.builder().idUsuario(2L).build())
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

        assertThatThrownBy(() -> briefingService.obtenerBriefing(99L))
                .isInstanceOf(uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado.class);
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
}
