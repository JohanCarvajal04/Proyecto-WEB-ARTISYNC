package uteq.edu.ec.artisync.service.legal.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uteq.edu.ec.artisync.dto.peticion.legal.EscrowPaymentFilter;
import uteq.edu.ec.artisync.dto.respuesta.legal.EscrowPaymentResponse;
import uteq.edu.ec.artisync.dto.respuesta.legal.EscrowPaymentDetailResponse;
import uteq.edu.ec.artisync.dto.respuesta.legal.EscrowSummaryResponse;
import uteq.edu.ec.artisync.dto.respuesta.legal.PaymentTransactionResponse;
import uteq.edu.ec.artisync.entity.legal.EscrowPayment;
import uteq.edu.ec.artisync.entity.pedido.Order;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.repository.legal.EscrowPaymentRepository;
import uteq.edu.ec.artisync.repository.legal.PaymentTransactionRepository;
import uteq.edu.ec.artisync.service.legal.IEscrowPaymentAuditService;
import uteq.edu.ec.artisync.specification.legal.EscrowPaymentSpecification;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EscrowPaymentAuditServiceImpl implements IEscrowPaymentAuditService {

    private final EscrowPaymentRepository pagoGarantiaRepository;
    private final PaymentTransactionRepository transaccionPagoRepository;

    @Override
    @Transactional(readOnly = true)
    /**
     * Obtiene y estructura un listado completo o filtrado de los registros pertinentes del sistema.
     *
     * @param filtro criterios de busqueda y filtrado dinamico a aplicar
     * @param pageable configuracion de paginacion y ordenamiento para la capa de datos
     * @return una estructura de datos paginada con la porcion de resultados solicitada y metadatos de pagina
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public Page<EscrowPaymentResponse> list(EscrowPaymentFilter filtro, Pageable pageable) {
        var spec = EscrowPaymentSpecification.conFiltros(
                filtro.getEstadoFondos(), filtro.getIdPerfilCreador(), filtro.getIdUsuarioCliente(),
                filtro.getDesde(), filtro.getHasta());

        return pagoGarantiaRepository.findAll(spec, pageable).map(this::mapearAResumen);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @param idPago identificador unico que referencia de manera univoca al registro
     * @return un objeto especializado con el resultado estructurado de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public EscrowPaymentDetailResponse getDetail(Long idPago) {
        EscrowPayment pago = pagoGarantiaRepository.findById(idPago)
                .orElseThrow(() -> new ResourceNotFoundException("Pago de garantía no encontrado: " + idPago));

        Order pedido = pago.getContrato().getPedido();
        User cliente = pedido.getUsuarioCliente();
        User creador = pedido.getServicio().getPerfil().getUsuario();

        List<PaymentTransactionResponse> transacciones = transaccionPagoRepository
                .findByPagoIdPagoOrderByFechaEjecucionDesc(idPago).stream()
                .map(t -> PaymentTransactionResponse.builder()
                        .idTransaccion(t.getIdTransaccion())
                        .tipoTransaccion(t.getTipoTransaccion())
                        .monto(t.getMonto())
                        .fechaEjecucion(t.getFechaEjecucion())
                        .build())
                .toList();

        return EscrowPaymentDetailResponse.builder()
                .idPago(pago.getIdPago())
                .idContrato(pago.getContrato().getIdContrato())
                .idPedido(pedido.getIdPedido())
                .tituloServicio(pedido.getServicio().getTituloServicio())
                .idUsuarioCliente(cliente.getIdUsuario())
                .nombreCliente(cliente.getNombres() + " " + cliente.getApellidos())
                .correoCliente(cliente.getCorreo())
                .idPerfilCreador(pedido.getServicio().getPerfil().getIdPerfil())
                .nombreCreador(creador.getNombres() + " " + creador.getApellidos())
                .idOrdenPaypal(pago.getIdOrdenPaypal())
                .montoRetenido(pago.getMontoRetenido())
                .estadoFondos(pago.getEstadoFondos())
                .fechaFormalizacion(pago.getContrato().getFechaFormalizacion())
                .transacciones(transacciones)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Recupera la informacion detallada y estructurada correspondiente a los criterios de busqueda provistos.
     *
     * @return una coleccion indexada con todos los elementos resultantes de la operacion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public List<EscrowSummaryResponse> getSummary() {
        return pagoGarantiaRepository.resumenPorEstado();
    }

    private EscrowPaymentResponse mapearAResumen(EscrowPayment pago) {
        Order pedido = pago.getContrato().getPedido();
        User cliente = pedido.getUsuarioCliente();
        User creador = pedido.getServicio().getPerfil().getUsuario();

        return EscrowPaymentResponse.builder()
                .idPago(pago.getIdPago())
                .idContrato(pago.getContrato().getIdContrato())
                .idPedido(pedido.getIdPedido())
                .tituloServicio(pedido.getServicio().getTituloServicio())
                .idUsuarioCliente(cliente.getIdUsuario())
                .nombreCliente(cliente.getNombres() + " " + cliente.getApellidos())
                .idPerfilCreador(pedido.getServicio().getPerfil().getIdPerfil())
                .nombreCreador(creador.getNombres() + " " + creador.getApellidos())
                .idOrdenPaypal(pago.getIdOrdenPaypal())
                .montoRetenido(pago.getMontoRetenido())
                .estadoFondos(pago.getEstadoFondos())
                .fechaFormalizacion(pago.getContrato().getFechaFormalizacion())
                .build();
    }
}
