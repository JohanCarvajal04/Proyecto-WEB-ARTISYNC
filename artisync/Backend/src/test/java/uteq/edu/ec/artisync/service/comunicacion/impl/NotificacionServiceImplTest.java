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
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaNotificacion;
import uteq.edu.ec.artisync.entity.comunicacion.NotificacionSistema;
import uteq.edu.ec.artisync.entity.comunicacion.TipoNotificacion;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.repository.comunicacion.NotificacionSistemaRepository;
import uteq.edu.ec.artisync.repository.comunicacion.TipoNotificacionRepository;

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
class NotificacionServiceImplTest {

    @Mock private NotificacionSistemaRepository notificacionRepo;
    @Mock private TipoNotificacionRepository tipoNotificacionRepo;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks
    private NotificacionServiceImpl notificacionService;

    private Usuario destinatario;
    private TipoNotificacion tipo;

    @BeforeEach
    void setUp() {
        destinatario = Usuario.builder().idUsuario(1L).correo("user@test.com").build();
        tipo = TipoNotificacion.builder().idTipoNotificacion(1L).nombreEvento("MENSAJE_RECIBIDO")
                .formatoMensaje("primer mensaje que disparó el tipo").build();
    }

    @Test
    @DisplayName("notificar guarda el texto propio de cada notificación, no el del tipo")
    void notificar_guardaMensajePropio() {
        given(tipoNotificacionRepo.findByNombreEvento("MENSAJE_RECIBIDO")).willReturn(Optional.of(tipo));
        given(notificacionRepo.save(any(NotificacionSistema.class))).willAnswer(inv -> inv.getArgument(0));

        notificacionService.notificar(destinatario, "MENSAJE_RECIBIDO", "Juan te escribió: hola");

        ArgumentCaptor<NotificacionSistema> captor = ArgumentCaptor.forClass(NotificacionSistema.class);
        verify(notificacionRepo).save(captor.capture());
        assertThat(captor.getValue().getMensaje()).isEqualTo("Juan te escribió: hola");
    }

    @Test
    @DisplayName("listarMisNotificaciones muestra el texto propio de cada una, no uno compartido")
    void listarMisNotificaciones_dosDelMismoTipoConTextoDistinto() {
        NotificacionSistema n1 = NotificacionSistema.builder()
                .idNotificacion(1L).usuario(destinatario).tipoNotificacion(tipo)
                .mensaje("Juan te escribió: primer mensaje").estaLeida(false).build();
        NotificacionSistema n2 = NotificacionSistema.builder()
                .idNotificacion(2L).usuario(destinatario).tipoNotificacion(tipo)
                .mensaje("Juan te escribió: segundo mensaje, totalmente distinto").estaLeida(false).build();

        given(notificacionRepo.findByUsuarioIdUsuarioOrderByFechaEmisionDesc(1L, PageRequest.of(0, 10)))
                .willReturn(new PageImpl<>(List.of(n2, n1)));

        List<RespuestaNotificacion> resultado = notificacionService
                .listarMisNotificaciones(1L, PageRequest.of(0, 10)).getContent();

        assertThat(resultado).extracting(RespuestaNotificacion::getMensaje)
                .containsExactly(
                        "Juan te escribió: segundo mensaje, totalmente distinto",
                        "Juan te escribió: primer mensaje");
    }
}
