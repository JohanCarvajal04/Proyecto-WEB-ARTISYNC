package uteq.edu.ec.artisync.service.comunicacion.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaInfraccion;
import uteq.edu.ec.artisync.entity.comunicacion.InfraccionMensaje;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.repository.comunicacion.InfraccionRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.service.comunicacion.MensajeFilterService;
import uteq.edu.ec.artisync.service.comunicacion.NotificacionService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para InfraccionServiceImpl.registrarInfraccion (RF-15).
 * Es el unico punto de entrada real para registrar infracciones: ChatServiceImpl
 * delega aqui para que el INSERT + conteo + suspension corran en su propia
 * transaccion (REQUIRES_NEW), independiente de la del llamador.
 */
@ExtendWith(MockitoExtension.class)
class InfraccionServiceImplTest {

    @Mock private InfraccionRepository infraccionRepo;
    @Mock private UsuarioRepository    usuarioRepo;
    @Mock private MensajeFilterService mensajeFilterService;
    @Mock private NotificacionService  notificacionService;

    private InfraccionServiceImpl infraccionService;

    @BeforeEach
    void setUp() {
        infraccionService = new InfraccionServiceImpl(
                infraccionRepo, usuarioRepo, mensajeFilterService, notificacionService, new ObjectMapper());
    }

    @Test
    @DisplayName("registrarInfraccion — por debajo del umbral no suspende ni notifica")
    void registrarInfraccion_bajoUmbral_noSuspende() {
        String mensaje = "Llámame al +593 99 123 4567";
        when(mensajeFilterService.detectarPatron(mensaje)).thenReturn("TELEFONO");
        when(infraccionRepo.registrarInfraccion(1L, 10L, mensaje, "TELEFONO"))
                .thenReturn("{\"idInfraccion\":1,\"totalInfraccionesPeriodo\":1,\"cuentaSuspendida\":false}");

        assertThatCode(() -> infraccionService.registrarInfraccion(1L, 10L, mensaje))
                .doesNotThrowAnyException();

        verify(infraccionRepo).registrarInfraccion(1L, 10L, mensaje, "TELEFONO");
        verify(notificacionService, never()).notificar(any(), eq("CUENTA_SUSPENDIDA"), anyString());
        verifyNoInteractions(usuarioRepo);
    }

    @Test
    @DisplayName("registrarInfraccion — al cruzar el umbral suspende la cuenta y notifica")
    void registrarInfraccion_cruzaUmbral_suspendeYNotifica() {
        String mensaje = "Escríbeme a test@ejemplo.com";
        Usuario usuario = Usuario.builder().idUsuario(1L).correo("test-user@example.com").estadoCuenta(false).build();

        when(mensajeFilterService.detectarPatron(mensaje)).thenReturn("EMAIL");
        when(infraccionRepo.registrarInfraccion(1L, 10L, mensaje, "EMAIL"))
                .thenReturn("{\"idInfraccion\":3,\"totalInfraccionesPeriodo\":3,\"cuentaSuspendida\":true}");
        when(usuarioRepo.findById(1L)).thenReturn(Optional.of(usuario));

        infraccionService.registrarInfraccion(1L, 10L, mensaje);

        verify(notificacionService).notificar(eq(usuario), eq("CUENTA_SUSPENDIDA"), anyString());
    }

    @Test
    @DisplayName("registrarInfraccion — cuenta ya suspendida antes (cuentaSuspendida=false) no repite la notificación")
    void registrarInfraccion_yaSuspendida_noRepiteNotificacion() {
        String mensaje = "Contáctame por whatsapp al 0991234567";
        when(mensajeFilterService.detectarPatron(mensaje)).thenReturn("TELEFONO");
        when(infraccionRepo.registrarInfraccion(1L, 10L, mensaje, "TELEFONO"))
                .thenReturn("{\"idInfraccion\":5,\"totalInfraccionesPeriodo\":5,\"cuentaSuspendida\":false}");

        infraccionService.registrarInfraccion(1L, 10L, mensaje);

        verifyNoInteractions(usuarioRepo);
        verify(notificacionService, never()).notificar(any(), eq("CUENTA_SUSPENDIDA"), anyString());
    }

    @Test
    @DisplayName("historialPorUsuario — filtra en la consulta, no devuelve infracciones de otros usuarios")
    void historialPorUsuario_filtraPorUsuarioEnLaQuery() {
        Usuario usuario1 = Usuario.builder().idUsuario(1L).nombres("Juan").apellidos("Pérez").correo("juan@example.com").build();
        Pedido pedido = Pedido.builder().idPedido(10L).build();
        InfraccionMensaje infraccionDeUsuario1 = InfraccionMensaje.builder()
                .idInfraccion(7L)
                .usuario(usuario1)
                .pedido(pedido)
                .patronDetectado("EMAIL")
                .fechaInfraccion(LocalDateTime.now())
                .build();

        Pageable pageable = PageRequest.of(0, 10);
        when(infraccionRepo.findByUsuarioIdUsuario(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(infraccionDeUsuario1), pageable, 1));

        var resultado = infraccionService.historialPorUsuario(1L, pageable);

        assertThat(resultado.getTotalElements()).isEqualTo(1);
        assertThat(resultado.getContent()).extracting(RespuestaInfraccion::getIdUsuario).containsOnly(1L);
        verify(infraccionRepo).findByUsuarioIdUsuario(1L, pageable);
        verify(infraccionRepo, never()).findAll(any(Pageable.class));
    }
}
