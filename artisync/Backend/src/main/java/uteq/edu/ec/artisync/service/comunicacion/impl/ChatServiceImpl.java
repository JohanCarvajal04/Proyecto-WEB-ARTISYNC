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
import uteq.edu.ec.artisync.entity.legal.Mensaje;
import uteq.edu.ec.artisync.entity.legal.SalaChat;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.legal.MensajeRepository;
import uteq.edu.ec.artisync.repository.legal.SalaChatRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRepository;
import uteq.edu.ec.artisync.service.comunicacion.ChatService;
import uteq.edu.ec.artisync.service.comunicacion.InfraccionService;
import uteq.edu.ec.artisync.service.comunicacion.MensajeFilterService;
import uteq.edu.ec.artisync.service.comunicacion.NotificacionService;

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

    private final SalaChatRepository      salaChatRepo;
    private final MensajeRepository       mensajeRepo;
    private final UsuarioRepository       usuarioRepo;
    private final InfraccionService       infraccionService;
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

        // Los controladores (REST y @MessageMapping) solo exigen
        // isAuthenticated(): sin esto, cualquier usuario logueado podía
        // escribir en el chat de un pedido ajeno.
        verificarParticipante(sala.getPedido(), idRemitente);

        if (Boolean.FALSE.equals(sala.getSalaActiva())) {
            throw new ExcepcionReglaNegocio("Esta sala ha sido cerrada");
        }

        // RF-15: Filtrar datos de contacto antes de persistir el mensaje.
        // infraccionService.registrarInfraccion corre en su propia transaccion
        // (REQUIRES_NEW): queda confirmada en el motor aunque esta llamada
        // termine lanzando la excepcion de abajo, que hace rollback de la
        // transaccion de enviarMensaje pero no de la de la infraccion.
        if (mensajeFilterService.contieneContacto(cuerpoMensaje)) {
            infraccionService.registrarInfraccion(idRemitente, sala.getPedido().getIdPedido(), cuerpoMensaje);
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

        // El WS solo llega a quien tenga esa sala abierta en ese momento; sin
        // esto, la otra parte no se enteraba de un mensaje nuevo salvo que
        // entrara a revisar el pedido por su cuenta.
        Pedido pedido = sala.getPedido();
        Usuario destinatario = idRemitente.equals(pedido.getUsuarioCliente().getIdUsuario())
                ? pedido.getServicio().getPerfil().getUsuario()
                : pedido.getUsuarioCliente();
        notificacionService.notificar(destinatario, "MENSAJE_RECIBIDO",
                remitente.getNombres() + " te escribió en \"" + pedido.getServicio().getTituloServicio()
                        + "\": " + resumirMensaje(cuerpoMensaje));

        return response;
    }

    private String resumirMensaje(String cuerpoMensaje) {
        final int maxCaracteres = 80;
        return cuerpoMensaje.length() > maxCaracteres
                ? cuerpoMensaje.substring(0, maxCaracteres) + "…"
                : cuerpoMensaje;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<RespuestaMensaje> obtenerMensajes(Long idPedido, Long idUsuario, Pageable pageable) {
        SalaChat sala = salaChatRepo.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "No existe sala de chat para el pedido " + idPedido));

        // El controlador solo exige isAuthenticated(): sin esto, cualquier
        // usuario logueado podía leer el historial de un chat ajeno.
        verificarParticipante(sala.getPedido(), idUsuario);

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
    public RespuestaSalaChat obtenerEstadoSala(Long idPedido, Long idUsuario) {
        SalaChat sala = salaChatRepo.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado(
                        "No existe sala de chat para el pedido " + idPedido));

        // El controlador solo exige isAuthenticated(): sin esto, cualquier
        // usuario logueado podía ver el estado del chat de un pedido ajeno.
        verificarParticipante(sala.getPedido(), idUsuario);

        return RespuestaSalaChat.builder()
                .idSala(sala.getIdSala())
                .idPedido(sala.getPedido().getIdPedido())
                .salaActiva(sala.getSalaActiva())
                .fechaApertura(sala.getFechaApertura())
                .build();
    }

    /**
     * Único chequeo de pertenencia al pedido, reutilizado por los tres
     * métodos de arriba: nadie fuera del cliente o el creador del pedido
     * puede leer, escuchar o escribir en su sala de chat.
     */
    private void verificarParticipante(Pedido pedido, Long idUsuario) {
        boolean esCliente = pedido.getUsuarioCliente().getIdUsuario().equals(idUsuario);
        boolean esCreador = pedido.getServicio().getPerfil().getUsuario().getIdUsuario().equals(idUsuario);
        if (!esCliente && !esCreador) {
            throw new ExcepcionReglaNegocio("No tiene acceso al chat de este pedido");
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
