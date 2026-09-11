package uteq.edu.ec.artisync.dto.respuesta.perfil;

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
 */
@Builder
public record RespuestaVerificacion(
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



