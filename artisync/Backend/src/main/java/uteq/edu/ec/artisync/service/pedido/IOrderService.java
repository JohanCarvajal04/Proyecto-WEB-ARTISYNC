package uteq.edu.ec.artisync.service.pedido;

import uteq.edu.ec.artisync.dto.peticion.pedido.AdvanceStageRequest;
import uteq.edu.ec.artisync.dto.peticion.pedido.CreateOrderRequest;
import uteq.edu.ec.artisync.dto.peticion.pedido.CreateTermsProposalRequest;
import uteq.edu.ec.artisync.dto.respuesta.pedido.StatusHistoryResponse;
import uteq.edu.ec.artisync.dto.respuesta.pedido.OrderResponse;
import uteq.edu.ec.artisync.dto.respuesta.pedido.OrderSummaryResponse;
import uteq.edu.ec.artisync.dto.respuesta.pedido.TermsProposalResponse;
import uteq.edu.ec.artisync.dto.respuesta.pedido.OrderTrackingResponse;
import uteq.edu.ec.artisync.service.shared.reporte.GeneratedDocument;
import uteq.edu.ec.artisync.service.shared.reporte.ReportFormat;

import java.util.List;

public interface IOrderService {

    /**
     * Crea un pedido de un cliente sobre un servicio del catálogo, validando
     * identidad verificada, que el cliente no sea el propio creador y, si el
     * servicio exige cuestionario, que venga completo.
     *
     * @param idCliente id del usuario que actúa como cliente
     * @param peticion  datos del pedido a crear (servicio, precio ofrecido, respuestas de briefing, etc.)
     * @return el pedido recién creado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el cliente o el servicio no existen
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si la identidad no está verificada, el cliente es dueño del servicio, el flujo no tiene etapas configuradas o falta responder el cuestionario
     */
    OrderResponse createOrder(Long idCliente, CreateOrderRequest peticion);

    /**
     * Obtiene el detalle de un pedido, validando que el solicitante sea parte
     * del pedido (cliente o creador) o un administrador.
     *
     * @param idPedido            id del pedido
     * @param idUsuarioSolicitante id del usuario que consulta
     * @return el detalle del pedido
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido no existe
     */
    OrderResponse getOrderById(Long idPedido, Long idUsuarioSolicitante);

    /**
     * Lista, en formato resumido, los pedidos donde el usuario indicado actúa como cliente.
     *
     * @param idCliente id del cliente
     * @return los pedidos del cliente, ordenados según el repositorio
     */
    List<OrderSummaryResponse> listMyOrders(Long idCliente);

    /**
     * Lista, en formato resumido, los pedidos (comisiones) donde el usuario indicado actúa como creador.
     *
     * @param idCreador id del creador
     * @return las comisiones del creador, ordenadas según el repositorio
     */
    List<OrderSummaryResponse> listMyCommissions(Long idCreador);

    /**
     * Exportación "propia": el permiso lo da ya tener sesión como el cliente
     * dueño de estos pedidos, no hay un permiso de exportación aparte (a
     * diferencia de auditoría/finanzas/contratos, que son reportes
     * administrativos transversales).
     *
     * @param idCliente         id del cliente cuyos pedidos se exportan
     * @param formato           formato del documento a generar
     * @param correoSolicitante correo de quien solicita la exportación, registrado en el documento
     * @return el documento generado con el listado de pedidos del cliente
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el listado excede el tope de filas admitido por el formato
     */
    GeneratedDocument exportMyOrders(Long idCliente, ReportFormat formato, String correoSolicitante);

    /**
     * 1.4 (INFORME-REVISION-COMPLETA.md): {@code idsPedido} nulo o vacío exporta
     * todas las comisiones (compatibilidad); si llega con valores, se exportan
     * solo esos pedidos — el frontend manda ahí los ids ya visibles tras
     * aplicar sus filtros de pantalla (estado/etapa/búsqueda), así se garantiza
     * "se exporta lo que se ve" sin duplicar esa lógica de filtrado en Java.
     *
     * @param idCreador         id del creador cuyas comisiones se exportan
     * @param idsPedido         ids opcionales para restringir el listado a un subconjunto ya visible para el creador
     * @param formato           formato del documento a generar
     * @param correoSolicitante correo de quien solicita la exportación, registrado en el documento
     * @return el documento generado con el listado de comisiones del creador
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el listado excede el tope de filas admitido por el formato
     */
    GeneratedDocument exportMyCommissions(Long idCreador, List<Long> idsPedido, ReportFormat formato, String correoSolicitante);

    /**
     * Avanza el pedido a la siguiente etapa del flujo de trabajo configurado,
     * validando que quien avanza sea el creador del servicio y que la etapa
     * actual no exija un entregable pendiente.
     *
     * @param idPedido  id del pedido a avanzar
     * @param idCreador id del creador que solicita el avance
     * @param peticion  observación asociada a la transición
     * @return el pedido con su etapa ya actualizada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el solicitante no es el creador del servicio, la etapa actual ya no está en el flujo, exige un entregable pendiente o el pedido ya está en la etapa final
     */
    OrderResponse advanceStage(Long idPedido, Long idCreador, AdvanceStageRequest peticion);

    /**
     * Propone un precio y/o fecha de entrega final, negociados por chat,
     * antes de que el contrato tenga alguna firma. Puede llamarlo el cliente
     * o el creador del pedido. El cambio no se aplica al pedido hasta que la
     * contraparte lo acepte con {@link #acceptTermsProposal}.
     *
     * @param idPedido  id del pedido sobre el que se proponen los términos
     * @param idUsuario id del usuario que propone (cliente o creador del pedido)
     * @param peticion  precio y/o fecha de entrega propuestos
     * @return la propuesta de términos recién creada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si no se indica ningún término, el usuario no es parte del pedido, el contrato ya tiene alguna firma o ya existe una propuesta pendiente
     */
    TermsProposalResponse proposeTerms(Long idPedido, Long idUsuario, CreateTermsProposalRequest peticion);

    /**
     * Solo la contraparte del proponente puede aceptar. Aplica los valores
     * propuestos al pedido y, si el pedido aún no tiene contrato, lo genera
     * ya con esos valores como términos finales.
     *
     * @param idPedido    id del pedido
     * @param idPropuesta id de la propuesta pendiente a aceptar
     * @param idUsuario   id del usuario que acepta (debe ser la contraparte del proponente)
     * @return el pedido con los términos ya aplicados
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido o la propuesta no existen
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el usuario es el propio proponente, no es parte del pedido, el contrato ya tiene alguna firma o la propuesta ya fue resuelta
     */
    OrderResponse acceptTermsProposal(Long idPedido, Long idPropuesta, Long idUsuario);

    /**
     * Solo la contraparte del proponente puede rechazar. No modifica el pedido.
     *
     * @param idPedido    id del pedido
     * @param idPropuesta id de la propuesta pendiente a rechazar
     * @param idUsuario   id del usuario que rechaza (debe ser la contraparte del proponente)
     * @return la propuesta ya marcada como rechazada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido o la propuesta no existen
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el usuario es el propio proponente, no es parte del pedido o la propuesta ya fue resuelta
     */
    TermsProposalResponse rejectTermsProposal(Long idPedido, Long idPropuesta, Long idUsuario);

    /**
     * Solo el propio proponente puede cancelar su propuesta pendiente.
     *
     * @param idPedido    id del pedido
     * @param idPropuesta id de la propuesta pendiente a cancelar
     * @param idUsuario   id del usuario que cancela (debe ser quien la propuso)
     * @return la propuesta ya marcada como cancelada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si la propuesta no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el usuario no es quien propuso los términos o la propuesta ya fue resuelta
     */
    TermsProposalResponse cancelTermsProposal(Long idPedido, Long idPropuesta, Long idUsuario);

    /**
     * Lanza ResourceNotFoundException si no hay ninguna propuesta pendiente.
     *
     * @param idPedido             id del pedido
     * @param idUsuarioSolicitante id del usuario que consulta (debe ser parte del pedido o admin)
     * @return la propuesta de términos actualmente pendiente
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido no existe o no hay ninguna propuesta pendiente
     */
    TermsProposalResponse getPendingProposal(Long idPedido, Long idUsuarioSolicitante);

    /**
     * Obtiene el historial completo de transiciones de etapa de un pedido, en orden cronológico.
     *
     * @param idPedido             id del pedido
     * @param idUsuarioSolicitante id del usuario que consulta (debe ser parte del pedido o admin)
     * @return el historial de estados del pedido
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido no existe
     */
    List<StatusHistoryResponse> getHistory(Long idPedido, Long idUsuarioSolicitante);

    /**
     * Obtiene una vista de seguimiento del pedido: etapa actual, progreso
     * porcentual sobre el flujo configurado y si está bloqueado por falta de entregable.
     *
     * @param idPedido             id del pedido
     * @param idUsuarioSolicitante id del usuario que consulta (debe ser parte del pedido o admin)
     * @return el resumen de seguimiento del pedido
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido no existe
     */
    OrderTrackingResponse getTracking(Long idPedido, Long idUsuarioSolicitante);
}
