package uteq.edu.ec.artisync.service.shared.report;

/** Un motor de renderizado para un {@link ReportFormat}. Cada implementación es un
 *  bean de Spring; {@code IExportService} los descubre todos por inyección de
 *  lista y despacha por {@link #formato()}. */
public interface ReportGenerator {

    /** @return el formato de salida que esta implementación produce */
    ReportFormat formato();

    /**
     * Renderiza el modelo de reporte en el formato de esta implementación.
     * @param modelo columnas y filas a renderizar, con su tipo de dato
     * @return el documento generado, listo para descargar
     */
    <T> GeneratedDocument generate(ReportModel<T> modelo);
}
