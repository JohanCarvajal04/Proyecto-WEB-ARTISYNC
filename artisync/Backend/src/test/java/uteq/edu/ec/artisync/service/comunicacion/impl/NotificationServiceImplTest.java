package uteq.edu.ec.artisync.service.comunicacion.impl;

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
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.NotificationResponse;
import uteq.edu.ec.artisync.entity.comunicacion.SystemNotification;
import uteq.edu.ec.artisync.entity.comunicacion.NotificationType;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.repository.comunicacion.SystemNotificationRepository;
import uteq.edu.ec.artisync.repository.comunicacion.NotificationTypeRepository;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * El texto de cada notificación debe guardarse por instancia (columna
 * {@code mensaje} de notificaciones_sistema, migración V14). Antes se leía
 * de {@code tipoNotificacion.formatoMensaje} — un campo compartido por TODAS
 * las notificaciones del mismo evento y fijado solo la primera vez que ese
 * evento se disparaba — así que dos notificaciones del mismo tipo con
 * contenido distinto (p. ej. dos mensajes de chat) terminaban mostrando,
 * al recargar, el mismo texto de la primera.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class NotificationServiceImplTest {

    @Mock private SystemNotificationRepository notificacionRepo;
    @Mock private NotificationTypeRepository tipoNotificacionRepo;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificationServiceImpl notificacionService;

    private User destinatario;
    private NotificationType tipo;

    @BeforeEach
    void setUp() {
        destinatario = User.builder().idUsuario(1L).correo("user@test.com").build();
        tipo = NotificationType.builder().idTipoNotificacion(1L).nombreEvento("MENSAJE_RECIBIDO")
                .formatoMensaje("primer mensaje que disparó el tipo").build();
    }

    @Test
    @DisplayName("notify guarda el texto propio de cada notificación, no el del tipo")
    void notificar_guardaMensajePropio() {
        given(tipoNotificacionRepo.findByNombreEvento("MENSAJE_RECIBIDO")).willReturn(Optional.of(tipo));
        given(notificacionRepo.save(any(SystemNotification.class))).willAnswer(inv -> inv.getArgument(0));

        notificacionService.notify(destinatario, "MENSAJE_RECIBIDO", "Juan te escribió: hola");

        ArgumentCaptor<SystemNotification> captor = ArgumentCaptor.forClass(SystemNotification.class);
        verify(notificacionRepo).save(captor.capture());
        assertThat(captor.getValue().getMensaje()).isEqualTo("Juan te escribió: hola");
    }

    @Test
    @DisplayName("notify no propaga una falla del envío WebSocket -- es un efecto secundario de mejor esfuerzo")
    void notificar_noPropagaFalloDeEnvio() {
        // Si notify() se llama dentro de la transacción de una operación
        // de negocio real (pago liberado, ganador de sorteo...), dejar
        // escapar esta excepción marcaría esa transacción como
        // rollback-only y revertiría en silencio el cambio ya confirmado.
        given(tipoNotificacionRepo.findByNombreEvento("MENSAJE_RECIBIDO")).willReturn(Optional.of(tipo));
        given(notificacionRepo.save(any(SystemNotification.class))).willAnswer(inv -> inv.getArgument(0));
        org.mockito.Mockito.doThrow(new RuntimeException("broker STOMP caído"))
                .when(messagingTemplate).convertAndSendToUser(org.mockito.ArgumentMatchers.anyString(),
                        org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any());

        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() ->
                notificacionService.notify(destinatario, "MENSAJE_RECIBIDO", "hola"));
    }

    @Test
    @DisplayName("listMyNotifications muestra el texto propio de cada una, no uno compartido")
    void listarMisNotificaciones_dosDelMismoTipoConTextoDistinto() {
        SystemNotification n1 = SystemNotification.builder()
                .idNotificacion(1L).usuario(destinatario).tipoNotificacion(tipo)
                .mensaje("Juan te escribió: primer mensaje").estaLeida(false).build();
        SystemNotification n2 = SystemNotification.builder()
                .idNotificacion(2L).usuario(destinatario).tipoNotificacion(tipo)
                .mensaje("Juan te escribió: segundo mensaje, totalmente distinto").estaLeida(false).build();

        given(notificacionRepo.findByUsuarioIdUsuarioOrderByFechaEmisionDesc(1L, PageRequest.of(0, 10)))
                .willReturn(new PageImpl<>(List.of(n2, n1)));

        List<NotificationResponse> resultado = notificacionService
                .listMyNotifications(1L, PageRequest.of(0, 10)).getContent();

        assertThat(resultado).extracting(NotificationResponse::getMensaje)
                .containsExactly(
                        "Juan te escribió: segundo mensaje, totalmente distinto",
                        "Juan te escribió: primer mensaje");
    }
}
