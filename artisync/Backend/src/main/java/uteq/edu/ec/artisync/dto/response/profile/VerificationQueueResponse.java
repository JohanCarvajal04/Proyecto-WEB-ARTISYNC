package uteq.edu.ec.artisync.dto.response.profile;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Objeto de transferencia de datos (DTO) utilizado como carga útil de respuesta (Response).
 * 
 * Propósito: Elemento de la lista de usuarios pendientes de validacion KYC para el dashboard administrativo.
 * 
 * Este DTO se encarga de serializar la información hacia el cliente, enmascarando 
 * el modelo de dominio interno (Entidades JPA) y exponiendo estrictamente los 
 * atributos necesarios para cumplir con el contrato de esta vista del API.
 *
 * @param idCertificado id del certificado pendiente de revisión
 * @param idUsuario id del usuario que lo solicitó
 * @param nombreUsuario nombre completo del usuario
 * @param tipoDocumento tipo de documento cargado
 * @param nombreEstado nombre del estado de verificación actual
 * @param veredictoIa veredicto emitido por el análisis de IA
 * @param puntajeConfianzaIa puntaje de confianza devuelto por la IA
 * @param fechaAnalisis fecha en que se analizó el documento
 */
@Builder
public record VerificationQueueResponse(
        Long idCertificado,
        Long idUsuario,
        String nombreUsuario,
        String tipoDocumento,
        String nombreEstado,
        String veredictoIa,
        BigDecimal puntajeConfianzaIa,
        LocalDateTime fechaAnalisis
) {
}



