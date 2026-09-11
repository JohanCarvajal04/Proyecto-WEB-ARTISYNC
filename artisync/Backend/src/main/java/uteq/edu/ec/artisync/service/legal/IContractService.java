package uteq.edu.ec.artisync.service.legal;

import uteq.edu.ec.artisync.dto.respuesta.legal.ContractResponse;
import uteq.edu.ec.artisync.dto.respuesta.legal.SignatureStatusResponse;
import uteq.edu.ec.artisync.dto.respuesta.legal.IntegrityVerificationResponse;

public interface IContractService {

    /**
     * Genera el contrato de un pedido a partir de la plantilla legal asignada al servicio.
     *
     * @param idPedido             id del pedido a contratar
     * @param idUsuarioSolicitante id del usuario que solicita la generación
     * @return el contrato recién generado, sin firmas
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si ya existe un contrato para este pedido
     */
    ContractResponse generarContrato(Long idPedido, Long idUsuarioSolicitante);

    /**
     * Registra la firma del cliente o del creador sobre un contrato.
     *
     * @param idContrato id del contrato a firmar
     * @param idUsuario  id del usuario que firma, debe ser el cliente o el creador del pedido
     * @return el contrato con la firma ya registrada
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el contrato no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si esa parte ya había firmado el contrato
     * @throws org.springframework.security.access.AccessDeniedException si el solicitante no es parte del contrato
     */
    ContractResponse firmarContrato(Long idContrato, Long idUsuario);

    /**
     * Obtiene el detalle de un contrato por su id.
     *
     * @param idContrato           id del contrato
     * @param idUsuarioSolicitante id del usuario que consulta
     * @return el detalle del contrato
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el contrato no existe
     */
    ContractResponse obtenerContrato(Long idContrato, Long idUsuarioSolicitante);

    /**
     * Obtiene el contrato asociado a un pedido.
     *
     * @param idPedido             id del pedido
     * @param idUsuarioSolicitante id del usuario que consulta
     * @return el contrato del pedido
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido no tiene contrato
     */
    ContractResponse obtenerContratoPorPedido(Long idPedido, Long idUsuarioSolicitante);

    /**
     * Obtiene el estado de firma de un contrato (quién ha firmado y quién falta).
     *
     * @param idContrato           id del contrato
     * @param idUsuarioSolicitante id del usuario que consulta
     * @return el estado de firma del contrato
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el contrato no existe
     */
    SignatureStatusResponse obtenerEstadoFirma(Long idContrato, Long idUsuarioSolicitante);

    /**
     * Genera el PDF del contrato, con su hash de integridad si ya está firmado por ambas partes.
     *
     * @param idContrato           id del contrato
     * @param idUsuarioSolicitante id del usuario que solicita el PDF
     * @return los bytes del PDF generado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el contrato no existe
     */
    byte[] generarPdf(Long idContrato, Long idUsuarioSolicitante);

    /**
     * Recalcula el hash SHA-256 del contenido congelado de un contrato y lo
     * compara con el guardado al momento de la firma (REQ-NF-020).
     *
     * @param idContrato id del contrato a verificar
     * @return el resultado de la comparación
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el contrato no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el contrato aún no está firmado por ambas partes
     */
    IntegrityVerificationResponse verificarIntegridadHash(Long idContrato);
}
