package uteq.edu.ec.artisync.service.legal.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaContrato;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaEstadoFirma;
import uteq.edu.ec.artisync.entity.legal.Contrato;
import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.pedido.PlantillaContrato;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import uteq.edu.ec.artisync.exception.ExcepcionRecursoNoEncontrado;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.legal.ContratoRepository;
import uteq.edu.ec.artisync.repository.pedido.PedidoRepository;
import uteq.edu.ec.artisync.repository.pedido.PlantillaContratoRepository;
import uteq.edu.ec.artisync.service.legal.IContratoServicio;
import uteq.edu.ec.artisync.service.legal.IPdfGeneracionServicio;
import uteq.edu.ec.artisync.util.ValidadorPertenenciaPedido;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContratoServicioImpl implements IContratoServicio {

    private final ContratoRepository contratoRepository;
    private final PedidoRepository pedidoRepository;
    private final PlantillaContratoRepository plantillaContratoRepository;
    private final IPdfGeneracionServicio pdfGeneracionServicio;

    @Override
    @Transactional
    @Auditable(accion = "CONTRATO_GENERAR", modulo = ModuloAuditoria.FINANZAS,
            entidad = "contratos", idEntidad = "#resultado.idContrato")
    public RespuestaContrato generarContrato(Long idPedido, Long idUsuarioSolicitante) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Pedido no encontrado"));
        // H-02: evita que cualquier autenticado genere un contrato sobre un pedido ajeno.
        ValidadorPertenenciaPedido.validarPertenenciaOAdmin(pedido, idUsuarioSolicitante);

        // Verificar que no exista ya un contrato para este pedido
        if (contratoRepository.findByPedidoIdPedido(idPedido).isPresent()) {
            throw new ExcepcionReglaNegocio("Ya existe un contrato para este pedido");
        }

        // Obtener plantilla activa (la más reciente)
        PlantillaContrato plantilla = plantillaContratoRepository.findFirstByOrderByIdPlantillaDesc()
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("No hay plantillas de contrato disponibles en el sistema"));

        // Crear contrato
        Contrato contrato = Contrato.builder()
                .pedido(pedido)
                .plantilla(plantilla)
                .limiteRevisiones(pedido.getServicio().getLimiteRevisionesBase() != null
                        ? pedido.getServicio().getLimiteRevisionesBase() : 0)
                .build();

        contrato = contratoRepository.save(contrato);
        log.info("Contrato {} generado para pedido {}", contrato.getIdContrato(), idPedido);

        return mapToRespuesta(contrato);
    }

    @Override
    @Transactional
    @Auditable(accion = "CONTRATO_FIRMAR", modulo = ModuloAuditoria.FINANZAS,
            entidad = "contratos", idEntidad = "#idContrato")
    public RespuestaContrato firmarContrato(Long idContrato, Long idUsuario) {
        Contrato contrato = contratoRepository.findById(idContrato)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Contrato no encontrado"));

        Pedido pedido = contrato.getPedido();
        String hash = generarHashFirma(idContrato, idUsuario);

        Long idCreador = pedido.getServicio().getPerfil().getUsuario().getIdUsuario();
        Long idCliente = pedido.getUsuarioCliente().getIdUsuario();

        if (idUsuario.equals(idCreador)) {
            if (contrato.getHashFirmaCreador() != null) {
                throw new ExcepcionReglaNegocio("El creador ya firmo este contrato");
            }
            contrato.setHashFirmaCreador(hash);
            log.info("Contrato {} firmado por creador (usuario {})", idContrato, idUsuario);
        } else if (idUsuario.equals(idCliente)) {
            if (contrato.getHashFirmaCliente() != null) {
                throw new ExcepcionReglaNegocio("El cliente ya firmo este contrato");
            }
            contrato.setHashFirmaCliente(hash);
            log.info("Contrato {} firmado por cliente (usuario {})", idContrato, idUsuario);
        } else {
            // H-02: 403, no 422 — coherente con el resto del proyecto (ManejadorGlobalExcepciones).
            throw new AccessDeniedException("No eres parte de este contrato");
        }

        contratoRepository.save(contrato);

        return mapToRespuesta(contrato);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaContrato obtenerContrato(Long idContrato, Long idUsuarioSolicitante) {
        Contrato contrato = contratoRepository.findById(idContrato)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Contrato no encontrado"));
        // H-02: evita el acceso a contratos ajenos (IDOR).
        ValidadorPertenenciaPedido.validarPertenenciaOAdmin(contrato.getPedido(), idUsuarioSolicitante);
        return mapToRespuesta(contrato);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaContrato obtenerContratoPorPedido(Long idPedido, Long idUsuarioSolicitante) {
        Contrato contrato = contratoRepository.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("No existe contrato para el pedido con ID: " + idPedido));
        // H-02: evita el acceso a contratos ajenos (IDOR).
        ValidadorPertenenciaPedido.validarPertenenciaOAdmin(contrato.getPedido(), idUsuarioSolicitante);
        return mapToRespuesta(contrato);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaEstadoFirma obtenerEstadoFirma(Long idContrato, Long idUsuarioSolicitante) {
        Contrato contrato = contratoRepository.findById(idContrato)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Contrato no encontrado"));
        // H-02: evita el acceso a contratos ajenos (IDOR).
        ValidadorPertenenciaPedido.validarPertenenciaOAdmin(contrato.getPedido(), idUsuarioSolicitante);

        boolean firmaCreador = contrato.getHashFirmaCreador() != null;
        boolean firmaCliente = contrato.getHashFirmaCliente() != null;
        boolean ambas = firmaCreador && firmaCliente;

        String mensaje;
        if (ambas) {
            mensaje = "Contrato completamente firmado por ambas partes";
        } else if (firmaCreador) {
            mensaje = "Esperando firma del Cliente";
        } else if (firmaCliente) {
            mensaje = "Esperando firma del Creador";
        } else {
            mensaje = "Pendiente de firma por ambas partes";
        }

        return RespuestaEstadoFirma.builder()
                .idContrato(idContrato)
                .firmaCreadorCompleta(firmaCreador)
                .firmaClienteCompleta(firmaCliente)
                .ambasFirmasCompletas(ambas)
                .mensajeEstado(mensaje)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] generarPdf(Long idContrato, Long idUsuarioSolicitante) {
        long start = System.currentTimeMillis();

        Contrato contrato = contratoRepository.findById(idContrato)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Contrato no encontrado"));
        // H-02: evita descargar el PDF de un contrato ajeno (IDOR).
        ValidadorPertenenciaPedido.validarPertenenciaOAdmin(contrato.getPedido(), idUsuarioSolicitante);

        String html = renderizarContratoCompleto(contrato);
        byte[] pdf = pdfGeneracionServicio.generarPdfDesdeHtml(html);

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
    private String generarContratoHtml(PlantillaContrato plantilla, Contrato contrato) {
        Pedido pedido = contrato.getPedido();
        Usuario creador = pedido.getServicio().getPerfil().getUsuario();
        Usuario cliente = pedido.getUsuarioCliente();

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

    private String renderizarContratoCompleto(Contrato contrato) {
        String html = generarContratoHtml(contrato.getPlantilla(), contrato);

        // Agregar hashes de firma al pie del documento. No requieren
        // HtmlUtils.htmlEscape: son hex SHA-256 calculados en servidor por
        // generarHashFirma(), no texto libre de usuario.
        StringBuilder footer = new StringBuilder();
        // openhtmltopdf usa un parser XML estricto (XHTML): un <hr> sin cerrar
        // rompe el render con SAXParseException, que el catch genérico de
        // PdfGeneracionServicioImpl reenvía como 500 sin detalle.
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

    private String generarHashFirma(Long idContrato, Long idUsuario) {
        try {
            String data = idContrato + ":" + idUsuario + ":" + Instant.now().toString();
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

    private RespuestaContrato mapToRespuesta(Contrato contrato) {
        Pedido pedido = contrato.getPedido();
        Usuario creador = pedido.getServicio().getPerfil().getUsuario();
        Usuario cliente = pedido.getUsuarioCliente();

        String htmlRenderizado = generarContratoHtml(contrato.getPlantilla(), contrato);

        return RespuestaContrato.builder()
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
