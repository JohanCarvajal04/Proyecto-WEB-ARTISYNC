package uteq.edu.ec.artisync.service.seguridad.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.dto.seguridad.response.TwoFactorSetupResponse;
import uteq.edu.ec.artisync.entity.seguridad.AutenticacionDosFactores;
import uteq.edu.ec.artisync.entity.seguridad.Rol;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.entity.seguridad.UsuarioRol;
import uteq.edu.ec.artisync.repository.perfil.CertificadoIaRepository;
import uteq.edu.ec.artisync.repository.seguridad.AutenticacionDosFactoresRepository;
import uteq.edu.ec.artisync.repository.seguridad.CodigoRespaldo2FaRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRolRepository;
import uteq.edu.ec.artisync.service.shared.IntentosAutenticacionService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TwoFactorServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AutenticacionDosFactoresRepository autenticacionDosFactoresRepository;
    @Mock
    private CodigoRespaldo2FaRepository codigoRespaldo2FaRepository;
    @Mock
    private UsuarioRolRepository usuarioRolRepository;
    @Mock
    private CertificadoIaRepository certificadoIaRepository;
    @Mock
    private IntentosAutenticacionService intentosAutenticacionService;

    @InjectMocks
    private TwoFactorServiceImpl twoFactorService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .idUsuario(1L)
                .correo("creador@example.com")
                .build();
        // @InjectMocks no procesa @Value; se fuerza el campo, igual que
        // PagoServicioImplWebhookTest hace con sus propios @Value.
        ReflectionTestUtils.setField(twoFactorService, "claveHmac", "clave-de-prueba-para-tests-2fa-1234567890");
    }

    @Test
    void setup2Fa_ShouldThrowForbidden_WhenUserIsCreadorAndNotApproved() {
        when(usuarioRepository.findByCorreo("creador@example.com")).thenReturn(Optional.of(usuario));
        Rol rolCreador = Rol.builder().nombreRol("CREADOR").build();
        UsuarioRol ur = UsuarioRol.builder().rol(rolCreador).build();
        when(usuarioRolRepository.findByUsuarioIdUsuario(1L)).thenReturn(List.of(ur));
        
        when(certificadoIaRepository.existsByUsuarioIdUsuarioAndTipoDocumentoAndEstadoVerificacionNombreEstado(1L, "IDENTIDAD", "APROBADO")).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> twoFactorService.setup2Fa("creador@example.com"));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertTrue(exception.getReason().contains("verificar tu identidad"));
    }

    @Test
    // Fase 3 concurrencia: setup2Fa delega en fn_configurar_2fa
    // (autenticacionDosFactoresRepository.configurar2Fa), que hace el upsert
    // del secreto + reemplazo de codigos de respaldo en una sola transaccion.
    void setup2Fa_ShouldSucceed_WhenUserIsCliente() {
        when(usuarioRepository.findByCorreo("cliente@example.com")).thenReturn(Optional.of(usuario));
        Rol rolCliente = Rol.builder().nombreRol("CLIENTE").build();
        UsuarioRol ur = UsuarioRol.builder().rol(rolCliente).build();
        when(usuarioRolRepository.findByUsuarioIdUsuario(1L)).thenReturn(List.of(ur));

        TwoFactorSetupResponse response = twoFactorService.setup2Fa("cliente@example.com");

        assertNotNull(response);
        assertNotNull(response.getSecreto());
        assertEquals(8, response.getCodigosRespaldo().size());
        verify(autenticacionDosFactoresRepository).configurar2Fa(eq(1L), eq(response.getSecreto()), any());
    }

    // ── validarCodigoOBackup: codigos de respaldo (Fase 1 concurrencia) ─────
    // fn_consumir_codigo_respaldo_2fa (docs/basedatos/PLAN-CONCURRENCIA-SP.md §2)
    // hace el UPDATE atomico en una sola llamada al motor; estas pruebas
    // verifican que el servicio delega en ella en vez de leer todos los codigos
    // no usados a memoria y compararlos en un bucle Java.

    @Test
    void validarCodigoOBackup_ShouldReturnTrue_WhenCodigoDeRespaldoNoUsado() {
        AutenticacionDosFactores dosFactores = AutenticacionDosFactores.builder()
                .llaveSecreta("SECRETO").estaHabilitado(true).build();
        when(usuarioRepository.findByCorreo("creador@example.com")).thenReturn(Optional.of(usuario));
        when(autenticacionDosFactoresRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.of(dosFactores));
        when(codigoRespaldo2FaRepository.consumirCodigoRespaldo(eq(1L), anyString())).thenReturn(true);

        boolean resultado = twoFactorService.validarCodigoOBackup("creador@example.com", "ABCD1234");

        assertTrue(resultado);
        verify(codigoRespaldo2FaRepository).consumirCodigoRespaldo(eq(1L), anyString());
    }

    @Test
    void validarCodigoOBackup_ShouldReturnFalse_WhenCodigoYaFueConsumidoPorOtraPeticionConcurrente() {
        // Simula la anomalia que la Fase 1 cierra: el UPDATE atomico ya no
        // encuentra la fila (otra transaccion la marco usado = TRUE primero) y
        // devuelve FALSE en vez de "true" dos veces.
        AutenticacionDosFactores dosFactores = AutenticacionDosFactores.builder()
                .llaveSecreta("SECRETO").estaHabilitado(true).build();
        when(usuarioRepository.findByCorreo("creador@example.com")).thenReturn(Optional.of(usuario));
        when(autenticacionDosFactoresRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.of(dosFactores));
        when(codigoRespaldo2FaRepository.consumirCodigoRespaldo(eq(1L), anyString())).thenReturn(false);

        boolean resultado = twoFactorService.validarCodigoOBackup("creador@example.com", "ABCD1234");

        assertFalse(resultado);
    }

    // ── disable2Fa: cuota de intentos por cuenta (revisión técnica 2026-09-01) ──
    // Ni confirm2Fa ni disable2Fa tenían límite de intentos: con una sesión
    // válida, un atacante podía probar sin freno los 10^6 códigos TOTP.

    @Test
    void disable2Fa_ShouldIncrementarCuota_WhenCodigoIncorrecto() {
        AutenticacionDosFactores dosFactores = AutenticacionDosFactores.builder()
                .llaveSecreta("SECRETO").estaHabilitado(true).build();
        when(usuarioRepository.findByCorreo("creador@example.com")).thenReturn(Optional.of(usuario));
        when(autenticacionDosFactoresRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.of(dosFactores));
        when(codigoRespaldo2FaRepository.consumirCodigoRespaldo(eq(1L), anyString())).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> twoFactorService.disable2Fa("creador@example.com", "WRONGCODE"));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verify(intentosAutenticacionService).verificarCuota(
                eq("2fa-desactivar-cuenta"), eq("creador@example.com"), anyInt(), any());
        verify(autenticacionDosFactoresRepository, never()).desactivar2Fa(any());
    }

    @Test
    void disable2Fa_ShouldLimpiarCuota_WhenCodigoCorrecto() {
        AutenticacionDosFactores dosFactores = AutenticacionDosFactores.builder()
                .llaveSecreta("SECRETO").estaHabilitado(true).build();
        when(usuarioRepository.findByCorreo("creador@example.com")).thenReturn(Optional.of(usuario));
        when(autenticacionDosFactoresRepository.findByUsuarioIdUsuario(1L)).thenReturn(Optional.of(dosFactores));
        when(codigoRespaldo2FaRepository.consumirCodigoRespaldo(eq(1L), anyString())).thenReturn(true);

        twoFactorService.disable2Fa("creador@example.com", "ABCD1234");

        verify(intentosAutenticacionService).limpiar("2fa-desactivar-cuenta", "creador@example.com");
        verify(intentosAutenticacionService, never()).verificarCuota(anyString(), anyString(), anyInt(), any());
        verify(autenticacionDosFactoresRepository).desactivar2Fa(1L);
    }
}
