package uteq.edu.ec.artisync.service.legal.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    public RespuestaContrato generarContrato(Long idPedido) {
        Pedido pedido = pedidoRepository.findById(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Pedido no encontrado"));

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
            throw new ExcepcionReglaNegocio("No eres parte de este contrato");
        }

        contratoRepository.save(contrato);

        // Si ambos firmaron, crear sala de chat (M6 - placeholder)
        if (contrato.getHashFirmaCreador() != null && contrato.getHashFirmaCliente() != null) {
            log.info("Ambas partes firmaron contrato {}. Sala de chat pendiente (M6)", idContrato);
            // TODO M6: chatService.crearSala(pedido);
        }

        return mapToRespuesta(contrato);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaContrato obtenerContrato(Long idContrato) {
        Contrato contrato = contratoRepository.findById(idContrato)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Contrato no encontrado"));
        return mapToRespuesta(contrato);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaContrato obtenerContratoPorPedido(Long idPedido) {
        Contrato contrato = contratoRepository.findByPedidoIdPedido(idPedido)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("No existe contrato para el pedido con ID: " + idPedido));
        return mapToRespuesta(contrato);
    }

    @Override
    @Transactional(readOnly = true)
    public RespuestaEstadoFirma obtenerEstadoFirma(Long idContrato) {
        Contrato contrato = contratoRepository.findById(idContrato)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Contrato no encontrado"));

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
    public byte[] generarPdf(Long idContrato) {
        long start = System.currentTimeMillis();

        Contrato contrato = contratoRepository.findById(idContrato)
                .orElseThrow(() -> new ExcepcionRecursoNoEncontrado("Contrato no encontrado"));

        String html = renderizarContratoCompleto(contrato);
        byte[] pdf = pdfGeneracionServicio.generarPdfDesdeHtml(html);

        long elapsed = System.currentTimeMillis() - start;
        log.info("PDF generado para contrato {} en {} ms (RNF-06: max 5000ms)", idContrato, elapsed);

        return pdf;
    }

    // ── Métodos auxiliares ───────────────────────────────────────────────────

    private String generarContratoHtml(PlantillaContrato plantilla, Contrato contrato) {
        Pedido pedido = contrato.getPedido();
        Usuario creador = pedido.getServicio().getPerfil().getUsuario();
        Usuario cliente = pedido.getUsuarioCliente();

        String html = plantilla.getCuerpoHtmlPlantilla();
        html = html.replace("{{nombre_creador}}", creador.getNombres() + " " + creador.getApellidos());
        html = html.replace("{{nombre_cliente}}", cliente.getNombres() + " " + cliente.getApellidos());
        html = html.replace("{{descripcion_servicio}}", pedido.getServicio().getDescripcionDetallada());
        html = html.replace("{{precio_pactado}}", pedido.getPrecioPactado().toString());
        html = html.replace("{{limite_revisiones}}", String.valueOf(contrato.getLimiteRevisiones()));
        html = html.replace("{{fecha_entrega}}",
                pedido.getFechaEntregaEstimada() != null ? pedido.getFechaEntregaEstimada().toString() : "Por definir");
        html = html.replace("{{fecha_actual}}", LocalDate.now().toString());

        return html;
    }

    private String renderizarContratoCompleto(Contrato contrato) {
        String html = generarContratoHtml(contrato.getPlantilla(), contrato);

        // Agregar hashes de firma al pie del documento
        StringBuilder footer = new StringBuilder();
        footer.append("<hr><div style='font-size:10px; color:#666;'>");
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
                .nombreCreador(creador.getNombres() + " " + creador.getApellidos())
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
