package uteq.edu.ec.artisync.service.comunicacion.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.PeticionCrearBriefingPlantilla;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaBriefing;
import uteq.edu.ec.artisync.entity.catalogo.Servicio;
import uteq.edu.ec.artisync.entity.comunicacion.*;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.comunicacion.*;
import uteq.edu.ec.artisync.repository.perfil.PerfilCreadorRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para BriefingServiceImpl.
 * REQ-F-016 ampliado: el envío y la respuesta del cuestionario se prueban en
 * PedidoServicioImplBriefingTest (ahora ocurren al crear el pedido); aquí
 * queda la gestión de plantillas del creador y la lectura de respuestas.
 */
@ExtendWith(MockitoExtension.class)
class BriefingServiceImplTest {

    @Mock private BriefingPlantillaRepository plantillaRepo;
    @Mock private BriefingEnviadoRepository   enviadoRepo;
    @Mock private BriefingRespuestaRepository respuestaRepo;
    @Mock private PerfilCreadorRepository     perfilRepo;

    @InjectMocks
    private BriefingServiceImpl briefingService;

    private PerfilCreador perfilCreador;
    private User       usuarioCreador;
    private Pedido        pedido;

    @BeforeEach
    void setUp() {
        usuarioCreador = User.builder()
                .idUsuario(1L)
                .nombres("Ana")
                .apellidos("Creadora")
                .correo("ana@example.com")
                .build();

        perfilCreador = PerfilCreador.builder()
                .idPerfil(5L)
                .usuario(usuarioCreador)
                .build();

        // OrderOwnershipValidator evalúa cliente y creador sin cortocircuito,
        // así que el pedido necesita un servicio/perfil/usuario completos aunque
        // el caso bajo prueba solo ejercite la ruta del cliente.
        Servicio servicioPedido = Servicio.builder().idServicio(1L).perfil(perfilCreador).build();
        pedido = Pedido.builder()
                .idPedido(10L)
                .usuarioCliente(User.builder().idUsuario(2L).build())
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

        // idUsuario (1L) != idPerfil (5L) a propósito: si crearPlantilla buscara
        // el perfil con findById(idUsuario) en vez de findByUsuarioIdUsuario,
        // este stub no aplicaría y el test fallaría con "Perfil creador no
        // encontrado" — así queda protegida la regresión real que reportó el
        // creador ("no me deja guardar un cuestionario").
        when(perfilRepo.findByUsuarioIdUsuario(1L)).thenReturn(Optional.of(perfilCreador));
        when(plantillaRepo.save(any(BriefingPlantilla.class))).thenReturn(plantilla);

        RespuestaBriefing respuesta = briefingService.crearPlantilla(1L, peticion);

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

        when(perfilRepo.findByUsuarioIdUsuario(1L)).thenReturn(Optional.of(perfilCreador));

        assertThatThrownBy(() -> briefingService.crearPlantilla(1L, peticion))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("10 preguntas");
    }

    @Test
    @DisplayName("crearPlantilla — usuario sin perfil de creador lanza ResourceNotFoundException")
    void crearPlantilla_sinPerfilCreador_lanzaExcepcion() {
        PeticionCrearBriefingPlantilla peticion = PeticionCrearBriefingPlantilla.builder()
                .nombrePlantilla("X").preguntas(List.of(new PeticionCrearBriefingPlantilla.PreguntaRequest("¿?", 1)))
                .build();
        when(perfilRepo.findByUsuarioIdUsuario(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> briefingService.crearPlantilla(999L, peticion))
                .isInstanceOf(uteq.edu.ec.artisync.exception.ResourceNotFoundException.class);
    }

    // =========================================================================
    // obtenerMisPlantillas / editarPlantilla / eliminarPlantilla
    // =========================================================================

    @Test
    @DisplayName("obtenerMisPlantillas — filtra por el id_perfil real del usuario autenticado, no por su id_usuario")
    void obtenerMisPlantillas_resuelvePerfilPorUsuario() {
        when(perfilRepo.findByUsuarioIdUsuario(1L)).thenReturn(Optional.of(perfilCreador));
        when(plantillaRepo.findByPerfilCreadorIdPerfil(5L)).thenReturn(List.of(
                BriefingPlantilla.builder().idBriefingPlantilla(1L).perfilCreador(perfilCreador)
                        .nombrePlantilla("Briefing Logo").preguntas(new ArrayList<>()).build()
        ));

        List<RespuestaBriefing> resultado = briefingService.obtenerMisPlantillas(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombrePlantilla()).isEqualTo("Briefing Logo");
    }

    @Test
    @DisplayName("editarPlantilla — el dueño real (por id_perfil) puede editar su plantilla")
    void editarPlantilla_dueno_puedeEditar() {
        BriefingPlantilla plantilla = BriefingPlantilla.builder()
                .idBriefingPlantilla(1L).perfilCreador(perfilCreador)
                .nombrePlantilla("Vieja").preguntas(new ArrayList<>()).build();
        PeticionCrearBriefingPlantilla peticion = PeticionCrearBriefingPlantilla.builder()
                .nombrePlantilla("Nueva").preguntas(List.of(new PeticionCrearBriefingPlantilla.PreguntaRequest("¿?", 1)))
                .build();

        when(perfilRepo.findByUsuarioIdUsuario(1L)).thenReturn(Optional.of(perfilCreador));
        when(plantillaRepo.findById(1L)).thenReturn(Optional.of(plantilla));
        when(plantillaRepo.save(any(BriefingPlantilla.class))).thenReturn(plantilla);

        RespuestaBriefing respuesta = briefingService.editarPlantilla(1L, 1L, peticion);

        assertThat(respuesta.getNombrePlantilla()).isEqualTo("Nueva");
    }

    @Test
    @DisplayName("editarPlantilla — un creador ajeno (otro id_perfil) no puede editarla")
    void editarPlantilla_ajeno_rechaza() {
        BriefingPlantilla plantilla = BriefingPlantilla.builder()
                .idBriefingPlantilla(1L).perfilCreador(perfilCreador)
                .nombrePlantilla("Vieja").preguntas(new ArrayList<>()).build();
        PerfilCreador otroPerfil = PerfilCreador.builder().idPerfil(6L)
                .usuario(User.builder().idUsuario(2L).build()).build();

        when(perfilRepo.findByUsuarioIdUsuario(2L)).thenReturn(Optional.of(otroPerfil));
        when(plantillaRepo.findById(1L)).thenReturn(Optional.of(plantilla));

        PeticionCrearBriefingPlantilla peticion = PeticionCrearBriefingPlantilla.builder()
                .nombrePlantilla("Nueva").preguntas(List.of()).build();

        assertThatThrownBy(() -> briefingService.editarPlantilla(1L, 2L, peticion))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("permiso");
    }

    @Test
    @DisplayName("eliminarPlantilla — el dueño real (por id_perfil) puede eliminar su plantilla")
    void eliminarPlantilla_dueno_puedeEliminar() {
        BriefingPlantilla plantilla = BriefingPlantilla.builder()
                .idBriefingPlantilla(1L).perfilCreador(perfilCreador).build();

        when(perfilRepo.findByUsuarioIdUsuario(1L)).thenReturn(Optional.of(perfilCreador));
        when(plantillaRepo.findById(1L)).thenReturn(Optional.of(plantilla));

        assertThat(briefingService.eliminarPlantilla(1L, 1L).getMessage()).contains("eliminada");
        verify(plantillaRepo).delete(plantilla);
    }

    // =========================================================================
    // obtenerBriefing (solo lectura)
    // =========================================================================

    @Test
    @DisplayName("obtenerBriefing — briefing inexistente lanza ResourceNotFoundException")
    void obtenerBriefing_noExiste_lanzaExcepcion() {
        when(enviadoRepo.findByPedidoIdPedido(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> briefingService.obtenerBriefing(99L, 2L))
                .isInstanceOf(uteq.edu.ec.artisync.exception.ResourceNotFoundException.class);
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

}
