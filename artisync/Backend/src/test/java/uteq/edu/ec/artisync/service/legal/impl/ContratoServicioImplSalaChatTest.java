package uteq.edu.ec.artisync.service.legal.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import uteq.edu.ec.artisync.entity.catalogo.Servicio;
import uteq.edu.ec.artisync.entity.legal.Contrato;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.pedido.PlantillaContrato;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.repository.legal.ContratoRepository;
import uteq.edu.ec.artisync.repository.pedido.PedidoRepository;
import uteq.edu.ec.artisync.repository.pedido.PlantillaContratoRepository;
import uteq.edu.ec.artisync.service.comunicacion.ChatService;
import uteq.edu.ec.artisync.service.legal.IPdfGeneracionServicio;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * RF-14: la sala de chat se abre al completarse ambas firmas.
 *
 * <p>`crearSala` estaba implementado pero no lo llamaba nadie: era un TODO en
 * firmarContrato, así que el chat de un pedido no llegaba a existir nunca y la
 * vista se quedaba en "la sala se abre cuando ambas partes firmen" para
 * siempre.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ContratoServicioImplSalaChatTest {

    @Mock private ContratoRepository contratoRepository;
    @Mock private PedidoRepository pedidoRepository;
    @Mock private PlantillaContratoRepository plantillaContratoRepository;
    @Mock private IPdfGeneracionServicio pdfGeneracionServicio;
    @Mock private ChatService chatService;

    @InjectMocks
    private ContratoServicioImpl contratoServicio;

    private static final Long ID_CREADOR = 2L;
    private static final Long ID_CLIENTE = 1L;

    private Contrato contrato;
    private Pedido pedido;

    @BeforeEach
    void setUp() {
        // Nombres, título y precio no son objeto de esta prueba, pero
        // firmarContrato devuelve el contrato renderizado y la plantilla
        // sustituye esos campos: sin ellos el render falla con NPE.
        Usuario cliente = Usuario.builder()
                .idUsuario(ID_CLIENTE).nombres("Ana").apellidos("Cliente").build();
        Usuario creador = Usuario.builder()
                .idUsuario(ID_CREADOR).nombres("Beto").apellidos("Creador").build();

        PerfilCreador perfil = PerfilCreador.builder().idPerfil(10L).usuario(creador).build();
        Servicio servicio = Servicio.builder()
                .idServicio(100L)
                .perfil(perfil)
                .tituloServicio("Ilustracion de personaje")
                .descripcionDetallada("Personaje a color")
                .build();

        pedido = Pedido.builder()
                .idPedido(50L)
                .usuarioCliente(cliente)
                .servicio(servicio)
                .precioPactado(new BigDecimal("50.00"))
                .fechaEntregaEstimada(LocalDateTime.now().plusDays(7))
                .build();

        // La plantilla hace falta porque firmarContrato devuelve el contrato ya
        // renderizado; no es lo que se prueba aquí, pero sin ella el mapeo casca.
        PlantillaContrato plantilla = PlantillaContrato.builder()
                .idPlantilla(1L)
                .versionLegal("v1.0")
                .cuerpoHtmlPlantilla("<html><body>Contrato de prueba</body></html>")
                .build();

        contrato = Contrato.builder().idContrato(7L).pedido(pedido).plantilla(plantilla).limiteRevisiones(2).build();

        given(contratoRepository.findById(7L)).willReturn(Optional.of(contrato));
        given(contratoRepository.save(any(Contrato.class))).willAnswer(inv -> inv.getArgument(0));
    }

    @Test
    @DisplayName("con una sola firma la sala todavia no se abre")
    void unaFirmaNoAbreSala() {
        contratoServicio.firmarContrato(7L, ID_CREADOR);

        verify(chatService, never()).crearSala(any());
    }

    @Test
    @DisplayName("al completarse la segunda firma se abre la sala del pedido")
    void segundaFirmaAbreSala() {
        // El creador ya había firmado; ahora firma el cliente.
        contrato.setHashFirmaCreador("hash-creador");

        contratoServicio.firmarContrato(7L, ID_CLIENTE);

        verify(chatService).crearSala(pedido);
    }

    @Test
    @DisplayName("la sala se abre igual si el ultimo en firmar es el creador")
    void ordenDeFirmaIndiferente() {
        contrato.setHashFirmaCliente("hash-cliente");

        contratoServicio.firmarContrato(7L, ID_CREADOR);

        verify(chatService).crearSala(pedido);
    }
}
