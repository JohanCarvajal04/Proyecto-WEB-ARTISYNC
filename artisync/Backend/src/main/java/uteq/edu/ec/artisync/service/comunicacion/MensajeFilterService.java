package uteq.edu.ec.artisync.service.comunicacion;

/**
 * Servicio de filtrado de mensajes para detección de datos de contacto.
 * RF-15: Detecta teléfonos y correos electrónicos en el cuerpo del mensaje.
 */
public interface MensajeFilterService {

    /**
     * Indica si el texto contiene un número de teléfono o dirección de email.
     */
    boolean contieneContacto(String texto);

    /**
     * Retorna el nombre del patrón detectado ("TELEFONO" | "EMAIL" | "DESCONOCIDO").
     * Útil para registrar en InfraccionMensaje.patronDetectado.
     */
    String detectarPatron(String texto);
}
