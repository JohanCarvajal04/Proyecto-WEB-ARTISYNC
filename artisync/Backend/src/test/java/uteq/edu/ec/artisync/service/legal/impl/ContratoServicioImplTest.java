package uteq.edu.ec.artisync.service.legal.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaContrato;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaEstadoFirma;
import uteq.edu.ec.artisync.entity.catalogo.Servicio;
import uteq.edu.ec.artisync.entity.legal.Contrato;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.pedido.PlantillaContrato;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.legal.ContratoRepository;
import uteq.edu.ec.artisync.repository.pedido.PedidoRepository;
import uteq.edu.ec.artisync.repository.pedido.PlantillaContratoRepository;
import uteq.edu.ec.artisync.service.comunicacion.ChatService;
import uteq.edu.ec.artisync.service.legal.IPdfGeneracionServicio;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class ContratoServicioImplTest {

    @Mock private ContratoRepository contratoRepository;
    @Mock private PedidoRepository pedidoRepository;
    @Mock private PlantillaContratoRepository plantillaContratoRepository;
    @Mock private IPdfGeneracionServicio pdfGeneracionServicio;
    @Mock private ChatService chatService;

    @InjectMocks
    private ContratoServicioImpl contratoServicio;

    private Usuario creador;
    private Usuario cliente;
    private Pedido pedido;
    private PlantillaContrato plantilla;

    @BeforeEach
    void setUp() {
        creador = Usuario.builder().idUsuario(1L).nombres("Creador").apellidos("Uno").correo("creador@test.com").build();
        cliente = Usuario.builder().idUsuario(2L).nombres("Cliente").apellidos("Uno").correo("cliente@test.com").build();
        PerfilCreador perfil = PerfilCreador.builder().idPerfil(1L).usuario(creador).build();
        Servicio servicio = Servicio.builder().idServicio(1L).perfil(perfil)
                .tituloServicio("Logo").descripcionDetallada("Descripcion detallada de ejemplo con veinte caracteres")
                .limiteRevisionesBase(2).build();
        pedido = Pedido.builder().idPedido(1L).usuarioCliente(cliente).servicio(servicio).precioPactado(new BigDecimal("50.00")).build();
        plantilla = PlantillaContrato.builder().idPlantilla(1L).versionLegal("v1")
                .cuerpoHtmlPlantilla("<html><body>{{nombre_creador}} - {{nombre_cliente}} - {{descripcion_servicio}} - {{precio_pactado}} - {{limite_revisiones}} - {{fecha_entrega}} - {{fecha_actual}}</body></html>")
                .build();
    }

    @Test
    @DisplayName("generarContrato crea el contrato con el limite de revisiones del servicio")
    void generarContrato_creaContrato() {
        given(pedidoRepository.findById(1L)).willReturn(Optional.of(pedido));
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.empty());
        given(plantillaContratoRepository.findFirstByOrderByIdPlantillaDesc()).willReturn(Optional.of(plantilla));
        given(contratoRepository.save(any(Contrato.class))).willAnswer(inv -> {
            Contrato c = inv.getArgument(0);
            c.setIdContrato(10L);
            return c;
        });

        RespuestaContrato respuesta = contratoServicio.generarContrato(1L);

        assertThat(respuesta.getIdContrato()).isEqualTo(10L);
        assertThat(respuesta.getLimiteRevisiones()).isEqualTo(2);
        assertThat(respuesta.getContenidoHtml()).contains("Creador Uno").contains("Cliente Uno");
    }

    @Test
    @DisplayName("generarContrato lanza recurso no encontrado si el pedido no existe")
    void generarContrato_pedidoInexistente() {
        given(pedidoRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> contratoServicio.generarContrato(1L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("generarContrato rechaza si ya existe un contrato para el pedido")
    void generarContrato_rechazaDuplicado() {
        given(pedidoRepository.findById(1L)).willReturn(Optional.of(pedido));
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.of(new Contrato()));

        assertThatThrownBy(() -> contratoServicio.generarContrato(1L))
                .isInstanceOf(ExcepcionReglaNegocio.class);
    }

    @Test
    @DisplayName("generarContrato lanza recurso no encontrado si no hay plantillas disponibles")
    void generarContrato_sinPlantillas() {
        given(pedidoRepository.findById(1L)).willReturn(Optional.of(pedido));
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.empty());
        given(plantillaContratoRepository.findFirstByOrderByIdPlantillaDesc()).willReturn(Optional.empty());

        assertThatThrownBy(() -> contratoServicio.generarContrato(1L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("firmarContrato registra la firma del creador")
    void firmarContrato_firmaCreador() {
        Contrato contrato = Contrato.builder().idContrato(10L).pedido(pedido).plantilla(plantilla).limiteRevisiones(2).build();
        given(contratoRepository.findById(10L)).willReturn(Optional.of(contrato));
        given(contratoRepository.save(any(Contrato.class))).willReturn(contrato);

        RespuestaContrato respuesta = contratoServicio.firmarContrato(10L, 1L);

        assertThat(respuesta.getHashFirmaCreador()).isNotBlank();
        assertThat(respuesta.getHashFirmaCliente()).isNull();
        assertThat(respuesta.getAmbasFirmasCompletas()).isFalse();
    }

    @Test
    @DisplayName("firmarContrato registra la firma del cliente")
    void firmarContrato_firmaCliente() {
        Contrato contrato = Contrato.builder().idContrato(10L).pedido(pedido).plantilla(plantilla).limiteRevisiones(2).build();
        given(contratoRepository.findById(10L)).willReturn(Optional.of(contrato));
        given(contratoRepository.save(any(Contrato.class))).willReturn(contrato);

        RespuestaContrato respuesta = contratoServicio.firmarContrato(10L, 2L);

        assertThat(respuesta.getHashFirmaCliente()).isNotBlank();
    }

    @Test
    @DisplayName("firmarContrato marca ambas firmas completas cuando ya firmo el creador y firma el cliente")
    void firmarContrato_ambasFirmas() {
        Contrato contrato = Contrato.builder().idContrato(10L).pedido(pedido).plantilla(plantilla)
                .limiteRevisiones(2).hashFirmaCreador("hash-creador").build();
        given(contratoRepository.findById(10L)).willReturn(Optional.of(contrato));
        given(contratoRepository.save(any(Contrato.class))).willReturn(contrato);

        RespuestaContrato respuesta = contratoServicio.firmarContrato(10L, 2L);

        assertThat(respuesta.getAmbasFirmasCompletas()).isTrue();
    }

    @Test
    @DisplayName("firmarContrato rechaza una segunda firma del mismo creador")
    void firmarContrato_rechazaDobleFirmaCreador() {
        Contrato contrato = Contrato.builder().idContrato(10L).pedido(pedido).plantilla(plantilla)
                .limiteRevisiones(2).hashFirmaCreador("hash-existente").build();
        given(contratoRepository.findById(10L)).willReturn(Optional.of(contrato));

        assertThatThrownBy(() -> contratoServicio.firmarContrato(10L, 1L))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("El creador ya firmo");
    }

    @Test
    @DisplayName("firmarContrato rechaza una segunda firma del mismo cliente")
    void firmarContrato_rechazaDobleFirmaCliente() {
        Contrato contrato = Contrato.builder().idContrato(10L).pedido(pedido).plantilla(plantilla)
                .limiteRevisiones(2).hashFirmaCliente("hash-existente").build();
        given(contratoRepository.findById(10L)).willReturn(Optional.of(contrato));

        assertThatThrownBy(() -> contratoServicio.firmarContrato(10L, 2L))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("El cliente ya firmo");
    }

    @Test
    @DisplayName("firmarContrato rechaza a un usuario que no es parte del contrato")
    void firmarContrato_rechazaAjeno() {
        Contrato contrato = Contrato.builder().idContrato(10L).pedido(pedido).plantilla(plantilla).limiteRevisiones(2).build();
        given(contratoRepository.findById(10L)).willReturn(Optional.of(contrato));

        assertThatThrownBy(() -> contratoServicio.firmarContrato(10L, 999L))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("No eres parte de este contrato");
    }

    @Test
    @DisplayName("firmarContrato lanza recurso no encontrado si el contrato no existe")
    void firmarContrato_contratoInexistente() {
        given(contratoRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> contratoServicio.firmarContrato(10L, 1L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("obtenerContrato devuelve el contrato existente")
    void obtenerContrato_devuelveContrato() {
        Contrato contrato = Contrato.builder().idContrato(10L).pedido(pedido).plantilla(plantilla).limiteRevisiones(2).build();
        given(contratoRepository.findById(10L)).willReturn(Optional.of(contrato));

        assertThat(contratoServicio.obtenerContrato(10L)).isNotNull();
    }

    @Test
    @DisplayName("obtenerContrato lanza recurso no encontrado si no existe")
    void obtenerContrato_inexistente() {
        given(contratoRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> contratoServicio.obtenerContrato(10L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("obtenerContratoPorPedido devuelve el contrato asociado al pedido")
    void obtenerContratoPorPedido_devuelveContrato() {
        Contrato contrato = Contrato.builder().idContrato(10L).pedido(pedido).plantilla(plantilla).limiteRevisiones(2).build();
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.of(contrato));

        assertThat(contratoServicio.obtenerContratoPorPedido(1L)).isNotNull();
    }

    @Test
    @DisplayName("obtenerContratoPorPedido lanza recurso no encontrado si no existe")
    void obtenerContratoPorPedido_inexistente() {
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> contratoServicio.obtenerContratoPorPedido(1L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("obtenerEstadoFirma reporta pendiente de ambas partes")
    void obtenerEstadoFirma_pendienteAmbas() {
        Contrato contrato = Contrato.builder().idContrato(10L).build();
        given(contratoRepository.findById(10L)).willReturn(Optional.of(contrato));

        RespuestaEstadoFirma estado = contratoServicio.obtenerEstadoFirma(10L);

        assertThat(estado.getAmbasFirmasCompletas()).isFalse();
        assertThat(estado.getMensajeEstado()).contains("Pendiente de firma por ambas partes");
    }

    @Test
    @DisplayName("obtenerEstadoFirma reporta esperando al cliente cuando solo firmo el creador")
    void obtenerEstadoFirma_esperandoCliente() {
        Contrato contrato = Contrato.builder().idContrato(10L).hashFirmaCreador("hash").build();
        given(contratoRepository.findById(10L)).willReturn(Optional.of(contrato));

        assertThat(contratoServicio.obtenerEstadoFirma(10L).getMensajeEstado()).contains("Esperando firma del Cliente");
    }

    @Test
    @DisplayName("obtenerEstadoFirma reporta esperando al creador cuando solo firmo el cliente")
    void obtenerEstadoFirma_esperandoCreador() {
        Contrato contrato = Contrato.builder().idContrato(10L).hashFirmaCliente("hash").build();
        given(contratoRepository.findById(10L)).willReturn(Optional.of(contrato));

        assertThat(contratoServicio.obtenerEstadoFirma(10L).getMensajeEstado()).contains("Esperando firma del Creador");
    }

    @Test
    @DisplayName("obtenerEstadoFirma reporta contrato completamente firmado")
    void obtenerEstadoFirma_completo() {
        Contrato contrato = Contrato.builder().idContrato(10L).hashFirmaCreador("h1").hashFirmaCliente("h2").build();
        given(contratoRepository.findById(10L)).willReturn(Optional.of(contrato));

        RespuestaEstadoFirma estado = contratoServicio.obtenerEstadoFirma(10L);

        assertThat(estado.getAmbasFirmasCompletas()).isTrue();
        assertThat(estado.getMensajeEstado()).contains("completamente firmado");
    }

    @Test
    @DisplayName("obtenerEstadoFirma lanza recurso no encontrado si el contrato no existe")
    void obtenerEstadoFirma_inexistente() {
        given(contratoRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> contratoServicio.obtenerEstadoFirma(10L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("generarPdf delega en el servicio de generacion de PDF")
    void generarPdf_delega() {
        Contrato contrato = Contrato.builder().idContrato(10L).pedido(pedido).plantilla(plantilla)
                .hashFirmaCreador("h1").hashFirmaCliente("h2").limiteRevisiones(2).build();
        given(contratoRepository.findById(10L)).willReturn(Optional.of(contrato));
        given(pdfGeneracionServicio.generarPdfDesdeHtml(org.mockito.ArgumentMatchers.anyString())).willReturn(new byte[]{1, 2, 3});

        byte[] pdf = contratoServicio.generarPdf(10L);

        assertThat(pdf).hasSize(3);
    }

    @Test
    @DisplayName("generarPdf lanza recurso no encontrado si el contrato no existe")
    void generarPdf_inexistente() {
        given(contratoRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> contratoServicio.generarPdf(10L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }
}
