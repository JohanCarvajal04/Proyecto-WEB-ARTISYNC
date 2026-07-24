package uteq.edu.ec.artisync.service.comunicacion.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaSalaChat;
import uteq.edu.ec.artisync.entity.comunicacion.InfraccionMensaje;
import uteq.edu.ec.artisync.entity.legal.Mensaje;
import uteq.edu.ec.artisync.entity.legal.SalaChat;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.comunicacion.InfraccionRepository;
import uteq.edu.ec.artisync.repository.legal.MensajeRepository;
import uteq.edu.ec.artisync.repository.legal.SalaChatRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.service.comunicacion.ChatService;
import uteq.edu.ec.artisync.service.comunicacion.MensajeFilterService;
import uteq.edu.ec.artisync.service.comunicacion.NotificacionService;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Implementación del servicio de chat.
 * RF-14: Chat en tiempo real vía WebSocket.
 * RF-15: Filtrado de datos de contacto con registro de infracciones.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private static final int MAX_INFRACCIONES = 3;
    private static final int PERIODO_DIAS     = 30;

    private final SalaChatRepository      salaChatRepo;
    private final MensajeRepository       mensajeRepo;
    private final UsuarioRepository       usuarioRepo;
    private final InfraccionRepository    infraccionRepo;
    private final MensajeFilterService    mensajeFilterService;
    private final NotificacionService     notificacionService;
    private final SimpMessagingTemplate   messagingTemplate;

    // -------------------------------------------------------------------------
    // Sala de Chat
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public SalaChat crearSala(Pedido pedido) {
        // Prevenir duplicados: un pedido → una sala
        return salaChatRepo.findByPedidoIdPedido(pedido.getIdPedido())
                .orElseGet(() -> {
                    SalaChat sala = SalaChat.builder()
                            .pedido(pedido)
                            .salaActiva(true)
                            .build();
                    log.info("Sala de chat creada para pedido {}", pedido.getIdPedido());
                    return salaChatRepo.save(sala);
                });
    }

    @Override
    @Transactional
    public void cerrarSala(Long idPedido) {
        salaChatRepo.findByPedidoIdPedido(idPedido).ifPresent(sala -> {
            sala.setSalaActiva(false);
            salaChatRepo.save(sala);

            // Notificar a los participantes que la sala fue cerrada
            messagingTemplate.convertAndSend(
                    "/topic/sala." + sala.getIdSala(),
                    (Object) Map.of("tipo", "SALA_CERRADA", "mensaje", "Esta sala ha sido cerrada")
            );
            log.info("Sala {} cerrada para pedido {}", sala.getIdSala(), idPedido);
        });
    }

    // -------------------------------------------------------------------------
    // Mensajes
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    public RespuestaMensaje enviarMensaje(Long idPedido, Long idRemitente, String cuerpoMensaje) {
        SalaChat sala = salaChatRepo.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "No existe sala de chat para el pedido " + idPedido));

        if (Boolean.FALSE.equals(sala.getSalaActiva())) {
            throw new ExcepcionReglaNegocio("Esta sala ha sido cerrada");
        }

        // RF-15: Filtrar datos de contacto antes de persistir el mensaje
        if (mensajeFilterService.contieneContacto(cuerpoMensaje)) {
            registrarInfraccionInterna(idRemitente, sala.getPedido(), cuerpoMensaje);
            throw new ExcepcionReglaNegocio(
                    "Tu mensaje no fue entregado porque contiene datos de contacto. Infracción registrada.");
        }

        Usuario remitente = usuarioRepo.getReferenceById(idRemitente);
        Mensaje mensaje = Mensaje.builder()
                .sala(sala)
                .remitente(remitente)
                .cuerpoMensaje(cuerpoMensaje)
                .leido(false)
                .build();
        mensaje = mensajeRepo.save(mensaje);

        RespuestaMensaje response = mapToResponse(mensaje, remitente);

        // Publicar en el tópico de la sala para entrega en tiempo real
        messagingTemplate.convertAndSend("/topic/sala." + sala.getIdSala(), response);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RespuestaMensaje> obtenerMensajes(Long idPedido, Pageable pageable) {
        SalaChat sala = salaChatRepo.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "No existe sala de chat para el pedido " + idPedido));

        List<Mensaje> mensajes = mensajeRepo.findBySalaIdSalaOrderByFechaHoraEnvioAsc(sala.getIdSala());
        List<RespuestaMensaje> dtos = mensajes.stream()
                .map(m -> mapToResponse(m, m.getRemitente()))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), dtos.size());
        List<RespuestaMensaje> page = start > dtos.size() ? List.of() : dtos.subList(start, end);
        return new PageImpl<>(page, pageable, dtos.size());
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaSalaChat obtenerEstadoSala(Long idPedido) {
        SalaChat sala = salaChatRepo.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "No existe sala de chat para el pedido " + idPedido));

        return RespuestaSalaChat.builder()
                .idSala(sala.getIdSala())
                .idPedido(sala.getPedido().getIdPedido())
                .salaActiva(sala.getSalaActiva())
                .fechaApertura(sala.getFechaApertura())
                .build();
    }

    // -------------------------------------------------------------------------
    // Infracciones (RF-15) — lógica interna de Chat
    // -------------------------------------------------------------------------

    private void registrarInfraccionInterna(Long idRemitente, Pedido pedido, String cuerpoMensaje) {
        Usuario usuario = usuarioRepo.findById(idRemitente)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Usuario no encontrado: " + idRemitente));

        String patron = mensajeFilterService.detectarPatron(cuerpoMensaje);

        InfraccionMensaje infraccion = InfraccionMensaje.builder()
                .usuario(usuario)
                .pedido(pedido)
                .mensajeOriginal(cuerpoMensaje)
                .patronDetectado(patron)
                .build();
        infraccionRepo.save(infraccion);

        long count = infraccionRepo.countByUsuarioIdUsuarioAndFechaInfraccionAfter(
                idRemitente, LocalDateTime.now().minusDays(PERIODO_DIAS));

        log.warn("Infracción RF-15 registrada para usuario {}. Patrón: {}. Total en 30 días: {}",
                idRemitente, patron, count);

        if (count >= MAX_INFRACCIONES) {
            usuario.setEstadoCuenta(false);
            usuarioRepo.save(usuario);
            LocalDateTime hastaFecha = LocalDateTime.now().plusDays(15);
            String mensaje = "Tu cuenta está suspendida hasta " + hastaFecha.toLocalDate()
                    + " por superar el límite de infracciones de datos de contacto.";
            notificacionService.notificar(usuario, "CUENTA_SUSPENDIDA", mensaje);
            log.warn("Cuenta del usuario {} suspendida hasta {}", usuario.getCorreo(), hastaFecha.toLocalDate());
        }
    }

    // -------------------------------------------------------------------------
    private RespuestaMensaje mapToResponse(Mensaje m, Usuario remitente) {
        return RespuestaMensaje.builder()
                .idMensaje(m.getIdMensaje())
                .idSala(m.getSala().getIdSala())
                .idRemitente(remitente.getIdUsuario())
                .nombreRemitente(remitente.getNombres() + " " + remitente.getApellidos())
                .cuerpoMensaje(m.getCuerpoMensaje())
                .fechaHoraEnvio(m.getFechaHoraEnvio())
                .leido(m.getLeido())
                .build();
    }
}
