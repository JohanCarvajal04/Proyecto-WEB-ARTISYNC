package uteq.edu.ec.artisync.service.legal.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.AuditModule;
import uteq.edu.ec.artisync.dto.respuesta.legal.ContractResponse;
import uteq.edu.ec.artisync.dto.respuesta.legal.SignatureStatusResponse;
import uteq.edu.ec.artisync.dto.respuesta.legal.IntegrityVerificationResponse;
import uteq.edu.ec.artisync.entity.legal.Contract;
import uteq.edu.ec.artisync.entity.pedido.Order;
import uteq.edu.ec.artisync.entity.pedido.ContractTemplate;
import uteq.edu.ec.artisync.entity.seguridad.User;
import uteq.edu.ec.artisync.exception.ResourceNotFoundException;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.legal.ContractRepository;
import uteq.edu.ec.artisync.repository.pedido.OrderRepository;
import uteq.edu.ec.artisync.repository.pedido.ContractTemplateRepository;
import uteq.edu.ec.artisync.service.legal.IContractService;
import uteq.edu.ec.artisync.service.legal.IPdfGenerationService;
import uteq.edu.ec.artisync.util.OrderOwnershipValidator;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractServiceImpl implements IContractService {

    private final ContractRepository contratoRepository;
    private final OrderRepository pedidoRepository;
    private final ContractTemplateRepository plantillaContratoRepository;
    private final IPdfGenerationService pdfGeneracionServicio;

    /**
     * REQ-NF-020: período de retención del contenido congelado del contrato.
     * El valor de 7 es un placeholder -- confirmar la cifra real bajo la
     * normativa ecuatoriana aplicable a registros contractuales/tributarios
     * antes de depender de ella en producción.
     */
    @Value("${contrato.retencion-anios:7}")
    private int retencionAnios;

    @Override
    @Transactional
    @Auditable(accion = "CONTRATO_GENERAR", modulo = AuditModule.FINANZAS,
            entidad = "contratos", idEntidad = "#resultado.idContrato")
    /**
     * Genera el contrato de un pedido, tomando la plantilla que su servicio
     * tiene asignada o, si no tiene ninguna, la marcada como predeterminada.
     *
     * @param idPedido identificador del pedido a contratar
     * @param idUsuarioSolicitante identificador de quien solicita; debe ser parte del pedido o admin
     * @return el contrato recién creado, sin firmas
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido no existe, o si
     *         el servicio no tiene plantilla asignada y tampoco existe una predeterminada
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el pedido ya tiene un contrato
     */
    public ContractResponse generateContract(Long idPedido, Long idUsuarioSolicitante) {
        Order pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("Order no encontrado"));
        // H-02: evita que cualquier autenticado genere un contrato sobre un pedido ajeno.
        OrderOwnershipValidator.validarPertenenciaOAdmin(pedido, idUsuarioSolicitante);

        // Verificar que no exista ya un contrato para este pedido
        if (contratoRepository.findByPedidoIdPedido(idPedido).isPresent()) {
            throw new BusinessRuleException("Ya existe un contrato para este pedido");
        }

        // REQ-F-017 ampliado: la plantilla ya no es única y global. Se usa la
        // que el creador asignó a su servicio (catálogo curado por ADMIN,
        // ver ContractTemplateAdminController); si no asignó ninguna, se
        // cae a la marcada como predeterminada, para no bloquear el contrato.
        ContractTemplate plantilla = pedido.getServicio().getPlantillaContrato();
        if (plantilla == null) {
            plantilla = plantillaContratoRepository.findByEsPredeterminadaTrue()
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "No hay una plantilla de contrato predeterminada configurada en el sistema"));
        }

        // Crear contrato
        Contract contrato = Contract.builder()
                .pedido(pedido)
                .plantilla(plantilla)
                .limiteRevisiones(pedido.getServicio().getLimiteRevisionesBase() != null
                        ? pedido.getServicio().getLimiteRevisionesBase() : 0)
                .build();

        contrato = contratoRepository.save(contrato);
        log.info("Contract {} generado para pedido {}", contrato.getIdContrato(), idPedido);

        return mapToRespuesta(contrato);
    }

    @Override
    @Transactional
    @Auditable(accion = "CONTRATO_FIRMAR", modulo = AuditModule.FINANZAS,
            entidad = "contratos", idEntidad = "#idContrato")
    /**
     * Registra la firma del creador o del cliente sobre un contrato. Cuando
     * ambas firmas quedan registradas, el contenido del contrato se congela
     * de forma definitiva (ver {@link #congelarContenidoYHash}).
     *
     * @param idContrato identificador del contrato a firmar
     * @param idUsuario identificador de quien firma; debe ser el creador o el cliente del pedido
     * @return el contrato con el estado de firma actualizado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el contrato no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si esa parte ya había firmado
     * @throws org.springframework.security.access.AccessDeniedException si quien firma no es
     *         ni el creador ni el cliente del pedido
     */
    public ContractResponse signContract(Long idContrato, Long idUsuario) {
        Contract contrato = contratoRepository.findByIdParaFirmar(idContrato)
                .orElseThrow(() -> new ResourceNotFoundException("Contract no encontrado"));

        Order pedido = contrato.getPedido();
        String hash = generateSignatureHash(idContrato, idUsuario);

        Long idCreador = pedido.getServicio().getPerfil().getUsuario().getIdUsuario();
        Long idCliente = pedido.getUsuarioCliente().getIdUsuario();

        if (idUsuario.equals(idCreador)) {
            if (contrato.getHashFirmaCreador() != null) {
                throw new BusinessRuleException("El creador ya firmo este contrato");
            }
            contrato.setHashFirmaCreador(hash);
            log.info("Contract {} firmado por creador (usuario {})", idContrato, idUsuario);
        } else if (idUsuario.equals(idCliente)) {
            if (contrato.getHashFirmaCliente() != null) {
                throw new BusinessRuleException("El cliente ya firmo este contrato");
            }
            contrato.setHashFirmaCliente(hash);
            log.info("Contract {} firmado por cliente (usuario {})", idContrato, idUsuario);
        } else {
            // H-02: 403, no 422 — coherente con el resto del proyecto (GlobalExceptionHandler).
            throw new AccessDeniedException("No eres parte de este contrato");
        }

        // REQ-NF-020: recién ahora, con la segunda firma, el contenido del
        // contrato queda definitivo -- se congela una única vez.
        if (contrato.getHashFirmaCreador() != null && contrato.getHashFirmaCliente() != null) {
            congelarContenidoYHash(contrato);
        }

        contratoRepository.save(contrato);

        return mapToRespuesta(contrato);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * Recalcula el hash SHA-256 del contenido congelado de un contrato ya
     * firmado por ambas partes y lo compara contra el hash almacenado
     * (REQ-NF-020), para detectar cualquier alteración posterior.
     *
     * @param idContrato identificador del contrato a verificar
     * @return el resultado de la verificación, con ambos hashes y sus fechas
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el contrato no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el contrato aún no está
     *         firmado por ambas partes y por lo tanto no tiene contenido congelado
     */
    public IntegrityVerificationResponse verifyHashIntegrity(Long idContrato) {
        Contract contrato = contratoRepository.findById(idContrato)
                .orElseThrow(() -> new ResourceNotFoundException("Contract no encontrado"));

        if (contrato.getHashContenido() == null) {
            throw new BusinessRuleException(
                    "El contrato aún no está firmado por ambas partes; no tiene un hash de contenido que verificar");
        }

        String hashRecalculado = sha256Hex(contrato.getContenidoCongelado());
        boolean integro = hashRecalculado.equals(contrato.getHashContenido());

        if (!integro) {
            log.error("Discrepancia de integridad en el contrato {}: hash guardado={}, recalculado={}",
                    idContrato, contrato.getHashContenido(), hashRecalculado);
        }

        return IntegrityVerificationResponse.builder()
                .idContrato(idContrato)
                .integro(integro)
                .hashAlmacenado(contrato.getHashContenido())
                .hashRecalculado(hashRecalculado)
                .fechaHashOriginal(contrato.getFechaHashContenido())
                .fechaVerificacion(LocalDateTime.now())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * @param idContrato identificador del contrato
     * @param idUsuarioSolicitante identificador de quien consulta; debe ser parte del pedido o admin
     * @return el contrato solicitado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el contrato no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si quien consulta no es parte
     *         del pedido asociado ni administrador
     */
    public ContractResponse getContract(Long idContrato, Long idUsuarioSolicitante) {
        Contract contrato = contratoRepository.findById(idContrato)
                .orElseThrow(() -> new ResourceNotFoundException("Contract no encontrado"));
        // H-02: evita el acceso a contratos ajenos (IDOR).
        OrderOwnershipValidator.validarPertenenciaOAdmin(contrato.getPedido(), idUsuarioSolicitante);
        return mapToRespuesta(contrato);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * @param idPedido identificador del pedido cuyo contrato se busca
     * @param idUsuarioSolicitante identificador de quien consulta; debe ser parte del pedido o admin
     * @return el contrato asociado a ese pedido
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el pedido no tiene contrato
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si quien consulta no es parte
     *         del pedido ni administrador
     */
    public ContractResponse getContractByOrder(Long idPedido, Long idUsuarioSolicitante) {
        Contract contrato = contratoRepository.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ResourceNotFoundException("No existe contrato para el pedido con ID: " + idPedido));
        // H-02: evita el acceso a contratos ajenos (IDOR).
        OrderOwnershipValidator.validarPertenenciaOAdmin(contrato.getPedido(), idUsuarioSolicitante);
        return mapToRespuesta(contrato);
    }

    @Override
    @Transactional(readOnly = true)
    /**
     * @param idContrato identificador del contrato
     * @param idUsuarioSolicitante identificador de quien consulta; debe ser parte del pedido o admin
     * @return el estado de firma del contrato (quién firmó y un mensaje descriptivo)
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el contrato no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si quien consulta no es parte
     *         del pedido asociado ni administrador
     */
    public SignatureStatusResponse getSignatureStatus(Long idContrato, Long idUsuarioSolicitante) {
        Contract contrato = contratoRepository.findById(idContrato)
                .orElseThrow(() -> new ResourceNotFoundException("Contract no encontrado"));
        // H-02: evita el acceso a contratos ajenos (IDOR).
        OrderOwnershipValidator.validarPertenenciaOAdmin(contrato.getPedido(), idUsuarioSolicitante);

        boolean firmaCreador = contrato.getHashFirmaCreador() != null;
        boolean firmaCliente = contrato.getHashFirmaCliente() != null;
        boolean ambas = firmaCreador && firmaCliente;

        String mensaje;
        if (ambas) {
            mensaje = "Contract completamente firmado por ambas partes";
        } else if (firmaCreador) {
            mensaje = "Esperando firma del Cliente";
        } else if (firmaCliente) {
            mensaje = "Esperando firma del Creador";
        } else {
            mensaje = "Pendiente de firma por ambas partes";
        }

        return SignatureStatusResponse.builder()
                .idContrato(idContrato)
                .firmaCreadorCompleta(firmaCreador)
                .firmaClienteCompleta(firmaCliente)
                .ambasFirmasCompletas(ambas)
                .mensajeEstado(mensaje)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generatePdf(Long idContrato, Long idUsuarioSolicitante) {
        long start = System.currentTimeMillis();

        Contract contrato = contratoRepository.findById(idContrato)
                .orElseThrow(() -> new ResourceNotFoundException("Contract no encontrado"));
        // H-02: evita descargar el PDF de un contrato ajeno (IDOR).
        OrderOwnershipValidator.validarPertenenciaOAdmin(contrato.getPedido(), idUsuarioSolicitante);

        String html = renderFullContract(contrato);
        byte[] pdf = pdfGeneracionServicio.generatePdfFromHtml(html);

        long elapsed = System.currentTimeMillis() - start;
        log.info("PDF generado para contrato {} en {} ms (RNF-06: max 5000ms)", idContrato, elapsed);

        return pdf;
    }

    // ── Métodos auxiliares ───────────────────────────────────────────────────

    /**
     * Codificación usada para el escape de placeholders (ver más abajo): con
     * ella, HtmlUtils.htmlEscape solo convierte los caracteres realmente
     * peligrosos para HTML (&lt; &gt; &amp; " ') y deja el resto —incluidas
     * las tildes y la ñ— como texto UTF-8 literal en vez de entidades HTML
     * nombradas (&aacute;, &ntilde;...). openhtmltopdf parsea el documento
     * como XML estricto, que solo reconoce las 5 entidades predefinidas por
     * XML; sin este parámetro, HtmlUtils.htmlEscape(String) usa por defecto
     * ISO-8859-1 y emite esas entidades nombradas para cualquier caracter no
     * ASCII, y el parser rechazaba TODO contrato cuyo texto tuviera una tilde
     * con "The entity ... was referenced, but not declared." (SAXParseException).
     */
    private static final String CODIFICACION_ESCAPE = "UTF-8";

    /**
     * Hallazgo SEC-01 (auditoria de seguridad): cada valor que entra aqui desde
     * datos de usuario (nombres, descripcion del servicio) se escapa con
     * HtmlUtils.htmlEscape antes de sustituirse en la plantilla. Antes se
     * interpolaban crudos: un creador podia poner una etiqueta HTML en la
     * descripcion de su servicio (p. ej. <img src="http://...">) y, al generar
     * el PDF, ese marcado se renderizaba tal cual. plantilla.getCuerpoHtmlPlantilla()
     * NO se escapa: es la plantilla legal en si, solo sembrable por migracion
     * (V13__seed_plantilla_contrato.sql), sin ningun endpoint que la edite.
     */
    private String generateContractHtml(ContractTemplate plantilla, Contract contrato) {
        Order pedido = contrato.getPedido();
        User creador = pedido.getServicio().getPerfil().getUsuario();
        User cliente = pedido.getUsuarioCliente();

        String html = plantilla.getCuerpoHtmlPlantilla();
        html = html.replace("{{nombre_creador}}",
                HtmlUtils.htmlEscape(creador.getNombres() + " " + creador.getApellidos(), CODIFICACION_ESCAPE));
        html = html.replace("{{nombre_cliente}}",
                HtmlUtils.htmlEscape(cliente.getNombres() + " " + cliente.getApellidos(), CODIFICACION_ESCAPE));
        html = html.replace("{{descripcion_servicio}}",
                HtmlUtils.htmlEscape(pedido.getServicio().getDescripcionDetallada(), CODIFICACION_ESCAPE));
        // precio_pactado, limite_revisiones, fecha_entrega y fecha_actual no son
        // controlables por el usuario (numeros/fechas calculados en servidor), pero
        // se escapan igual por uniformidad con el resto de placeholders.
        html = html.replace("{{precio_pactado}}",
                HtmlUtils.htmlEscape(pedido.getPrecioPactado().toString(), CODIFICACION_ESCAPE));
        html = html.replace("{{limite_revisiones}}",
                HtmlUtils.htmlEscape(String.valueOf(contrato.getLimiteRevisiones()), CODIFICACION_ESCAPE));
        html = html.replace("{{fecha_entrega}}", HtmlUtils.htmlEscape(
                pedido.getFechaEntregaEstimada() != null ? pedido.getFechaEntregaEstimada().toString() : "Por definir",
                CODIFICACION_ESCAPE));
        html = html.replace("{{fecha_actual}}",
                HtmlUtils.htmlEscape(LocalDate.now().toString(), CODIFICACION_ESCAPE));

        return html;
    }

    private String renderFullContract(Contract contrato) {
        String html = generateContractHtml(contrato.getPlantilla(), contrato);

        // Agregar hashes de firma al pie del documento. No requieren
        // HtmlUtils.htmlEscape: son hex SHA-256 calculados en servidor por
        // generateSignatureHash(), no texto libre de usuario.
        StringBuilder footer = new StringBuilder();
        // openhtmltopdf usa un parser XML estricto (XHTML): un <hr> sin cerrar
        // rompe el render con SAXParseException, que el catch genérico de
        // PdfGenerationServiceImpl reenvía como 500 sin detalle.
        footer.append("<hr/><div style='font-size:10px; color:#666;'>");
        if (contrato.getHashFirmaCreador() != null) {
            footer.append("<p>Firma Creador (SHA-256): ").append(contrato.getHashFirmaCreador()).append("</p>");
        }
        if (contrato.getHashFirmaCliente() != null) {
            footer.append("<p>Firma Cliente (SHA-256): ").append(contrato.getHashFirmaCliente()).append("</p>");
        }
        footer.append("</div>");

        // Insertar antes del cierre de </body>
        if (html.contains("</body>")) {
            html = html.replace("</body>", footer.toString() + "</body>");
        } else {
            html = html + footer.toString();
        }

        return html;
    }

    private String generateSignatureHash(Long idContrato, Long idUsuario) {
        String data = idContrato + ":" + idUsuario + ":" + Instant.now().toString();
        return sha256Hex(data);
    }

    /**
     * REQ-NF-020: congela el HTML ya renderizado (mismo contenido que
     * generatePdf ya produce) y guarda su hash SHA-256, una sola vez, al
     * completarse la segunda firma. La re-verificación posterior solo vuelve
     * a hashear ESTE contenido guardado -- nunca vuelve a llamar a
     * generateContractHtml, que incluye {{fecha_actual}} = LocalDate.now() y
     * por lo tanto no es reproducible día a día.
     */
    private void congelarContenidoYHash(Contract contrato) {
        String contenido = generateContractHtml(contrato.getPlantilla(), contrato);
        contrato.setContenidoCongelado(contenido);
        contrato.setHashContenido(sha256Hex(contenido));
        contrato.setFechaHashContenido(LocalDateTime.now());
        contrato.setFechaLimiteRetencion(LocalDateTime.now().plusYears(retencionAnios));
    }

    private static String sha256Hex(String data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error al generar hash SHA-256", e);
        }
    }

    private ContractResponse mapToRespuesta(Contract contrato) {
        Order pedido = contrato.getPedido();
        User creador = pedido.getServicio().getPerfil().getUsuario();
        User cliente = pedido.getUsuarioCliente();

        String htmlRenderizado = generateContractHtml(contrato.getPlantilla(), contrato);

        return ContractResponse.builder()
                .idContrato(contrato.getIdContrato())
                .idPedido(pedido.getIdPedido())
                .tituloServicio(pedido.getServicio().getTituloServicio())
                .idCreador(creador.getIdUsuario())
                .nombreCreador(creador.getNombres() + " " + creador.getApellidos())
                .idCliente(cliente.getIdUsuario())
                .nombreCliente(cliente.getNombres() + " " + cliente.getApellidos())
                .versionLegal(contrato.getPlantilla().getVersionLegal())
                .contenidoHtml(htmlRenderizado)
                .hashFirmaCreador(contrato.getHashFirmaCreador())
                .hashFirmaCliente(contrato.getHashFirmaCliente())
                .limiteRevisiones(contrato.getLimiteRevisiones())
                .fechaFormalizacion(contrato.getFechaFormalizacion())
                .urlDocumentoPdf(contrato.getUrlDocumentoPdf())
                .ambasFirmasCompletas(contrato.getHashFirmaCreador() != null && contrato.getHashFirmaCliente() != null)
                .build();
    }
}
