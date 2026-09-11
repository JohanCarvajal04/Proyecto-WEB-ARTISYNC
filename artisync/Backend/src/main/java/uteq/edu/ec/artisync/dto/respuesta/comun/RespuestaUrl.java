package uteq.edu.ec.artisync.dto.respuesta.comun;

/**
 * Objeto de transferencia de datos (DTO) utilizado como carga útil de respuesta (Response).
 * 
 * Propósito: Enlace temporal o firmado (ej. SAS token) para descargar o acceder a un recurso.
 * 
 * Este DTO se encarga de serializar la información hacia el cliente, enmascarando 
 * el modelo de dominio interno (Entidades JPA) y exponiendo estrictamente los 
 * atributos necesarios para cumplir con el contrato de esta vista del API.
 */
public record RespuestaUrl(String url) {
}



