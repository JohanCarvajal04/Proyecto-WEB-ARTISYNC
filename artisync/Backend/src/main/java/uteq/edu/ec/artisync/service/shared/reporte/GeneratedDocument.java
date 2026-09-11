package uteq.edu.ec.artisync.service.shared.reporte;

/** Un documento ya renderizado, listo para devolverse como descarga. */
/**
 * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
 *
 * @param contenido parametro requerido para la correcta ejecucion del procedimiento
 * @param contentType parametro requerido para la correcta ejecucion del procedimiento
 * @param nombreArchivo objeto binario multipart representando el documento o medio fisico
 * @return el resultado esperado de aplicar las reglas de negocio de la funcion
 * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
 */
public record GeneratedDocument(byte[] contenido, String contentType, String nombreArchivo) {
}
