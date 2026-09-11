package uteq.edu.ec.artisync.service.respaldo;

import org.springframework.core.io.Resource;

/** Archivo de un respaldo listo para transmitirse en streaming (ver RespaldoControlador#descargar). */
/**
 * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
 *
 * @param recurso parametro requerido para la correcta ejecucion del procedimiento
 * @param nombreArchivo objeto binario multipart representando el documento o medio fisico
 * @param tamanoBytes parametro requerido para la correcta ejecucion del procedimiento
 * @return el resultado esperado de aplicar las reglas de negocio de la funcion
 * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
 */
public record ArchivoRespaldo(Resource recurso, String nombreArchivo, long tamanoBytes) {
}
