package uteq.edu.ec.artisync.service.comunicacion.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaMensajeChat;
import uteq.edu.ec.artisync.dto.respuesta.comunicacion.RespuestaSalaChat;
import uteq.edu.ec.artisync.entity.legal.Mensaje;
import uteq.edu.ec.artisync.entity.legal.SalaChat;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.legal.MensajeRepository;
import uteq.edu.ec.artisync.repository.legal.SalaChatRepository;
import uteq.edu.ec.artisync.repository.seguridad.UserRepository;
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
    private final UserRepository       usuarioRepo;
    private final InfraccionService       infraccionService;
    private final MensajeFilterService    mensajeFilterService;
    private final NotificacionService     notificacionService;
    private final SimpMessagingTemplate   messagingTemplate;

    // -------------------------------------------------------------------------
    // Sala de Chat
    // -------------------------------------------------------------------------

    @Override
    @Transactional
    /**
     * Procesa y persiste la creacion de un nuevo recurso en el contexto de negocio aplicable.
     *
     * @param pedido parametro requerido para la correcta ejecucion del procedimiento
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
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
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idPedido identificador unico que referencia de manera univoca al registro
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
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
    /**
     * Despacha un mensaje o notificacion a los destinatarios especificados.
     *
     * @param idPedido identificador unico que referencia de manera univoca al registro
     * @param idRemitente identificador unico que referencia de manera univoca al registro
     * @param cuerpoMensaje parametro requerido para la correcta ejecucion del procedimiento
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaMensajeChat enviarMensaje(Long idPedido, Long idRemitente, String cuerpoMensaje) {
        SalaChat sala = salaChatRepo.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe sala de chat para el pedido " + idPedido));

        // Los controladores (REST y @MessageMapping) solo exigen
        // isAuthenticated(): sin esto, cualquier usuario logueado podía
        // escribir en el chat de un pedido ajeno.
        verificarParticipante(sala.getPedido(), idRemitente);

        if (Boolean.FALSE.equals(sala.getSalaActiva())) {
            throw new BusinessRuleException("Esta sala ha sido cerrada");
        }

        // RF-15: Filtrar datos de contacto antes de persistir el mensaje.
        // infraccionService.registrarInfraccion corre en su propia transaccion
        // (REQUIRES_NEW): queda confirmada en el motor aunque esta llamada
        // termine lanzando la excepcion de abajo, que hace rollback de la
        // transaccion de enviarMensaje pero no de la de la infraccion.
        if (mensajeFilterService.contieneContacto(cuerpoMensaje)) {
            infraccionService.registrarInfraccion(idRemitente, sala.getPedido().getIdPedido(), cuerpoMensaje);
            throw new BusinessRuleException(
                    "Tu mensaje no fue entregado porque contiene datos de contacto. Infracción registrada.");
        }

        User remitente = usuarioRepo.getReferenceById(idRemitente);
        Mensaje mensaje = Mensaje.builder()
                .sala(sala)
                .remitente(remitente)
                .cuerpoMensaje(cuerpoMensaje)
                .leido(false)
                .build();
        mensaje = mensajeRepo.save(mensaje);

        RespuestaMensajeChat response = mapToResponse(mensaje, remitente);

        // Publicar en el tópico de la sala para entrega en tiempo real
        messagingTemplate.convertAndSend("/topic/sala." + sala.getIdSala(), response);

        // El WS solo llega a quien tenga esa sala abierta en ese momento; sin
        // esto, la otra parte no se enteraba de un mensaje nuevo salvo que
        // entrara a revisar el pedido por su cuenta.
        Pedido pedido = sala.getPedido();
        User destinatario = idRemitente.equals(pedido.getUsuarioCliente().getIdUsuario())
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
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param idPedido identificador unico que referencia de manera univoca al registro
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @param pageable configuracion de paginacion y ordenamiento para la capa de datos
     * @return una estructura de datos paginada con la porcion de resultados solicitada y metadatos de pagina
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public Page<RespuestaMensajeChat> obtenerMensajes(Long idPedido, Long idUsuario, Pageable pageable) {
        SalaChat sala = salaChatRepo.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe sala de chat para el pedido " + idPedido));

        // El controlador solo exige isAuthenticated(): sin esto, cualquier
        // usuario logueado podía leer el historial de un chat ajeno.
        verificarParticipante(sala.getPedido(), idUsuario);

        List<Mensaje> mensajes = mensajeRepo.findBySalaIdSalaOrderByFechaHoraEnvioAsc(sala.getIdSala());
        List<RespuestaMensajeChat> dtos = mensajes.stream()
                .map(m -> mapToResponse(m, m.getRemitente()))
                .toList();

        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), dtos.size());
        List<RespuestaMensajeChat> page = start > dtos.size() ? List.of() : dtos.subList(start, end);
        return new PageImpl<>(page, pageable, dtos.size());
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param idPedido identificador unico que referencia de manera univoca al registro
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public RespuestaSalaChat obtenerEstadoSala(Long idPedido, Long idUsuario) {
        SalaChat sala = salaChatRepo.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException(
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
            throw new BusinessRuleException("No tiene acceso al chat de este pedido");
        }
    }

    // -------------------------------------------------------------------------
    private RespuestaMensajeChat mapToResponse(Mensaje m, User remitente) {
        return RespuestaMensajeChat.builder()
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
