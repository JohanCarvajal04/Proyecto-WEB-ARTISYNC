package uteq.edu.ec.artisync.service.shared.ia;

import uteq.edu.ec.artisync.dto.ia.*;

import java.util.List;

/**
 * Contrato Strategy de IA. Los seis métodos existen aunque hoy solo se
 * cableen verificarIdentidad/analizarCertificado (REQ-F-006/007); el resto
 * queda listo para futuras herramientas del moderador sin romper la interfaz.
 */
public interface IaService {

    IaVerificacionResponse verificarIdentidad(byte[] imagenBytes, String mimeType);

    IaVerificacionResponse analizarCertificado(byte[] imagenBytes, String mimeType);

    IaModeracionResponse moderarContenido(String textoMensaje);

    IaClasificacionResponse clasificarServicio(String titulo, String descripcion,
                                                List<String> categoriasDisponibles);

    List<String> sugerirPreguntasBriefing(String categoria, String titulo, String descripcion);

    IaResenaResponse analizarResena(String textoResena, int estrellas);
}
