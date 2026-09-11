package uteq.edu.ec.artisync.service.legal.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;
import uteq.edu.ec.artisync.dto.peticion.legal.PeticionSolicitudRetiro;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaSaldoCreador;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaSolicitudRetiro;
import uteq.edu.ec.artisync.entity.legal.SolicitudRetiro;
import uteq.edu.ec.artisync.entity.perfil.DatosPagoCreador;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.legal.SolicitudRetiroRepository;
import uteq.edu.ec.artisync.repository.legal.TransaccionPagoRepository;
import uteq.edu.ec.artisync.repository.perfil.DatosPagoCreadorRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Reglas de negocio de retiros (RF-20): quedan fuera de alcance de este test
 * la llamada real a PayPal Payouts (Fase 2) y el bloqueo pesimista de
 * findByIdParaActualizar (cubierto por SolicitudRetiroConcurrenciaIT contra
 * Postgres real).
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SolicitudRetiroServicioImplTest {

    private static final Long ID_CREADOR = 200L;
    private static final Long ID_ADMIN = 900L;
    private static final BigDecimal MONTO_MINIMO = new BigDecimal("10.00");

    @Mock private SolicitudRetiroRepository solicitudRetiroRepository;
    @Mock private DatosPagoCreadorRepository datosPagoCreadorRepository;
    @Mock private TransaccionPagoRepository transaccionPagoRepository;
    @Mock private UserRepository usuarioRepository;

    @InjectMocks
    private SolicitudRetiroServicioImpl servicio;

    private User creador;
    private DatosPagoCreador datosPago;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(servicio, "montoMinimo", MONTO_MINIMO);

        creador = User.builder().idUsuario(ID_CREADOR).nombres("Ana").apellidos("Creadora").build();
        datosPago = DatosPagoCreador.builder().usuario(creador).correoPaypal("ana@paypal.test").build();

        given(datosPagoCreadorRepository.findByUsuarioIdUsuario(ID_CREADOR)).willReturn(Optional.of(datosPago));
        given(usuarioRepository.findById(ID_CREADOR)).willReturn(Optional.of(creador));
        given(usuarioRepository.findById(ID_ADMIN))
                .willReturn(Optional.of(User.builder().idUsuario(ID_ADMIN).nombres("Admin").apellidos("X").build()));
        given(solicitudRetiroRepository.existsByUsuarioCreadorIdUsuarioAndEstadoIn(anyLong(), any())).willReturn(false);
        given(transaccionPagoRepository.sumEgresosPorCreador(ID_CREADOR)).willReturn(new BigDecimal("100.00"));
        given(solicitudRetiroRepository.sumMontosEnCursoPorCreador(anyLong(), any())).willReturn(BigDecimal.ZERO);
        given(solicitudRetiroRepository.save(any(SolicitudRetiro.class)))
                .willAnswer(inv -> inv.getArgument(0));
    }

    private PeticionSolicitudRetiro peticion(String monto) {
        PeticionSolicitudRetiro p = new PeticionSolicitudRetiro();
        p.setMontoSolicitado(new BigDecimal(monto));
        return p;
    }

    @Test
    @DisplayName("el saldo disponible resta lo ya reservado por solicitudes en curso")
    void saldoDisponible_restaSolicitudesEnCurso() {
        given(transaccionPagoRepository.sumEgresosPorCreador(ID_CREADOR)).willReturn(new BigDecimal("100.00"));
        given(solicitudRetiroRepository.sumMontosEnCursoPorCreador(anyLong(), any())).willReturn(new BigDecimal("30.00"));

        RespuestaSaldoCreador saldo = servicio.obtenerSaldo(ID_CREADOR);

        assertThat(saldo.saldoDisponible()).isEqualByComparingTo("70.00");
        assertThat(saldo.tieneCorreoPaypalConfigurado()).isTrue();
        assertThat(saldo.montoMinimoRetiro()).isEqualByComparingTo(MONTO_MINIMO);
    }

    @Test
    @DisplayName("sin correo de PayPal configurado, no se puede solicitar")
    void solicitar_sinCorreoConfigurado_rechaza() {
        given(datosPagoCreadorRepository.findByUsuarioIdUsuario(ID_CREADOR)).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.solicitar(ID_CREADOR, peticion("20.00")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("correo de PayPal");
        verify(solicitudRetiroRepository, never()).save(any());
    }

    @Test
    @DisplayName("un monto bajo el mínimo configurado se rechaza")
    void solicitar_bajoMontoMinimo_rechaza() {
        assertThatThrownBy(() -> servicio.solicitar(ID_CREADOR, peticion("5.00")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("mínimo");
        verify(solicitudRetiroRepository, never()).save(any());
    }

    @Test
    @DisplayName("un monto por encima del saldo disponible se rechaza")
    void solicitar_porEncimaDelSaldo_rechaza() {
        given(transaccionPagoRepository.sumEgresosPorCreador(ID_CREADOR)).willReturn(new BigDecimal("50.00"));

        assertThatThrownBy(() -> servicio.solicitar(ID_CREADOR, peticion("80.00")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("saldo disponible");
        verify(solicitudRetiroRepository, never()).save(any());
    }

    @Test
    @DisplayName("con una solicitud ya en curso, no se puede abrir otra")
    void solicitar_conSolicitudEnCurso_rechaza() {
        given(solicitudRetiroRepository.existsByUsuarioCreadorIdUsuarioAndEstadoIn(anyLong(), any())).willReturn(true);

        assertThatThrownBy(() -> servicio.solicitar(ID_CREADOR, peticion("20.00")))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("en curso");
        verify(solicitudRetiroRepository, never()).save(any());
    }

    @Test
    @DisplayName("una solicitud válida se crea Pendiente con el correo copiado de datos de pago")
    void solicitar_valida_creaPendiente() {
        RespuestaSolicitudRetiro respuesta = servicio.solicitar(ID_CREADOR, peticion("20.00"));

        assertThat(respuesta.estado()).isEqualTo("Pendiente");
        assertThat(respuesta.correoPaypalDestino()).isEqualTo("ana@paypal.test");

        ArgumentCaptor<SolicitudRetiro> captor = ArgumentCaptor.forClass(SolicitudRetiro.class);
        verify(solicitudRetiroRepository).save(captor.capture());
        assertThat(captor.getValue().getMontoSolicitado()).isEqualByComparingTo("20.00");
    }

    @Test
    @DisplayName("rechazar sin nota es rechazado")
    void rechazar_sinNota_rechaza() {
        assertThatThrownBy(() -> servicio.rechazar(1L, ID_ADMIN, "  "))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("motivo");
        verify(solicitudRetiroRepository, never()).findByIdParaActualizar(any());
    }

    @Test
    @DisplayName("rechazar una solicitud inexistente lanza recurso no encontrado")
    void rechazar_solicitudInexistente_lanzaNoEncontrado() {
        given(solicitudRetiroRepository.findByIdParaActualizar(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> servicio.rechazar(1L, ID_ADMIN, "motivo válido"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("rechazar una solicitud que ya no está Pendiente es rechazado")
    void rechazar_noPendiente_rechaza() {
        SolicitudRetiro solicitud = SolicitudRetiro.builder()
                .idSolicitud(1L).usuarioCreador(creador).estado("Aprobado")
                .montoSolicitado(new BigDecimal("20.00")).correoPaypalDestino("ana@paypal.test").build();
        given(solicitudRetiroRepository.findByIdParaActualizar(1L)).willReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> servicio.rechazar(1L, ID_ADMIN, "motivo válido"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Pendiente");
    }

    @Test
    @DisplayName("rechazar una solicitud Pendiente con nota la deja Rechazada")
    void rechazar_pendienteConNota_rechazaCorrectamente() {
        SolicitudRetiro solicitud = SolicitudRetiro.builder()
                .idSolicitud(1L).usuarioCreador(creador).estado("Pendiente")
                .montoSolicitado(new BigDecimal("20.00")).correoPaypalDestino("ana@paypal.test").build();
        given(solicitudRetiroRepository.findByIdParaActualizar(1L)).willReturn(Optional.of(solicitud));
        given(solicitudRetiroRepository.save(any(SolicitudRetiro.class))).willAnswer(inv -> inv.getArgument(0));

        RespuestaSolicitudRetiro respuesta = servicio.rechazar(1L, ID_ADMIN, "No cumple los requisitos");

        assertThat(respuesta.estado()).isEqualTo("Rechazado");
        assertThat(respuesta.notaAdmin()).isEqualTo("No cumple los requisitos");
    }

    // El camino feliz de aprobar() (y reintentar()) ejecuta un payout real
    // contra PayPal: se prueba en SolicitudRetiroServicioImplPayoutTest, con
    // RestTemplate mockeado, no aquí (este archivo es solo reglas de negocio
    // que no tocan PayPal).

    @Test
    @DisplayName("no se puede decidir dos veces sobre la misma solicitud")
    void aprobar_yaDecidida_rechaza() {
        SolicitudRetiro solicitud = SolicitudRetiro.builder()
                .idSolicitud(1L).usuarioCreador(creador).estado("Rechazado")
                .montoSolicitado(new BigDecimal("20.00")).correoPaypalDestino("ana@paypal.test").build();
        given(solicitudRetiroRepository.findByIdParaActualizar(1L)).willReturn(Optional.of(solicitud));

        assertThatThrownBy(() -> servicio.aprobar(1L, ID_ADMIN))
                .isInstanceOf(BusinessRuleException.class);
        verify(solicitudRetiroRepository, never()).save(any());
    }

    @Test
    @DisplayName("misSolicitudes devuelve el historial del creador mapeado")
    void misSolicitudes_devuelveHistorial() {
        SolicitudRetiro solicitud = SolicitudRetiro.builder()
                .idSolicitud(1L).usuarioCreador(creador).estado("Pagado")
                .montoSolicitado(new BigDecimal("20.00")).correoPaypalDestino("ana@paypal.test").build();
        given(solicitudRetiroRepository.findByUsuarioCreadorIdUsuarioOrderByFechaSolicitudDesc(ID_CREADOR))
                .willReturn(List.of(solicitud));

        List<RespuestaSolicitudRetiro> historial = servicio.misSolicitudes(ID_CREADOR);

        assertThat(historial).hasSize(1);
        assertThat(historial.get(0).estado()).isEqualTo("Pagado");
    }
}
