package uteq.edu.ec.artisync.dto.respuesta.perfil;

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
 */
@Builder
public record RespuestaColaVerificacion(
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



