package uteq.edu.ec.artisync.controller.legal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.respuesta.legal.ContractResponse;
import uteq.edu.ec.artisync.dto.respuesta.legal.SignatureStatusResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.legal.IContractService;

/** Consulta y firma de contratos de pedido. */
@RestController
@RequestMapping("/api/v1/contratos")
@RequiredArgsConstructor
public class ContractController {

    private final IContractService contratoServicio;

    /**
     * Genera el contrato formal de un pedido a partir de la plantilla elegida por el creador.
     * @param idPedido identificador del pedido sobre el cual se formaliza el contrato
     * @param userDetails usuario autenticado que solicita la generación del contrato
     * @return el contrato recién generado, pendiente de firma
     */
    @PostMapping("/pedido/{idPedido}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContractResponse> generateContract(
            @PathVariable Long idPedido,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(contratoServicio.generateContract(idPedido, userDetails.getIdUsuario()));
    }

    /**
     * Registra la firma del usuario autenticado sobre un contrato existente.
     * @param id identificador del contrato a firmar
     * @param userDetails usuario autenticado que firma (cliente o creador según su rol en el contrato)
     * @return el contrato con el estado de firma actualizado
     */
    @PostMapping("/{id}/firmar")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContractResponse> signContract(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(contratoServicio.signContract(id, userDetails.getIdUsuario()));
    }

    /**
     * Obtiene el detalle de un contrato por su identificador, si el usuario autenticado es parte de él.
     * @param id identificador del contrato solicitado
     * @param userDetails usuario autenticado que consulta el contrato
     * @return el detalle del contrato
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContractResponse> getContract(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(contratoServicio.getContract(id, userDetails.getIdUsuario()));
    }

    /**
     * Obtiene el contrato asociado a un pedido específico.
     * @param idPedido identificador del pedido cuyo contrato se busca
     * @param userDetails usuario autenticado que consulta el contrato
     * @return el contrato vinculado al pedido
     */
    @GetMapping("/pedido/{idPedido}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ContractResponse> getContractByOrder(
            @PathVariable Long idPedido,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(contratoServicio.getContractByOrder(idPedido, userDetails.getIdUsuario()));
    }

    /**
     * Consulta el estado de firma actual del contrato para el usuario autenticado.
     * @param id identificador del contrato
     * @param userDetails usuario autenticado que realiza la consulta
     * @return estado de firma indicando si el usuario ya firmó, si es el último en firmar, etc.
     */
    @GetMapping("/{id}/estado-firma")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<SignatureStatusResponse> getSignatureStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(contratoServicio.getSignatureStatus(id, userDetails.getIdUsuario()));
    }

    /**
     * Descarga el documento PDF final y firmado del contrato.
     * @param id identificador del contrato
     * @param userDetails usuario autenticado (debe ser parte del contrato o administrador)
     * @return el archivo PDF del contrato para descarga
     */
    @GetMapping("/{id}/pdf")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<byte[]> downloadPdf(
            @PathVariable Long id,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        byte[] pdf = contratoServicio.generatePdf(id, userDetails.getIdUsuario());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "contrato_" + id + ".pdf");
        headers.setContentLength(pdf.length);

        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }
}
