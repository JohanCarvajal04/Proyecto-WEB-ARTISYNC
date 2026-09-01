package uteq.edu.ec.artisync.service.shared.reporte;

/**
 * Punto de entrada único del motor común de exportación. Un dominio (auditoría,
 * finanzas, contratos...) construye un {@link ModeloReporte} y pide un
 * {@link FormatoReporte}; este servicio valida el tope de filas del formato y
 * delega en el {@link GeneradorReporte} correspondiente.
 */
public interface IServicioExportacion {

    /**
     * @throws uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio si el modelo supera
     *         el tope de filas del formato pedido (422 — el llamador debe acotar filtros)
     */
    <T> DocumentoGenerado exportar(ModeloReporte<T> modelo, FormatoReporte formato);
}
