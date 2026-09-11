package uteq.edu.ec.artisync.service.comunicacion.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.dto.peticion.comunicacion.CreateBriefingTemplateRequest;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.BriefingResponse;
import uteq.edu.ec.artisync.entity.catalogo.Offering;
import uteq.edu.ec.artisync.entity.comunicacion.*;
import uteq.edu.ec.artisync.entity.pedido.Order;
import uteq.edu.ec.artisync.entity.perfil.CreatorProfile;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.comunicacion.*;
import uteq.edu.ec.artisync.repository.perfil.CreatorProfileRepository;

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

    @Mock private BriefingTemplateRepository plantillaRepo;
    @Mock private SentBriefingRepository   enviadoRepo;
    @Mock private BriefingAnswerRepository respuestaRepo;
    @Mock private CreatorProfileRepository     perfilRepo;

    @InjectMocks
    private BriefingServiceImpl briefingService;

    private CreatorProfile perfilCreador;
    private User       usuarioCreador;
    private Order        pedido;

    @BeforeEach
    void setUp() {
        usuarioCreador = User.builder()
                .idUsuario(1L)
                .nombres("Ana")
                .apellidos("Creadora")
                .correo("ana@example.com")
                .build();

        perfilCreador = CreatorProfile.builder()
                .idPerfil(5L)
                .usuario(usuarioCreador)
                .build();

        // OrderOwnershipValidator evalúa cliente y creador sin cortocircuito,
        // así que el pedido necesita un servicio/perfil/usuario completos aunque
        // el caso bajo prueba solo ejercite la ruta del cliente.
        Offering servicioPedido = Offering.builder().idServicio(1L).perfil(perfilCreador).build();
        pedido = Order.builder()
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
        CreateBriefingTemplateRequest peticion = CreateBriefingTemplateRequest.builder()
                .nombrePlantilla("Briefing Logo")
                .preguntas(List.of(
                        new CreateBriefingTemplateRequest.PreguntaRequest("¿Colores preferidos?", 1),
                        new CreateBriefingTemplateRequest.PreguntaRequest("¿Estilo de diseño?", 2)
                ))
                .build();

        BriefingTemplate plantilla = BriefingTemplate.builder()
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
        when(plantillaRepo.save(any(BriefingTemplate.class))).thenReturn(plantilla);

        BriefingResponse respuesta = briefingService.crearPlantilla(1L, peticion);

        assertThat(respuesta.getNombrePlantilla()).isEqualTo("Briefing Logo");
        verify(plantillaRepo, times(2)).save(any(BriefingTemplate.class));
    }

    @Test
    @DisplayName("crearPlantilla — más de 10 preguntas lanza excepción")
    void crearPlantilla_masDeMaxPreguntas_lanzaExcepcion() {
        List<CreateBriefingTemplateRequest.PreguntaRequest> muchasPreguntas = new ArrayList<>();
        for (int i = 1; i <= 11; i++) {
            muchasPreguntas.add(new CreateBriefingTemplateRequest.PreguntaRequest("Pregunta " + i, i));
        }

        CreateBriefingTemplateRequest peticion = CreateBriefingTemplateRequest.builder()
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
        CreateBriefingTemplateRequest peticion = CreateBriefingTemplateRequest.builder()
                .nombrePlantilla("X").preguntas(List.of(new CreateBriefingTemplateRequest.PreguntaRequest("¿?", 1)))
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
                BriefingTemplate.builder().idBriefingPlantilla(1L).perfilCreador(perfilCreador)
                        .nombrePlantilla("Briefing Logo").preguntas(new ArrayList<>()).build()
        ));

        List<BriefingResponse> resultado = briefingService.obtenerMisPlantillas(1L);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombrePlantilla()).isEqualTo("Briefing Logo");
    }

    @Test
    @DisplayName("editarPlantilla — el dueño real (por id_perfil) puede editar su plantilla")
    void editarPlantilla_dueno_puedeEditar() {
        BriefingTemplate plantilla = BriefingTemplate.builder()
                .idBriefingPlantilla(1L).perfilCreador(perfilCreador)
                .nombrePlantilla("Vieja").preguntas(new ArrayList<>()).build();
        CreateBriefingTemplateRequest peticion = CreateBriefingTemplateRequest.builder()
                .nombrePlantilla("Nueva").preguntas(List.of(new CreateBriefingTemplateRequest.PreguntaRequest("¿?", 1)))
                .build();

        when(perfilRepo.findByUsuarioIdUsuario(1L)).thenReturn(Optional.of(perfilCreador));
        when(plantillaRepo.findById(1L)).thenReturn(Optional.of(plantilla));
        when(plantillaRepo.save(any(BriefingTemplate.class))).thenReturn(plantilla);

        BriefingResponse respuesta = briefingService.editarPlantilla(1L, 1L, peticion);

        assertThat(respuesta.getNombrePlantilla()).isEqualTo("Nueva");
    }

    @Test
    @DisplayName("editarPlantilla — un creador ajeno (otro id_perfil) no puede editarla")
    void editarPlantilla_ajeno_rechaza() {
        BriefingTemplate plantilla = BriefingTemplate.builder()
                .idBriefingPlantilla(1L).perfilCreador(perfilCreador)
                .nombrePlantilla("Vieja").preguntas(new ArrayList<>()).build();
        CreatorProfile otroPerfil = CreatorProfile.builder().idPerfil(6L)
                .usuario(User.builder().idUsuario(2L).build()).build();

        when(perfilRepo.findByUsuarioIdUsuario(2L)).thenReturn(Optional.of(otroPerfil));
        when(plantillaRepo.findById(1L)).thenReturn(Optional.of(plantilla));

        CreateBriefingTemplateRequest peticion = CreateBriefingTemplateRequest.builder()
                .nombrePlantilla("Nueva").preguntas(List.of()).build();

        assertThatThrownBy(() -> briefingService.editarPlantilla(1L, 2L, peticion))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("permiso");
    }

    @Test
    @DisplayName("eliminarPlantilla — el dueño real (por id_perfil) puede eliminar su plantilla")
    void eliminarPlantilla_dueno_puedeEliminar() {
        BriefingTemplate plantilla = BriefingTemplate.builder()
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
        SentBriefing enviado = SentBriefing.builder()
                .idBriefingEnviado(20L)
                .pedido(pedido) // cliente idUsuario=2
                .plantilla(BriefingTemplate.builder().preguntas(new ArrayList<>()).build())
                .completado(false)
                .build();
        when(enviadoRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(enviado));

        assertThat(briefingService.obtenerBriefing(10L, 2L)).isNotNull();
    }

    @Test
    @DisplayName("obtenerBriefing — el creador del servicio puede consultarlo")
    void obtenerBriefing_creador_puedeConsultar() {
        SentBriefing enviado = SentBriefing.builder()
                .idBriefingEnviado(20L)
                .pedido(pedido) // servicio.perfil.usuario idUsuario=1 (usuarioCreador)
                .plantilla(BriefingTemplate.builder().preguntas(new ArrayList<>()).build())
                .completado(false)
                .build();
        when(enviadoRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(enviado));

        assertThat(briefingService.obtenerBriefing(10L, 1L)).isNotNull();
    }

    @Test
    @DisplayName("obtenerBriefing — usuario ajeno al pedido recibe AccessDenied")
    void obtenerBriefing_usuarioAjeno_lanzaAccessDenied() {
        SentBriefing enviado = SentBriefing.builder()
                .idBriefingEnviado(20L)
                .pedido(pedido) // cliente=2, creador=1
                .completado(false)
                .build();
        when(enviadoRepo.findByPedidoIdPedido(10L)).thenReturn(Optional.of(enviado));

        assertThatThrownBy(() -> briefingService.obtenerBriefing(10L, 999L))
                .isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }

}
