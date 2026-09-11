package uteq.edu.ec.artisync.service.seguridad.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.entity.catalogo.Workflow;
import uteq.edu.ec.artisync.entity.legal.Contract;
import uteq.edu.ec.artisync.entity.legal.EscrowPayment;
import uteq.edu.ec.artisync.entity.pedido.WorkflowStage;
import uteq.edu.ec.artisync.entity.pedido.OrderStatusHistory;
import uteq.edu.ec.artisync.entity.pedido.Order;
import uteq.edu.ec.artisync.entity.perfil.AiCertificate;
import uteq.edu.ec.artisync.entity.perfil.CreatorPaymentDetails;
import uteq.edu.ec.artisync.entity.seguridad.TwoFactorAuthentication;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.legal.ContractRepository;
import uteq.edu.ec.artisync.repository.legal.EscrowPaymentRepository;
import uteq.edu.ec.artisync.repository.pedido.WorkflowStageConfigRepository;
import uteq.edu.ec.artisync.repository.pedido.OrderStatusHistoryRepository;
import uteq.edu.ec.artisync.repository.pedido.OrderRepository;
import uteq.edu.ec.artisync.repository.perfil.AiCertificateRepository;
import uteq.edu.ec.artisync.repository.perfil.CreatorPaymentDetailsRepository;
import uteq.edu.ec.artisync.repository.seguridad.TwoFactorAuthenticationRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;
import uteq.edu.ec.artisync.service.seguridad.TwoFactorService;
import uteq.edu.ec.artisync.service.shared.IntentosAutenticacionService;
import uteq.edu.ec.artisync.service.shared.SessionRevocationService;
import uteq.edu.ec.artisync.service.shared.almacenamiento.AlmacenamientoDocumentos;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * REQ-NF-018: cubre el mecanismo de supresión real de datos personales
 * (anonimización) y los ajustes de robustez: bloqueo pesimista para evitar
 * doble anonimización concurrente, idempotencia basada en el propio correo
 * anonimizado, bloqueo por pedido en curso, step-up de 2FA, y manejo de una
 * eventual colisión del correo anonimizado.
 */
@ExtendWith(MockitoExtension.class)
class PrivacyServiceImplTest {

    @Mock
    private UserRepository usuarioRepository;
    @Mock
    private AiCertificateRepository certificadoIaRepository;
    @Mock
    private CreatorPaymentDetailsRepository datosPagoCreadorRepository;
    @Mock
    private ContractRepository contratoRepository;
    @Mock
    private EscrowPaymentRepository pagoGarantiaRepository;
    @Mock
    private OrderRepository pedidoRepository;
    @Mock
    private OrderStatusHistoryRepository historialEstadoPedidoRepository;
    @Mock
    private WorkflowStageConfigRepository flujoEtapaConfigRepository;
    @Mock
    private TwoFactorAuthenticationRepository autenticacionDosFactoresRepository;
    @Mock
    private TwoFactorService twoFactorService;
    @Mock
    private IntentosAutenticacionService intentosAutenticacionService;
    @Mock
    private SessionRevocationService sessionRevocationService;
    @Mock
    private AlmacenamientoDocumentos almacenamientoDocumentos;

    @InjectMocks
    private PrivacyServiceImpl privacidadService;

    private User usuario;

    @BeforeEach
    void setUp() {
        usuario = User.builder()
                .idUsuario(1L)
                .nombres("Ana")
                .apellidos("Gomez")
                .correo("ana@example.com")
                .urlFotoPerfil("perfiles/ana.jpg")
                .build();

        lenient().when(pedidoRepository.findByUsuarioClienteIdUsuario(1L)).thenReturn(List.of());
        lenient().when(pedidoRepository.findByServicioPerfilUsuarioIdUsuario(1L)).thenReturn(List.of());
        lenient().when(contratoRepository.findByPedidoUsuarioClienteIdUsuario(1L)).thenReturn(List.of());
        lenient().when(contratoRepository.findByPedidoServicioPerfilUsuarioIdUsuario(1L)).thenReturn(List.of());
        lenient().when(certificadoIaRepository.findByUsuarioIdUsuario(1L)).thenReturn(List.of());
        lenient().when(datosPagoCreadorRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.empty());
        // Por defecto, sin 2FA configurado — la mayoría de los casos no lo necesitan.
        lenient().when(autenticacionDosFactoresRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.empty());
    }

    @Test
    void solicitarSupresionPropia_anonimizaDatosDelUsuario_sinImpedimentos() {
        when(usuarioRepository.findByIdParaAnonimizar(1L)).thenReturn(Optional.of(usuario));

        RespuestaMensaje respuesta = privacidadService.solicitarSupresionPropia(1L, null);

        assertNotNull(respuesta);
        assertEquals("Datos personales suprimidos exitosamente.", respuesta.getMensaje());
        assertEquals("User eliminado", usuario.getNombres());
        assertNull(usuario.getFechaNacimiento());
        assertNull(usuario.getUrlFotoPerfil());
        assertTrue(usuario.getCorreo().contains("@eliminado.artisync.invalid"));
        verify(almacenamientoDocumentos).eliminar("perfiles/ana.jpg");
        verify(sessionRevocationService).cambiarEstadoCuenta(1L, false);
        verify(usuarioRepository).save(usuario);
    }

    @Test
    void solicitarSupresionPropia_esIdempotente_siYaFueAnonimizado() {
        usuario.setCorreo("usuario-1-abc@eliminado.artisync.invalid");
        when(usuarioRepository.findByIdParaAnonimizar(1L)).thenReturn(Optional.of(usuario));

        RespuestaMensaje respuesta = privacidadService.solicitarSupresionPropia(1L, null);

        assertEquals("Tus datos personales ya fueron suprimidos anteriormente.", respuesta.getMensaje());
        verify(usuarioRepository, never()).save(any());
        verify(sessionRevocationService, never()).cambiarEstadoCuenta(any(), anyBoolean());
    }

    @Test
    void solicitarSupresionPropia_declaraExcepcionLegal_conFondosRetenidos() {
        when(usuarioRepository.findByIdParaAnonimizar(1L)).thenReturn(Optional.of(usuario));

        CreatorPaymentDetails datosPago = CreatorPaymentDetails.builder().idDatosPago(5L).correoPaypal("ana@paypal.com").build();
        when(datosPagoCreadorRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.of(datosPago));

        Contract contrato = Contract.builder().idContrato(10L).build();
        when(contratoRepository.findByPedidoServicioPerfilUsuarioIdUsuario(1L)).thenReturn(List.of(contrato));

        EscrowPayment pagoGarantia = EscrowPayment.builder().idPago(20L).estadoFondos("Retenido").build();
        when(pagoGarantiaRepository.findByContratoIdContrato(10L)).thenReturn(Optional.of(pagoGarantia));

        RespuestaMensaje respuesta = privacidadService.solicitarSupresionPropia(1L, null);

        assertTrue(respuesta.getMensaje().contains("excepciones legales"));
        assertEquals("ana@paypal.com", datosPago.getCorreoPaypal());
        verify(datosPagoCreadorRepository, never()).save(any());
        // El resto de los datos personales del usuario sí se anonimiza.
        assertEquals("User eliminado", usuario.getNombres());
    }

    @Test
    void solicitarSupresionPropia_seRechaza_siHayPedidoEnCurso() {
        when(usuarioRepository.findByIdParaAnonimizar(1L)).thenReturn(Optional.of(usuario));

        Workflow flujo = Workflow.builder().idFlujo(7L).build();
        Order pedido = Order.builder().idPedido(100L).flujo(flujo).build();
        when(pedidoRepository.findByUsuarioClienteIdUsuario(1L)).thenReturn(List.of(pedido));

        WorkflowStage etapaActual = WorkflowStage.builder().idEtapa(3L).build();
        OrderStatusHistory ultimaTransicion = OrderStatusHistory.builder().etapa(etapaActual).build();
        when(historialEstadoPedidoRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(100L))
                .thenReturn(Optional.of(ultimaTransicion));
        when(flujoEtapaConfigRepository.existsByFlujoIdFlujoAndEtapaIdEtapaAndEsEtapaFinalTrue(7L, 3L))
                .thenReturn(false);

        RespuestaMensaje respuesta = privacidadService.solicitarSupresionPropia(1L, null);

        assertTrue(respuesta.getMensaje().contains("pedido en curso"));
        verify(usuarioRepository, never()).save(any());
        verify(sessionRevocationService, never()).cambiarEstadoCuenta(any(), anyBoolean());
    }

    @Test
    void solicitarSupresionPropia_permiteSuprimir_siElPedidoYaEstaEnEtapaFinal() {
        when(usuarioRepository.findByIdParaAnonimizar(1L)).thenReturn(Optional.of(usuario));

        Workflow flujo = Workflow.builder().idFlujo(7L).build();
        Order pedido = Order.builder().idPedido(100L).flujo(flujo).build();
        when(pedidoRepository.findByUsuarioClienteIdUsuario(1L)).thenReturn(List.of(pedido));

        WorkflowStage etapaActual = WorkflowStage.builder().idEtapa(9L).build();
        OrderStatusHistory ultimaTransicion = OrderStatusHistory.builder().etapa(etapaActual).build();
        when(historialEstadoPedidoRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(100L))
                .thenReturn(Optional.of(ultimaTransicion));
        when(flujoEtapaConfigRepository.existsByFlujoIdFlujoAndEtapaIdEtapaAndEsEtapaFinalTrue(7L, 9L))
                .thenReturn(true);

        RespuestaMensaje respuesta = privacidadService.solicitarSupresionPropia(1L, null);

        assertEquals("Datos personales suprimidos exitosamente.", respuesta.getMensaje());
        verify(sessionRevocationService).cambiarEstadoCuenta(1L, false);
    }

    @Test
    void anonimizarUsuarioAdmin_rechaza_siYaFueAnonimizado() {
        usuario.setCorreo("usuario-1-abc@eliminado.artisync.invalid");
        when(usuarioRepository.findByIdParaAnonimizar(1L)).thenReturn(Optional.of(usuario));

        assertThrows(BusinessRuleException.class, () -> privacidadService.anonimizarUsuarioAdmin(1L, 99L));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void anonimizarUsuarioAdmin_rechaza_siAdminSeApuntaASiMismo() {
        assertThrows(BusinessRuleException.class, () -> privacidadService.anonimizarUsuarioAdmin(1L, 1L));
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    void anonimizarUsuarioAdmin_rechaza_siHayPedidoEnCurso() {
        when(usuarioRepository.findByIdParaAnonimizar(1L)).thenReturn(Optional.of(usuario));

        Workflow flujo = Workflow.builder().idFlujo(7L).build();
        Order pedido = Order.builder().idPedido(100L).flujo(flujo).build();
        when(pedidoRepository.findByServicioPerfilUsuarioIdUsuario(1L)).thenReturn(List.of(pedido));

        when(historialEstadoPedidoRepository.findTopByPedidoIdPedidoOrderByFechaTransicionDesc(100L))
                .thenReturn(Optional.empty());

        assertThrows(BusinessRuleException.class, () -> privacidadService.anonimizarUsuarioAdmin(1L, 99L));
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void anonimizarUsuarioAdmin_anonimiza_cuandoUsuarioNoLoHaSolicitado() {
        when(usuarioRepository.findByIdParaAnonimizar(1L)).thenReturn(Optional.of(usuario));

        RespuestaMensaje respuesta = privacidadService.anonimizarUsuarioAdmin(1L, 99L);

        assertEquals("Datos personales suprimidos exitosamente.", respuesta.getMensaje());
        verify(sessionRevocationService).cambiarEstadoCuenta(1L, false);
    }

    @Test
    void anonimizarUsuarioAdmin_noExigeCodigo2Fa_aunqueElUsuarioLoTengaActivo() {
        // El admin no tiene el código 2FA del usuario; esa vía es exclusiva del autoservicio.
        when(usuarioRepository.findByIdParaAnonimizar(1L)).thenReturn(Optional.of(usuario));
        TwoFactorAuthentication dosFactores = TwoFactorAuthentication.builder().estaHabilitado(true).build();
        lenient().when(autenticacionDosFactoresRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.of(dosFactores));

        RespuestaMensaje respuesta = privacidadService.anonimizarUsuarioAdmin(1L, 99L);

        assertEquals("Datos personales suprimidos exitosamente.", respuesta.getMensaje());
        verifyNoInteractions(twoFactorService);
    }

    @Test
    void solicitarSupresionPropia_lanzaNotFound_siUsuarioNoExiste() {
        when(usuarioRepository.findByIdParaAnonimizar(404L)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> privacidadService.solicitarSupresionPropia(404L, null));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    @Test
    void anonimizarCertificados_limpiaDatosExtraidosYEliminaDocumentoPendiente() {
        when(usuarioRepository.findByIdParaAnonimizar(1L)).thenReturn(Optional.of(usuario));

        AiCertificate certificado = AiCertificate.builder()
                .idCertificado(30L)
                .urlDocumentoS3("verificacion/doc.jpg")
                .hashDocumento("abc123")
                .datosExtraidosIa("{\"nombre\":\"Ana\"}")
                .documentoEliminado(false)
                .build();
        when(certificadoIaRepository.findByUsuarioIdUsuario(1L)).thenReturn(List.of(certificado));

        privacidadService.solicitarSupresionPropia(1L, null);

        assertNull(certificado.getDatosExtraidosIa());
        assertNull(certificado.getHashDocumento());
        assertTrue(certificado.isDocumentoEliminado());
        verify(almacenamientoDocumentos).eliminar("verificacion/doc.jpg");
        verify(certificadoIaRepository).saveAll(List.of(certificado));
    }

    // ── Ajuste 2: step-up de 2FA en la supresión propia ─────────────────────

    @Test
    void solicitarSupresionPropia_exigeCodigo_siTiene2FaActivoYNoEnviaCodigo() {
        when(usuarioRepository.findByIdParaAnonimizar(1L)).thenReturn(Optional.of(usuario));
        TwoFactorAuthentication dosFactores = TwoFactorAuthentication.builder().estaHabilitado(true).build();
        when(autenticacionDosFactoresRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.of(dosFactores));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> privacidadService.solicitarSupresionPropia(1L, null));

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        verify(usuarioRepository, never()).save(any());
        verifyNoInteractions(twoFactorService);
    }

    @Test
    void solicitarSupresionPropia_rechaza_siCodigo2FaEsInvalido() {
        when(usuarioRepository.findByIdParaAnonimizar(1L)).thenReturn(Optional.of(usuario));
        TwoFactorAuthentication dosFactores = TwoFactorAuthentication.builder().estaHabilitado(true).build();
        when(autenticacionDosFactoresRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.of(dosFactores));
        when(twoFactorService.validarCodigoOBackup("ana@example.com", "000000")).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> privacidadService.solicitarSupresionPropia(1L, "000000"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(intentosAutenticacionService).verificarCuota(eq("2fa-supresion-cuenta"), eq("ana@example.com"), anyInt(), any());
        verify(usuarioRepository, never()).save(any());
    }

    @Test
    void solicitarSupresionPropia_procede_siCodigo2FaEsValido() {
        when(usuarioRepository.findByIdParaAnonimizar(1L)).thenReturn(Optional.of(usuario));
        TwoFactorAuthentication dosFactores = TwoFactorAuthentication.builder().estaHabilitado(true).build();
        when(autenticacionDosFactoresRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.of(dosFactores));
        when(twoFactorService.validarCodigoOBackup("ana@example.com", "123456")).thenReturn(true);

        RespuestaMensaje respuesta = privacidadService.solicitarSupresionPropia(1L, "123456");

        assertEquals("Datos personales suprimidos exitosamente.", respuesta.getMensaje());
        verify(intentosAutenticacionService).limpiar("2fa-supresion-cuenta", "ana@example.com");
        verify(sessionRevocationService).cambiarEstadoCuenta(1L, false);
    }

    // ── Ajuste 3: colisión del correo anonimizado ───────────────────────────

    @Test
    void solicitarSupresionPropia_traduceColisionDeCorreo_aConflict() {
        when(usuarioRepository.findByIdParaAnonimizar(1L)).thenReturn(Optional.of(usuario));
        // El SQLException con SQLSTATE 23505 en la cadena de causas es lo que
        // StoredProcedureExceptionTranslator busca para traducir a CONFLICT.
        java.sql.SQLException sqlException = new java.sql.SQLException(
                "ERROR: duplicate key value violates unique constraint \"usuarios_correo_key\"", "23505");
        when(usuarioRepository.save(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("correo duplicado", sqlException));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> privacidadService.solicitarSupresionPropia(1L, null));

        assertEquals(HttpStatus.CONFLICT, exception.getStatusCode());
        verify(sessionRevocationService, never()).cambiarEstadoCuenta(any(), anyBoolean());
    }
}
