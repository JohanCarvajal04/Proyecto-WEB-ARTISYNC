package uteq.edu.ec.artisync.service.comunicacion;

/**
 * Servicio de filtrado de mensajes para detección de datos de contacto.
 * RF-15: Detecta teléfonos y correos electrónicos en el cuerpo del mensaje.
 */
public interface MensajeFilterService {

    /**
     * Indica si el texto contiene un número de teléfono o dirección de email.
     *
     * @param texto cuerpo del mensaje a analizar
     * @return {@code true} si se detecta un teléfono o un correo electrónico
     */
    boolean contieneContacto(String texto);

    /**
     * Retorna el nombre del patrón detectado ("TELEFONO" | "EMAIL" | "DESCONOCIDO").
     * Útil para registrar en InfraccionMensaje.patronDetectado.
     *
     * @param texto cuerpo del mensaje a analizar
     * @return el nombre del patrón detectado
     */
    String detectarPatron(String texto);
}
