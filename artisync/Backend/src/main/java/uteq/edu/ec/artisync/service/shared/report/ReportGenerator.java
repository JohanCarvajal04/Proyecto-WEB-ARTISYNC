package uteq.edu.ec.artisync.service.shared.report;

/** Un motor de renderizado para un {@link ReportFormat}. Cada implementación es un
 *  bean de Spring; {@code IExportService} los descubre todos por inyección de
 *  lista y despacha por {@link #formato()}. */
public interface ReportGenerator {

    ReportFormat formato();

    <T> GeneratedDocument generate(ReportModel<T> modelo);
}
