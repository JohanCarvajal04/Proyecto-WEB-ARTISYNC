package uteq.edu.ec.artisync.service.legal.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import uteq.edu.ec.artisync.dto.peticion.legal.FiltroPagoGarantia;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaPagoGarantia;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaPagoGarantiaDetalle;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaResumenEscrow;
import uteq.edu.ec.artisync.entity.catalogo.Servicio;
import uteq.edu.ec.artisync.entity.legal.Contrato;
import uteq.edu.ec.artisync.entity.legal.PagoGarantia;
import uteq.edu.ec.artisync.entity.legal.TransaccionPago;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.repository.legal.PagoGarantiaRepository;
import uteq.edu.ec.artisync.repository.legal.TransaccionPagoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class PagoGarantiaAuditoriaServicioImplTest {

    @Mock private PagoGarantiaRepository pagoGarantiaRepository;
    @Mock private TransaccionPagoRepository transaccionPagoRepository;

    @InjectMocks
    private PagoGarantiaAuditoriaServicioImpl servicio;

    private PagoGarantia pago;

    @BeforeEach
    void setUp() {
        Usuario cliente = Usuario.builder().idUsuario(1L).nombres("Ana").apellidos("Cliente").correo("ana@test.com").build();
        Usuario creador = Usuario.builder().idUsuario(2L).nombres("Beto").apellidos("Creador").build();
        PerfilCreador perfil = PerfilCreador.builder().idPerfil(10L).usuario(creador).build();
        Servicio servicioEntity = Servicio.builder().idServicio(20L).perfil(perfil).tituloServicio("Ilustración").build();
        Pedido pedido = Pedido.builder().idPedido(30L).usuarioCliente(cliente).servicio(servicioEntity).build();
        Contrato contrato = Contrato.builder().idContrato(40L).pedido(pedido)
                .fechaFormalizacion(LocalDateTime.of(2026, 1, 15, 10, 0)).build();
        pago = PagoGarantia.builder()
                .idPago(50L).contrato(contrato).idOrdenPaypal("PAYPAL-1")
                .montoRetenido(new BigDecimal("100.00")).estadoFondos("Retenido").build();
    }

    @Test
    void listar_mapeaClienteYCreadorDesdeElPedido() {
        Page<PagoGarantia> pagina = new PageImpl<>(List.of(pago));
        given(pagoGarantiaRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .willReturn(pagina);

        Page<RespuestaPagoGarantia> resultado = servicio.listar(new FiltroPagoGarantia(), PageRequest.of(0, 20));

        assertThat(resultado.getContent()).hasSize(1);
        RespuestaPagoGarantia fila = resultado.getContent().get(0);
        assertThat(fila.idPago()).isEqualTo(50L);
        assertThat(fila.nombreCliente()).isEqualTo("Ana Cliente");
        assertThat(fila.nombreCreador()).isEqualTo("Beto Creador");
        assertThat(fila.montoRetenido()).isEqualByComparingTo("100.00");
    }

    @Test
    void obtenerDetalle_incluyeElHistorialDeTransacciones() {
        given(pagoGarantiaRepository.findById(50L)).willReturn(Optional.of(pago));
        TransaccionPago transaccion = TransaccionPago.builder()
                .idTransaccion(60L).pago(pago).tipoTransaccion("CAPTURA").monto(new BigDecimal("100.00")).build();
        given(transaccionPagoRepository.findByPagoIdPagoOrderByFechaEjecucionDesc(50L))
                .willReturn(List.of(transaccion));

        RespuestaPagoGarantiaDetalle detalle = servicio.obtenerDetalle(50L);

        assertThat(detalle.correoCliente()).isEqualTo("ana@test.com");
        assertThat(detalle.transacciones()).hasSize(1);
        assertThat(detalle.transacciones().get(0).tipoTransaccion()).isEqualTo("CAPTURA");
    }

    @Test
    void obtenerDetalle_pagoInexistente_lanza404() {
        given(pagoGarantiaRepository.findById(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.obtenerDetalle(999L))
                .isInstanceOf(ExcepcionRecursoNoEncontrado.class);
    }

    @Test
    void obtenerResumen_delegaEnElRepositorio() {
        given(pagoGarantiaRepository.resumenPorEstado()).willReturn(List.of(
                RespuestaResumenEscrow.builder().estadoFondos("Retenido").cantidad(3).montoTotal(new BigDecimal("300.00")).build()));

        List<RespuestaResumenEscrow> resumen = servicio.obtenerResumen();

        assertThat(resumen).hasSize(1);
        assertThat(resumen.get(0).cantidad()).isEqualTo(3);
    }
}
