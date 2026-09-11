package uteq.edu.ec.artisync.dto.respuesta.comun;

/**
 * Objeto de transferencia de datos (DTO) utilizado como carga útil de respuesta (Response).
 * 
 * Propósito: Proyeccion de un mensaje de chat dentro de una sala encriptada.
 * 
 * Este DTO se encarga de serializar la información hacia el cliente, enmascarando 
 * el modelo de dominio interno (Entidades JPA) y exponiendo estrictamente los 
 * atributos necesarios para cumplir con el contrato de esta vista del API.
 */
public record RespuestaMensaje(
        String mensaje
) {
    public String getMensaje() {
        return mensaje;
    }

    public String getMessage() {
        return mensaje;
    }
}



