package uteq.edu.ec.artisync.service.shared.reporte;

/** Formato de salida de un reporte. Cada uno lleva su propio tope de filas: un PDF
 *  de 50 000 filas es inservible y revienta memoria, así que el tope no puede ser
 *  uno solo para los tres formatos. */
public enum FormatoReporte {

    CSV("text/csv; charset=UTF-8", "csv", 50_000),
    XLSX("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", "xlsx", 100_000),
    PDF("application/pdf", "pdf", 5_000);

    private final String contentType;
    private final String extension;
    private final int topeFilas;

    FormatoReporte(String contentType, String extension, int topeFilas) {
        this.contentType = contentType;
        this.extension = extension;
        this.topeFilas = topeFilas;
    }

    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public String contentType() {
        return contentType;
    }

    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public String extension() {
        return extension;
    }

    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public int topeFilas() {
        return topeFilas;
    }
}
