package uteq.edu.ec.artisync.controller.pedido;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.pedido.AdvanceStageRequest;
import uteq.edu.ec.artisync.dto.peticion.pedido.CreateOrderRequest;
import uteq.edu.ec.artisync.dto.peticion.pedido.CreateTermsProposalRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.respuesta.pedido.StatusHistoryResponse;
import uteq.edu.ec.artisync.dto.respuesta.pedido.OrderResponse;
import uteq.edu.ec.artisync.dto.respuesta.pedido.OrderSummaryResponse;
import uteq.edu.ec.artisync.dto.respuesta.pedido.TermsProposalResponse;
import uteq.edu.ec.artisync.dto.respuesta.pedido.OrderTrackingResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.pedido.IOrderService;
import uteq.edu.ec.artisync.service.shared.reporte.GeneratedDocument;
import uteq.edu.ec.artisync.service.shared.reporte.ReportFormat;
import uteq.edu.ec.artisync.util.DocumentResponse;

import java.util.List;

/** Ciclo de vida de los pedidos: creación, avance de etapas, propuestas de términos y reportes. */
@RestController
@RequestMapping("/api/v1/pedidos")
@RequiredArgsConstructor
public class OrderController {

    private final IOrderService pedidoServicio;

    /**
     * Crea un nuevo pedido para el servicio solicitado por el usuario autenticado.
     * @param userDetails usuario autenticado que actúa como cliente que solicita el pedido
     * @param peticion datos del pedido a crear (servicio contratado, términos iniciales)
     * @return el pedido recién creado, con estado 201 (Created)
     */
    @PostMapping
    @PreAuthorize("hasAuthority('PEDIDO_CREAR') or hasAuthority('PEDIDO_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> createOrder(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateOrderRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pedidoServicio.createOrder(userDetails.getIdUsuario(), peticion));
    }

    /**
     * Obtiene el detalle de un pedido específico, validando que el usuario autenticado
     * tenga relación con él (como cliente o como creador).
     * @param id identificador del pedido a consultar
     * @param userDetails usuario autenticado que solicita el detalle
     * @return el pedido con su detalle completo
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> getOrder(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(pedidoServicio.getOrderById(id, userDetails.getIdUsuario()));
    }

    /**
     * Lista los pedidos realizados por el usuario autenticado en su rol de cliente.
     * @param userDetails usuario autenticado dueño de los pedidos a listar
     * @return listado resumido de los pedidos del usuario
     */
    @GetMapping("/mis-pedidos")
    @PreAuthorize("hasAuthority('PEDIDO_CREAR') or hasAuthority('PEDIDO_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<List<OrderSummaryResponse>> listMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(pedidoServicio.listMyOrders(userDetails.getIdUsuario()));
    }

    /**
     * Lista los pedidos que el usuario autenticado gestiona como creador (comisiones recibidas).
     * @param userDetails usuario autenticado que actúa como creador del servicio contratado
     * @return listado resumido de los pedidos que el creador debe gestionar
     */
    @GetMapping("/mis-comisiones")
    @PreAuthorize("hasAuthority('PEDIDO_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<List<OrderSummaryResponse>> listMyCommissions(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(pedidoServicio.listMyCommissions(userDetails.getIdUsuario()));
    }

    /**
     * Exportación "propia": mismo @PreAuthorize que el listado, sin permiso
     * de exportación aparte — a diferencia de auditoría/finanzas/contratos,
     * que son reportes administrativos y sí lo llevan.
     */
    /**
     * Exportación "propia": mismo @PreAuthorize que el listado, sin permiso
     * de exportación aparte — a diferencia de auditoría/finanzas/contratos,
     * que son reportes administrativos y sí lo llevan.
     */
    /**
     * Exporta a un archivo descargable los pedidos del usuario autenticado como cliente.
     * @param userDetails usuario autenticado dueño de los pedidos a exportar
     * @param formato formato del documento de salida (por ejemplo PDF o Excel)
     * @param authentication identidad autenticada usada para trazar quién generó el reporte
     * @return el documento generado como arreglo de bytes descargable
     */
    @GetMapping("/mis-pedidos/exportar")
    @PreAuthorize("hasAuthority('PEDIDO_CREAR') or hasAuthority('PEDIDO_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportMyOrders(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam ReportFormat formato,
            Authentication authentication) {
        GeneratedDocument documento = pedidoServicio.exportMyOrders(
                userDetails.getIdUsuario(), formato, authentication.getName());
        return DocumentResponse.de(documento);
    }

    /**
     * Exporta a un archivo descargable las comisiones (pedidos gestionados como creador)
     * del usuario autenticado, opcionalmente filtradas por un subconjunto de pedidos.
     * @param userDetails usuario autenticado que actúa como creador de los servicios contratados
     * @param formato formato del documento de salida (por ejemplo PDF o Excel)
     * @param idsPedido identificadores de los pedidos a incluir; si se omite, se exportan todos
     * @param authentication identidad autenticada usada para trazar quién generó el reporte
     * @return el documento generado como arreglo de bytes descargable
     */
    @GetMapping("/mis-comisiones/exportar")
    @PreAuthorize("hasAuthority('PEDIDO_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportMyCommissions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam ReportFormat formato,
            @RequestParam(required = false) List<Long> idsPedido,
            Authentication authentication) {
        GeneratedDocument documento = pedidoServicio.exportMyCommissions(
                userDetails.getIdUsuario(), idsPedido, formato, authentication.getName());
        return DocumentResponse.de(documento);
    }

    /**
     * Avanza el pedido a la siguiente etapa de su flujo de trabajo.
     * @param id identificador del pedido a avanzar
     * @param userDetails usuario autenticado que ejecuta el avance (normalmente el creador)
     * @param peticion datos de la etapa destino y comentarios asociados al avance
     * @return el pedido actualizado con su nueva etapa
     */
    @PutMapping("/{id}/avanzar")
    @PreAuthorize("hasAuthority('PEDIDO_GESTIONAR') or hasRole('ADMIN')")
    public ResponseEntity<OrderResponse> advanceStage(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody AdvanceStageRequest peticion) {
        return ResponseEntity.ok(pedidoServicio.advanceStage(id, userDetails.getIdUsuario(), peticion));
    }

    /**
     * Crea una propuesta de cambio de términos (alcance, precio o plazo) sobre un pedido existente.
     * @param id identificador del pedido sobre el que se propone el cambio de términos
     * @param userDetails usuario autenticado que origina la propuesta
     * @param peticion datos de los nuevos términos propuestos
     * @return la propuesta de términos creada, con estado 201 (Created)
     */
    @PostMapping("/{id}/propuestas-terminos")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TermsProposalResponse> proposeTerms(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateTermsProposalRequest peticion) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pedidoServicio.proposeTerms(id, userDetails.getIdUsuario(), peticion));
    }

    /**
     * Obtiene la propuesta de términos pendiente de respuesta para un pedido, si existe.
     * @param id identificador del pedido a consultar
     * @param userDetails usuario autenticado que consulta la propuesta
     * @return la propuesta de términos pendiente
     */
    @GetMapping("/{id}/propuestas-terminos/pendiente")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TermsProposalResponse> getPendingProposal(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(pedidoServicio.getPendingProposal(id, userDetails.getIdUsuario()));
    }

    /**
     * Acepta una propuesta de cambio de términos, aplicando los nuevos términos al pedido.
     * @param id identificador del pedido asociado a la propuesta
     * @param idPropuesta identificador de la propuesta de términos a aceptar
     * @param userDetails usuario autenticado que acepta la propuesta
     * @return el pedido actualizado con los términos ya aplicados
     */
    @PutMapping("/{id}/propuestas-terminos/{idPropuesta}/aceptar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderResponse> acceptTermsProposal(
            @PathVariable Long id,
            @PathVariable Long idPropuesta,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(pedidoServicio.acceptTermsProposal(id, idPropuesta, userDetails.getIdUsuario()));
    }

    /**
     * Rechaza una propuesta de cambio de términos, dejando vigentes los términos actuales del pedido.
     * @param id identificador del pedido asociado a la propuesta
     * @param idPropuesta identificador de la propuesta de términos a rechazar
     * @param userDetails usuario autenticado que rechaza la propuesta
     * @return la propuesta de términos actualizada con su nuevo estado
     */
    @PutMapping("/{id}/propuestas-terminos/{idPropuesta}/rechazar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TermsProposalResponse> rejectTermsProposal(
            @PathVariable Long id,
            @PathVariable Long idPropuesta,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(pedidoServicio.rejectTermsProposal(id, idPropuesta, userDetails.getIdUsuario()));
    }

    /**
     * Cancela una propuesta de cambio de términos antes de que sea respondida.
     * @param id identificador del pedido asociado a la propuesta
     * @param idPropuesta identificador de la propuesta de términos a cancelar
     * @param userDetails usuario autenticado que cancela la propuesta (normalmente quien la creó)
     * @return la propuesta de términos actualizada con su nuevo estado
     */
    @PutMapping("/{id}/propuestas-terminos/{idPropuesta}/cancelar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<TermsProposalResponse> cancelTermsProposal(
            @PathVariable Long id,
            @PathVariable Long idPropuesta,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(pedidoServicio.cancelTermsProposal(id, idPropuesta, userDetails.getIdUsuario()));
    }

    /**
     * Obtiene el historial cronológico de cambios de estado de un pedido.
     * @param id identificador del pedido a consultar
     * @param userDetails usuario autenticado que consulta el historial
     * @return el listado de eventos de historial del pedido, en orden cronológico
     */
    @GetMapping("/{id}/historial")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<StatusHistoryResponse>> getHistory(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(pedidoServicio.getHistory(id, userDetails.getIdUsuario()));
    }

    /**
     * Obtiene el estado de seguimiento consolidado de un pedido (etapa actual, avance y próximos hitos).
     * @param id identificador del pedido a consultar
     * @param userDetails usuario autenticado que consulta el seguimiento
     * @return el resumen de seguimiento del pedido
     */
    @GetMapping("/{id}/seguimiento")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<OrderTrackingResponse> getTracking(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(pedidoServicio.getTracking(id, userDetails.getIdUsuario()));
    }

    // ── Inmutabilidad del Historial (RNF-13) ─────────────────────────────────
    // Los registros de historial_estados_pedido NO pueden ser eliminados ni modificados

    /**
     * Bloquea explícitamente el borrado de registros de historial de un pedido, ya que son
     * inmutables por requisito de auditoría (RNF-13).
     * @param id identificador del pedido cuyo historial se intentó eliminar
     * @return respuesta con estado 403 (Forbidden) y un mensaje explicativo
     */
    @DeleteMapping("/{id}/historial")
    public ResponseEntity<RespuestaMensaje> blockDeleteHistory(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new RespuestaMensaje("Operacion no permitida sobre registros de auditoria"));
    }

    /**
     * Bloquea explícitamente la modificación parcial de registros de historial de un pedido,
     * ya que son inmutables por requisito de auditoría (RNF-13).
     * @param id identificador del pedido cuyo historial se intentó modificar
     * @return respuesta con estado 403 (Forbidden) y un mensaje explicativo
     */
    @PatchMapping("/{id}/historial")
    public ResponseEntity<RespuestaMensaje> blockPatchHistory(@PathVariable Long id) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new RespuestaMensaje("Operacion no permitida sobre registros de auditoria"));
    }
}
