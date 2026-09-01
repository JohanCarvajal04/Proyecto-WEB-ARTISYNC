package uteq.edu.ec.artisync.service.legal.impl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
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
import uteq.edu.ec.artisync.service.legal.IPdfGeneracionServicio;

import java.math.BigDecimal;
import java.util.List;
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

    @InjectMocks
    private ContratoServicioImpl contratoServicio;

    private static final Long ID_CREADOR = 1L;
    private static final Long ID_CLIENTE = 2L;
    private static final Long ID_AJENO = 999L;

    private Usuario creador;
    private Usuario cliente;
    private Pedido pedido;
    private PlantillaContrato plantilla;

    @BeforeEach
    void setUp() {
        creador = Usuario.builder().idUsuario(ID_CREADOR).nombres("Creador").apellidos("Uno").correo("creador@test.com").build();
        cliente = Usuario.builder().idUsuario(ID_CLIENTE).nombres("Cliente").apellidos("Uno").correo("cliente@test.com").build();
        PerfilCreador perfil = PerfilCreador.builder().idPerfil(1L).usuario(creador).build();
        Servicio servicio = Servicio.builder().idServicio(1L).perfil(perfil)
                .tituloServicio("Logo").descripcionDetallada("Descripcion detallada de ejemplo con veinte caracteres")
                .limiteRevisionesBase(2).build();
        pedido = Pedido.builder().idPedido(1L).usuarioCliente(cliente).servicio(servicio).precioPactado(new BigDecimal("50.00")).build();
        plantilla = PlantillaContrato.builder().idPlantilla(1L).versionLegal("v1")
                .cuerpoHtmlPlantilla("<html><body>{{nombre_creador}} - {{nombre_cliente}} - {{descripcion_servicio}} - {{precio_pactado}} - {{limite_revisiones}} - {{fecha_entrega}} - {{fecha_actual}}</body></html>")
                .build();
    }

    @AfterEach
    void limpiarContexto() {
        SecurityContextHolder.clearContext();
    }

    private void autenticarComoAdmin() {
        var auth = new UsernamePasswordAuthenticationToken(
                "admin@test.com", "N/A", List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
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

        RespuestaContrato respuesta = contratoServicio.generarContrato(1L, ID_CLIENTE);

        assertThat(respuesta.getIdContrato()).isEqualTo(10L);
        assertThat(respuesta.getLimiteRevisiones()).isEqualTo(2);
        assertThat(respuesta.getContenidoHtml()).contains("Creador Uno").contains("Cliente Uno");
    }

    @Test
    @DisplayName("H-02: generarContrato rechaza a un usuario ajeno al pedido")
    void generarContrato_rechazaAjeno() {
        given(pedidoRepository.findById(1L)).willReturn(Optional.of(pedido));

        assertThatThrownBy(() -> contratoServicio.generarContrato(1L, ID_AJENO))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("H-02: generarContrato permite a un ADMIN generar sobre un pedido ajeno")
    void generarContrato_adminPuedeGenerar() {
        autenticarComoAdmin();
        given(pedidoRepository.findById(1L)).willReturn(Optional.of(pedido));
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.empty());
        given(plantillaContratoRepository.findFirstByOrderByIdPlantillaDesc()).willReturn(Optional.of(plantilla));
        given(contratoRepository.save(any(Contrato.class))).willAnswer(inv -> inv.getArgument(0));

        assertThat(contratoServicio.generarContrato(1L, ID_AJENO)).isNotNull();
    }

    @Test
    @DisplayName("generarContrato lanza recurso no encontrado si el pedido no existe")
    void generarContrato_pedidoInexistente() {
        given(pedidoRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> contratoServicio.generarContrato(1L, ID_CLIENTE))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("generarContrato rechaza si ya existe un contrato para el pedido")
    void generarContrato_rechazaDuplicado() {
        given(pedidoRepository.findById(1L)).willReturn(Optional.of(pedido));
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.of(new Contrato()));

        assertThatThrownBy(() -> contratoServicio.generarContrato(1L, ID_CLIENTE))
                .isInstanceOf(ExcepcionReglaNegocio.class);
    }

    @Test
    @DisplayName("generarContrato lanza recurso no encontrado si no hay plantillas disponibles")
    void generarContrato_sinPlantillas() {
        given(pedidoRepository.findById(1L)).willReturn(Optional.of(pedido));
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.empty());
        given(plantillaContratoRepository.findFirstByOrderByIdPlantillaDesc()).willReturn(Optional.empty());

        assertThatThrownBy(() -> contratoServicio.generarContrato(1L, ID_CLIENTE))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("firmarContrato registra la firma del creador")
    void firmarContrato_firmaCreador() {
        Contrato contrato = Contrato.builder().idContrato(10L).pedido(pedido).plantilla(plantilla).limiteRevisiones(2).build();
        given(contratoRepository.findByIdParaFirmar(10L)).willReturn(Optional.of(contrato));
        given(contratoRepository.save(any(Contrato.class))).willReturn(contrato);

        RespuestaContrato respuesta = contratoServicio.firmarContrato(10L, ID_CREADOR);

        assertThat(respuesta.getHashFirmaCreador()).isNotBlank();
        assertThat(respuesta.getHashFirmaCliente()).isNull();
        assertThat(respuesta.getAmbasFirmasCompletas()).isFalse();
    }

    @Test
    @DisplayName("firmarContrato registra la firma del cliente")
    void firmarContrato_firmaCliente() {
        Contrato contrato = Contrato.builder().idContrato(10L).pedido(pedido).plantilla(plantilla).limiteRevisiones(2).build();
        given(contratoRepository.findByIdParaFirmar(10L)).willReturn(Optional.of(contrato));
        given(contratoRepository.save(any(Contrato.class))).willReturn(contrato);

        RespuestaContrato respuesta = contratoServicio.firmarContrato(10L, ID_CLIENTE);

        assertThat(respuesta.getHashFirmaCliente()).isNotBlank();
    }

    @Test
    @DisplayName("firmarContrato marca ambas firmas completas cuando ya firmo el creador y firma el cliente")
    void firmarContrato_ambasFirmas() {
        Contrato contrato = Contrato.builder().idContrato(10L).pedido(pedido).plantilla(plantilla)
                .limiteRevisiones(2).hashFirmaCreador("hash-creador").build();
        given(contratoRepository.findByIdParaFirmar(10L)).willReturn(Optional.of(contrato));
        given(contratoRepository.save(any(Contrato.class))).willReturn(contrato);

        RespuestaContrato respuesta = contratoServicio.firmarContrato(10L, ID_CLIENTE);

        assertThat(respuesta.getAmbasFirmasCompletas()).isTrue();
    }

    @Test
    @DisplayName("firmarContrato rechaza una segunda firma del mismo creador")
    void firmarContrato_rechazaDobleFirmaCreador() {
        Contrato contrato = Contrato.builder().idContrato(10L).pedido(pedido).plantilla(plantilla)
                .limiteRevisiones(2).hashFirmaCreador("hash-existente").build();
        given(contratoRepository.findByIdParaFirmar(10L)).willReturn(Optional.of(contrato));

        assertThatThrownBy(() -> contratoServicio.firmarContrato(10L, ID_CREADOR))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("El creador ya firmo");
    }

    @Test
    @DisplayName("firmarContrato rechaza una segunda firma del mismo cliente")
    void firmarContrato_rechazaDobleFirmaCliente() {
        Contrato contrato = Contrato.builder().idContrato(10L).pedido(pedido).plantilla(plantilla)
                .limiteRevisiones(2).hashFirmaCliente("hash-existente").build();
        given(contratoRepository.findByIdParaFirmar(10L)).willReturn(Optional.of(contrato));

        assertThatThrownBy(() -> contratoServicio.firmarContrato(10L, ID_CLIENTE))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("El cliente ya firmo");
    }

    @Test
    @DisplayName("H-02: firmarContrato rechaza (403) a un usuario que no es parte del contrato")
    void firmarContrato_rechazaAjeno() {
        Contrato contrato = Contrato.builder().idContrato(10L).pedido(pedido).plantilla(plantilla).limiteRevisiones(2).build();
        given(contratoRepository.findByIdParaFirmar(10L)).willReturn(Optional.of(contrato));

        assertThatThrownBy(() -> contratoServicio.firmarContrato(10L, ID_AJENO))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("No eres parte de este contrato");
    }

    @Test
    @DisplayName("firmarContrato lanza recurso no encontrado si el contrato no existe")
    void firmarContrato_contratoInexistente() {
        given(contratoRepository.findByIdParaFirmar(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> contratoServicio.firmarContrato(10L, ID_CREADOR))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("obtenerContrato devuelve el contrato existente al cliente dueño")
    void obtenerContrato_devuelveContrato() {
        Contrato contrato = Contrato.builder().idContrato(10L).pedido(pedido).plantilla(plantilla).limiteRevisiones(2).build();
        given(contratoRepository.findById(10L)).willReturn(Optional.of(contrato));

        assertThat(contratoServicio.obtenerContrato(10L, ID_CLIENTE)).isNotNull();
    }

    @Test
    @DisplayName("H-02: obtenerContrato rechaza a un usuario ajeno (IDOR)")
    void obtenerContrato_rechazaAjeno() {
        Contrato contrato = Contrato.builder().idContrato(10L).pedido(pedido).plantilla(plantilla).limiteRevisiones(2).build();
        given(contratoRepository.findById(10L)).willReturn(Optional.of(contrato));

        assertThatThrownBy(() -> contratoServicio.obtenerContrato(10L, ID_AJENO))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("H-02: obtenerContrato permite a un ADMIN consultar un contrato ajeno")
    void obtenerContrato_adminPuedeVer() {
        autenticarComoAdmin();
        Contrato contrato = Contrato.builder().idContrato(10L).pedido(pedido).plantilla(plantilla).limiteRevisiones(2).build();
        given(contratoRepository.findById(10L)).willReturn(Optional.of(contrato));

        assertThat(contratoServicio.obtenerContrato(10L, ID_AJENO)).isNotNull();
    }

    @Test
    @DisplayName("obtenerContrato lanza recurso no encontrado si no existe")
    void obtenerContrato_inexistente() {
        given(contratoRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> contratoServicio.obtenerContrato(10L, ID_CLIENTE))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("obtenerContratoPorPedido devuelve el contrato asociado al pedido, al creador")
    void obtenerContratoPorPedido_devuelveContrato() {
        Contrato contrato = Contrato.builder().idContrato(10L).pedido(pedido).plantilla(plantilla).limiteRevisiones(2).build();
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.of(contrato));

        assertThat(contratoServicio.obtenerContratoPorPedido(1L, ID_CREADOR)).isNotNull();
    }

    @Test
    @DisplayName("H-02: obtenerContratoPorPedido rechaza a un usuario ajeno (IDOR)")
    void obtenerContratoPorPedido_rechazaAjeno() {
        Contrato contrato = Contrato.builder().idContrato(10L).pedido(pedido).plantilla(plantilla).limiteRevisiones(2).build();
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.of(contrato));

        assertThatThrownBy(() -> contratoServicio.obtenerContratoPorPedido(1L, ID_AJENO))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("obtenerContratoPorPedido lanza recurso no encontrado si no existe")
    void obtenerContratoPorPedido_inexistente() {
        given(contratoRepository.findByPedidoIdPedido(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> contratoServicio.obtenerContratoPorPedido(1L, ID_CLIENTE))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("obtenerEstadoFirma reporta pendiente de ambas partes")
    void obtenerEstadoFirma_pendienteAmbas() {
        Contrato contrato = Contrato.builder().idContrato(10L).pedido(pedido).build();
        given(contratoRepository.findById(10L)).willReturn(Optional.of(contrato));

        RespuestaEstadoFirma estado = contratoServicio.obtenerEstadoFirma(10L, ID_CLIENTE);

        assertThat(estado.getAmbasFirmasCompletas()).isFalse();
        assertThat(estado.getMensajeEstado()).contains("Pendiente de firma por ambas partes");
    }

    @Test
    @DisplayName("H-02: obtenerEstadoFirma rechaza a un usuario ajeno (IDOR)")
    void obtenerEstadoFirma_rechazaAjeno() {
        Contrato contrato = Contrato.builder().idContrato(10L).pedido(pedido).build();
        given(contratoRepository.findById(10L)).willReturn(Optional.of(contrato));

        assertThatThrownBy(() -> contratoServicio.obtenerEstadoFirma(10L, ID_AJENO))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("obtenerEstadoFirma reporta esperando al cliente cuando solo firmo el creador")
    void obtenerEstadoFirma_esperandoCliente() {
        Contrato contrato = Contrato.builder().idContrato(10L).pedido(pedido).hashFirmaCreador("hash").build();
        given(contratoRepository.findById(10L)).willReturn(Optional.of(contrato));

        assertThat(contratoServicio.obtenerEstadoFirma(10L, ID_CLIENTE).getMensajeEstado()).contains("Esperando firma del Cliente");
    }

    @Test
    @DisplayName("obtenerEstadoFirma reporta esperando al creador cuando solo firmo el cliente")
    void obtenerEstadoFirma_esperandoCreador() {
        Contrato contrato = Contrato.builder().idContrato(10L).pedido(pedido).hashFirmaCliente("hash").build();
        given(contratoRepository.findById(10L)).willReturn(Optional.of(contrato));

        assertThat(contratoServicio.obtenerEstadoFirma(10L, ID_CLIENTE).getMensajeEstado()).contains("Esperando firma del Creador");
    }

    @Test
    @DisplayName("obtenerEstadoFirma reporta contrato completamente firmado")
    void obtenerEstadoFirma_completo() {
        Contrato contrato = Contrato.builder().idContrato(10L).pedido(pedido).hashFirmaCreador("h1").hashFirmaCliente("h2").build();
        given(contratoRepository.findById(10L)).willReturn(Optional.of(contrato));

        RespuestaEstadoFirma estado = contratoServicio.obtenerEstadoFirma(10L, ID_CLIENTE);

        assertThat(estado.getAmbasFirmasCompletas()).isTrue();
        assertThat(estado.getMensajeEstado()).contains("completamente firmado");
    }

    @Test
    @DisplayName("obtenerEstadoFirma lanza recurso no encontrado si el contrato no existe")
    void obtenerEstadoFirma_inexistente() {
        given(contratoRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> contratoServicio.obtenerEstadoFirma(10L, ID_CLIENTE))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    @DisplayName("generarPdf delega en el servicio de generacion de PDF")
    void generarPdf_delega() {
        Contrato contrato = Contrato.builder().idContrato(10L).pedido(pedido).plantilla(plantilla)
                .hashFirmaCreador("h1").hashFirmaCliente("h2").limiteRevisiones(2).build();
        given(contratoRepository.findById(10L)).willReturn(Optional.of(contrato));
        given(pdfGeneracionServicio.generarPdfDesdeHtml(org.mockito.ArgumentMatchers.anyString())).willReturn(new byte[]{1, 2, 3});

        byte[] pdf = contratoServicio.generarPdf(10L, ID_CLIENTE);

        assertThat(pdf).hasSize(3);
    }

    @Test
    @DisplayName("H-02: generarPdf rechaza descargar el PDF de un contrato ajeno (IDOR)")
    void generarPdf_rechazaAjeno() {
        Contrato contrato = Contrato.builder().idContrato(10L).pedido(pedido).plantilla(plantilla)
                .hashFirmaCreador("h1").hashFirmaCliente("h2").limiteRevisiones(2).build();
        given(contratoRepository.findById(10L)).willReturn(Optional.of(contrato));

        assertThatThrownBy(() -> contratoServicio.generarPdf(10L, ID_AJENO))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("SEC-01: generarPdf escapa HTML controlado por el usuario antes de pasarlo al renderizador")
    void generarPdf_escapaHtmlDeUsuario() {
        // Servicio propio para este test: la descripcion detallada trae un
        // payload de SSRF/lectura de archivos como el que motivo el hallazgo
        // (ver docs del informe de seguridad, SEC-01).
        PerfilCreador perfil = PerfilCreador.builder().idPerfil(1L).usuario(creador).build();
        Servicio servicioMalicioso = Servicio.builder().idServicio(2L).perfil(perfil)
                .tituloServicio("Logo")
                .descripcionDetallada("<img src=\"http://169.254.169.254/latest/meta-data/\">")
                .limiteRevisionesBase(2).build();
        Pedido pedidoMalicioso = Pedido.builder().idPedido(2L).usuarioCliente(cliente)
                .servicio(servicioMalicioso).precioPactado(new BigDecimal("50.00")).build();
        Contrato contrato = Contrato.builder().idContrato(11L).pedido(pedidoMalicioso).plantilla(plantilla)
                .hashFirmaCreador("h1").hashFirmaCliente("h2").limiteRevisiones(2).build();
        given(contratoRepository.findById(11L)).willReturn(Optional.of(contrato));
        given(pdfGeneracionServicio.generarPdfDesdeHtml(org.mockito.ArgumentMatchers.anyString())).willReturn(new byte[]{1});

        contratoServicio.generarPdf(11L, ID_CLIENTE);

        org.mockito.ArgumentCaptor<String> htmlCapturado = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.Mockito.verify(pdfGeneracionServicio).generarPdfDesdeHtml(htmlCapturado.capture());
        String html = htmlCapturado.getValue();

        assertThat(html).contains("&lt;img");
        assertThat(html).doesNotContain("<img src=\"http");
    }

    @Test
    @DisplayName("generarPdf lanza recurso no encontrado si el contrato no existe")
    void generarPdf_inexistente() {
        given(contratoRepository.findById(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> contratoServicio.generarPdf(10L, ID_CLIENTE))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }
}
