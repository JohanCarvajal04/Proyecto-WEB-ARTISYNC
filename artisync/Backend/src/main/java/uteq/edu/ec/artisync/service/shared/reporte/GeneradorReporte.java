package uteq.edu.ec.artisync.service.shared.reporte;

/** Un motor de renderizado para un {@link FormatoReporte}. Cada implementación es un
 *  bean de Spring; {@code IServicioExportacion} los descubre todos por inyección de
 *  lista y despacha por {@link #formato()}. */
public interface GeneradorReporte {

    FormatoReporte formato();

    <T> DocumentoGenerado generar(ModeloReporte<T> modelo);
}
