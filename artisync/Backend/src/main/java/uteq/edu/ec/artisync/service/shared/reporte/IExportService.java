package uteq.edu.ec.artisync.service.shared.reporte;

/**
 * Punto de entrada único del motor común de exportación. Un dominio (auditoría,
 * finanzas, contratos...) construye un {@link ReportModel} y pide un
 * {@link ReportFormat}; este servicio valida el tope de filas del formato y
 * delega en el {@link ReportGenerator} correspondiente.
 */
public interface IExportService {

    /**
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el modelo supera
     *         el tope de filas del formato pedido (422 — el llamador debe acotar filtros)
     */
    <T> GeneratedDocument exportar(ReportModel<T> modelo, ReportFormat formato);
}
