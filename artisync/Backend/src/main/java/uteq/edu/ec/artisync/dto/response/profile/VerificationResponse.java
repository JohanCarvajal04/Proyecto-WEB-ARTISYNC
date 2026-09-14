package uteq.edu.ec.artisync.dto.response.profile;

import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Objeto de transferencia de datos (DTO) utilizado como carga útil de respuesta (Response).
 * 
 * Propósito: Proyeccion del estado detallado del proceso KYC de un usuario.
 * 
 * Este DTO se encarga de serializar la información hacia el cliente, enmascarando 
 * el modelo de dominio interno (Entidades JPA) y exponiendo estrictamente los 
 * atributos necesarios para cumplir con el contrato de esta vista del API.
 *
 * @param idCertificado id del certificado
 * @param idUsuario id del usuario que lo declaró
 * @param tipoDocumento tipo de documento cargado
 * @param nombreEstadoVerificacion nombre del estado de verificación actual
 * @param veredictoIa veredicto emitido por el análisis de IA
 * @param puntajeConfianzaIa puntaje de confianza devuelto por la IA
 * @param razonIa razón dada por la IA para su veredicto
 * @param datosExtraidosIa datos extraídos del documento por la IA, serializados
 * @param fechaDictamenIa fecha en que la IA emitió su dictamen
 * @param idModerador id del moderador que tomó la decisión final, si ya se decidió
 * @param fechaDecision fecha en que el moderador tomó la decisión
 * @param notaModerador nota del moderador que justifica la decisión
 * @param fechaAnalisis fecha en que se analizó el documento
 */
@Builder
public record VerificationResponse(
        Long idCertificado,
        Long idUsuario,
        String tipoDocumento,
        String nombreEstadoVerificacion,
        String veredictoIa,
        BigDecimal puntajeConfianzaIa,
        String razonIa,
        String datosExtraidosIa,
        LocalDateTime fechaDictamenIa,
        Long idModerador,
        LocalDateTime fechaDecision,
        String notaModerador,
        LocalDateTime fechaAnalisis
) {
}



