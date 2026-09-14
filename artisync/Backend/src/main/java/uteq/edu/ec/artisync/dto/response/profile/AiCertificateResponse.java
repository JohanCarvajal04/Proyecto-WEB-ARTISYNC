package uteq.edu.ec.artisync.dto.response.profile;

import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Objeto de transferencia de datos (DTO) utilizado como carga útil de respuesta (Response).
 * 
 * Propósito: Proyeccion de la declaracion de uso de herramientas de IA del creador.
 * 
 * Este DTO se encarga de serializar la información hacia el cliente, enmascarando 
 * el modelo de dominio interno (Entidades JPA) y exponiendo estrictamente los 
 * atributos necesarios para cumplir con el contrato de esta vista del API.
 *
 * @param idCertificado id del certificado
 * @param idUsuario id del usuario creador que lo declaró
 * @param idEstadoVerificacion id del estado de verificación actual
 * @param nombreEstadoVerificacion nombre legible del estado de verificación
 * @param urlDocumentoS3 referencia de almacenamiento del documento
 * @param puntajeConfianzaIa puntaje de confianza devuelto por el análisis de IA
 * @param fechaAnalisis fecha en que se analizó el documento
 */
@Builder
public record AiCertificateResponse(
        Long idCertificado,
        Long idUsuario,
        Long idEstadoVerificacion,
        String nombreEstadoVerificacion,
        String urlDocumentoS3,
        BigDecimal puntajeConfianzaIa,
        LocalDateTime fechaAnalisis
) {
}



