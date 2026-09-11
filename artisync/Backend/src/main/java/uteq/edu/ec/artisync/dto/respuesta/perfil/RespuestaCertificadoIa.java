package uteq.edu.ec.artisync.dto.respuesta.perfil;

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
 */
@Builder
public record RespuestaCertificadoIa(
        Long idCertificado,
        Long idUsuario,
        Long idEstadoVerificacion,
        String nombreEstadoVerificacion,
        String urlDocumentoS3,
        BigDecimal puntajeConfianzaIa,
        LocalDateTime fechaAnalisis
) {
}



