package uteq.edu.ec.artisync.service.legal.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.service.shared.almacenamiento.DocumentStorage;
import uteq.edu.ec.artisync.service.shared.almacenamiento.FileExtensions;
import uteq.edu.ec.artisync.service.shared.almacenamiento.FilePolicy;
import uteq.edu.ec.artisync.service.shared.almacenamiento.StoragePrefix;
import uteq.edu.ec.artisync.dto.respuesta.legal.DeliverableResponse;
import uteq.edu.ec.artisync.entity.legal.FinalDeliverable;
import uteq.edu.ec.artisync.entity.legal.EscrowPayment;
import uteq.edu.ec.artisync.entity.legal.PaymentTransaction;
import uteq.edu.ec.artisync.entity.pedido.Order;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.legal.*;
import uteq.edu.ec.artisync.repository.pedido.OrderRepository;
import uteq.edu.ec.artisync.service.comunicacion.ChatService;
import uteq.edu.ec.artisync.service.comunicacion.NotificationService;
import uteq.edu.ec.artisync.service.legal.IDeliverableService;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeliverableServiceImpl implements IDeliverableService {

    private final FinalDeliverableRepository entregableRepository;
    private final OrderRepository pedidoRepository;
    private final EscrowPaymentRepository pagoGarantiaRepository;
    private final ContractRepository contratoRepository;
    private final PaymentTransactionRepository transaccionPagoRepository;
    private final DocumentStorage almacenamiento;
    private final ChatService chatService;
    private final NotificationService notificacionService;

    /**
     * Fuente unica con FinancialReportServiceImpl: antes era un literal
     * 0.10 aqui y, por separado, el default de fn_reporte_comisiones_creador
     * en SQL -- dos constantes que podian divergir si se cambiaba una sin la
     * otra.
     */
    @Value("${plataforma.comision-tasa:0.10}")
    private BigDecimal tasaComision;

    /**
     * Procesa y persiste la creacion de un nuevo recurso en el contexto de negocio aplicable.
     * @param idPedido identificador del pedido
     * @param idCreador identificador del creador que sube el entregable
     * @param versionMarcaAgua versión con marca de agua, visible antes de la aprobación
     * @param versionLimpia versión sin marca de agua, liberada al cliente tras la aprobación
     * @return el entregable creado, con su identificador y estado inicial
     */
    @Override
    @Transactional
    public DeliverableResponse uploadDeliverable(Long idPedido, Long idCreador,
                                                MultipartFile versionMarcaAgua, MultipartFile versionLimpia) {
        FilePolicy.ENTREGABLE.validar(versionMarcaAgua);
        FilePolicy.ENTREGABLE.validar(versionLimpia);

        Order pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Order no encontrado"));

        // Verificar que el usuario es el creador del servicio
        Long idCreadorServicio = pedido.getServicio().getPerfil().getUsuario().getIdUsuario();
        if (!idCreadorServicio.equals(idCreador)) {
            throw new BusinessRuleException("Solo el creador del servicio puede subir entregables");
        }

        FinalDeliverable entregable = entregableRepository.findByPedidoIdPedido(idPedido)
                .orElse(FinalDeliverable.builder()
                        .pedido(pedido)
                        .estaLiberado(false)
                        .build());

        // Resubir reemplaza: las referencias anteriores quedarían sin nadie que
        // las apunte, y en Azure se seguirían facturando.
        String anteriorMarcaAgua = entregable.getUrlVersionMarcaAgua();
        String anteriorLimpia = entregable.getUrlVersionLimpia();

        entregable.setUrlVersionMarcaAgua(
                almacenamiento.guardar(versionMarcaAgua, StoragePrefix.ENTREGABLES));
        entregable.setUrlVersionLimpia(
                almacenamiento.guardar(versionLimpia, StoragePrefix.ENTREGABLES));

        entregable = entregableRepository.save(entregable);
        deleteIfExists(anteriorMarcaAgua);
        deleteIfExists(anteriorLimpia);

        log.info("Entregable subido para pedido {} por creador {}", idPedido, idCreador);

        return mapToRespuesta(entregable, false);
    }

    /**
     * El archivo viejo ya no se referencia; que no se pueda borrar no debe
     * tumbar una subida que por lo demás salió bien.
     */
    private void deleteIfExists(String referencia) {
        if (referencia == null || referencia.isBlank()) {
            return;
        }
        try {
            almacenamiento.eliminar(referencia);
        } catch (RuntimeException e) {
            log.warn("No se pudo eliminar el entregable reemplazado {}: {}", referencia, e.getMessage());
        }
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
    public DeliverableResponse getDeliverable(Long idPedido, Long idUsuario) {
        FinalDeliverable entregable = entregableRepository.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("No hay entregable para este pedido"));

        // El creador siempre ve ambas versiones; el cliente solo la marca de agua hasta aprobación
        Order pedido = entregable.getPedido();
        Long idCreadorServicio = pedido.getServicio().getPerfil().getUsuario().getIdUsuario();
        boolean esCreador = idCreadorServicio.equals(idUsuario);

        return mapToRespuesta(entregable, esCreador || entregable.getEstaLiberado());
    }

    @Override
    @Transactional
    // El evento financiero central del sistema: aquí se liberan los fondos en
    // garantía. Es exactamente el tipo de operación que REQ-NF-013 exige
    // poder auditar, incluidos los intentos fallidos (cliente equivocado,
    // entregable ya liberado).
    @Auditable(accion = "FONDOS_LIBERAR", modulo = AuditModule.FINANZAS,
            entidad = "pedidos", idEntidad = "#idPedido")
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @param idPedido identificador unico que referencia de manera univoca al registro
     * @param idCliente identificador unico que referencia de manera univoca al registro
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public void approveDelivery(Long idPedido, Long idCliente) {
        Order pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Order no encontrado"));

        // Verificar que el usuario es el cliente
        if (!pedido.getUsuarioCliente().getIdUsuario().equals(idCliente)) {
            throw new BusinessRuleException("Solo el cliente puede approve la entrega");
        }

        FinalDeliverable entregable = entregableRepository.findByPedidoIdPedidoParaActualizar(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("No hay entregable para este pedido"));

        if (entregable.getEstaLiberado()) {
            throw new BusinessRuleException("El entregable ya fue aprobado");
        }

        // Liberar fondos
        var contrato = contratoRepository.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("No existe contrato para el pedido"));

        var pagoOpt = pagoGarantiaRepository.findByContratoIdContrato(contrato.getIdContrato());
        if (pagoOpt.isPresent()) {
            EscrowPayment pago = pagoOpt.get();

            // REQ-NF-019: sin este guard, un pedido ya cancelado-y-reembolsado
            // (o liberado) por cancelOrderWithHeldFunds podía aprobarse
            // aquí después y pagar al creador una segunda vez sobre el mismo
            // dinero, o liberar fondos que ya se le devolvieron al cliente.
            if (!"Retenido".equalsIgnoreCase(pago.getEstadoFondos())) {
                throw new BusinessRuleException(
                        "No se puede approve: el pago de este pedido no está en estado Retenido (estado actual: "
                                + pago.getEstadoFondos() + ")");
            }

            pago.setEstadoFondos("Liberado");
            pagoGarantiaRepository.save(pago);

            // Registrar transacciones: egreso al creador y comisión plataforma
            // (tasaComision, misma fuente que usa FinancialReportServiceImpl)
            BigDecimal comision = pago.getMontoRetenido().multiply(tasaComision);
            BigDecimal pagoCreador = pago.getMontoRetenido().subtract(comision);

            transaccionPagoRepository.save(PaymentTransaction.builder()
                    .pago(pago).tipoTransaccion("Egreso").monto(pagoCreador).build());
            transaccionPagoRepository.save(PaymentTransaction.builder()
                    .pago(pago).tipoTransaccion("Comision").monto(comision).build());

            log.info("Fondos liberados para pedido {}: creador=${}, comision=${}", idPedido, pagoCreador, comision);
        }

        // Habilitar descarga
        entregable.setEstaLiberado(true);
        entregableRepository.save(entregable);

        log.info("Entrega aprobada para pedido {} por cliente {}", idPedido, idCliente);

        chatService.closeRoom(idPedido);
        notificacionService.notify(pedido.getServicio().getPerfil().getUsuario(), "PAGO_LIBERADO",
                "El cliente aprobó la entrega de \"" + pedido.getServicio().getTituloServicio()
                        + "\" y el pago fue liberado a tu favor.");
    }

    @Override
    @Transactional(readOnly = true)
    @Auditable(accion = "ENTREGABLE_DESCARGAR", modulo = AuditModule.FINANZAS,
            entidad = "pedidos", idEntidad = "#idPedido")
    /**
     * Prepara y ensambla un documento o archivo fisico de salida con los datos requeridos.
     *
     * @param idPedido identificador unico que referencia de manera univoca al registro
     * @param idCliente identificador unico que referencia de manera univoca al registro
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public ArchivoDescargado downloadCleanVersion(Long idPedido, Long idCliente) {
        Order pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Order no encontrado"));

        if (!pedido.getUsuarioCliente().getIdUsuario().equals(idCliente)) {
            throw new BusinessRuleException("Solo el cliente puede descargar el entregable");
        }

        FinalDeliverable entregable = entregableRepository.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("No hay entregable para este pedido"));

        if (!entregable.getEstaLiberado()) {
            throw new BusinessRuleException("El entregable no esta disponible hasta que el pago sea liberado");
        }

        String referencia = entregable.getUrlVersionLimpia();
        if (referencia == null || referencia.isBlank()) {
            throw new ResourceNotFoundException("El entregable no tiene un archivo asociado");
        }

        log.info("Descarga de version limpia para pedido {}", idPedido);
        return new ArchivoDescargado(
                almacenamiento.leer(referencia),
                "entregable-pedido-" + idPedido + extensionDe(referencia),
                FileExtensions.contentTypeDe(referencia));
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Prepara y ensambla un documento o archivo fisico de salida con los datos requeridos.
     *
     * @param idPedido identificador unico que referencia de manera univoca al registro
     * @param idUsuario identificador unico que referencia de manera univoca al registro
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public ArchivoDescargado downloadWatermarkedVersion(Long idPedido, Long idUsuario) {
        FinalDeliverable entregable = entregableRepository.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("No hay entregable para este pedido"));

        // La ve el cliente que la debe revisar y el creador que la subió; nadie más.
        Order pedido = entregable.getPedido();
        boolean esCliente = pedido.getUsuarioCliente().getIdUsuario().equals(idUsuario);
        boolean esCreador = pedido.getServicio().getPerfil().getUsuario().getIdUsuario().equals(idUsuario);
        if (!esCliente && !esCreador) {
            throw new BusinessRuleException("No tiene acceso a este entregable");
        }

        String referencia = entregable.getUrlVersionMarcaAgua();
        if (referencia == null || referencia.isBlank()) {
            throw new ResourceNotFoundException("El entregable no tiene version con marca de agua");
        }

        return new ArchivoDescargado(
                almacenamiento.leer(referencia),
                "vista-previa-pedido-" + idPedido + extensionDe(referencia),
                FileExtensions.contentTypeDe(referencia));
    }

    private String extensionDe(String referencia) {
        int punto = referencia.lastIndexOf('.');
        return punto < 0 ? "" : referencia.substring(punto);
    }

    // ── Métodos auxiliares ───────────────────────────────────────────────────

    private DeliverableResponse mapToRespuesta(FinalDeliverable entregable, boolean mostrarVersionLimpia) {
        Long idPedido = entregable.getPedido().getIdPedido();
        return DeliverableResponse.builder()
                .idEntregable(entregable.getIdEntregable())
                .idPedido(idPedido)
                .urlVersionMarcaAgua(downloadUrl(
                        entregable.getUrlVersionMarcaAgua(),
                        "/api/v1/pedidos/" + idPedido + "/entregable/descargar/marca-agua"))
                .urlVersionLimpia(mostrarVersionLimpia
                        ? downloadUrl(entregable.getUrlVersionLimpia(),
                                "/api/v1/pedidos/" + idPedido + "/entregable/descargar")
                        : null)
                .estaLiberado(entregable.getEstaLiberado())
                .build();
    }

    /**
     * Lo que se persiste es una referencia interna, no algo que el navegador
     * pueda pedir. Con Azure se entrega un SAS firmado y el archivo viaja
     * directo desde el blob; sin él, la ruta del endpoint que sirve los bytes.
     */
    private String downloadUrl(String referencia, String rutaProxy) {
        if (referencia == null || referencia.isBlank()) {
            return null;
        }
        return almacenamiento.urlTemporal(referencia).orElse(rutaProxy);
    }
}
