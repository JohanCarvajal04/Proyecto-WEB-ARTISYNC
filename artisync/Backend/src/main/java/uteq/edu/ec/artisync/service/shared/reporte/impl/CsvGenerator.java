package uteq.edu.ec.artisync.service.shared.reporte.impl;

import org.springframework.stereotype.Component;
import uteq.edu.ec.artisync.service.shared.reporte.ReportColumn;
import uteq.edu.ec.artisync.service.shared.reporte.GeneratedDocument;
import uteq.edu.ec.artisync.service.shared.reporte.ValueFormatter;
import uteq.edu.ec.artisync.service.shared.reporte.ReportFormat;
import uteq.edu.ec.artisync.service.shared.reporte.ReportGenerator;
import uteq.edu.ec.artisync.service.shared.reporte.ReportModel;
import uteq.edu.ec.artisync.util.CsvUtil;

import java.nio.charset.StandardCharsets;

/**
 * CSV plano: solo cabecera + filas. Totales y metadatos (filtros, quién/cuándo lo
 * generó) se quedan fuera a propósito — el CSV es el formato que otros sistemas
 * vuelven a parsear, así que se mantiene como dato puro (RFC 4180). Quien quiera
 * un documento con totales y contexto tiene XLSX o PDF.
 */
@Component
public class CsvGenerator implements ReportGenerator {

    @Override
    /**
     * Ejecuta la logica de negocio asociada a la operacion solicitada por el flujo principal.
     *
     * @return el resultado esperado de aplicar las reglas de negocio de la funcion
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException ante un flujo inconsistente u omision en restricciones primarias de la entidad
     */
    public ReportFormat formato() {
        return ReportFormat.CSV;
    }

    @Override
    public <T> GeneratedDocument generar(ReportModel<T> modelo) {
        StringBuilder csv = new StringBuilder(CsvUtil.BOM_UTF8);

        var columnas = modelo.getColumnas();
        for (int i = 0; i < columnas.size(); i++) {
            if (i > 0) {
                csv.append(',');
            }
            csv.append(CsvUtil.escapeCsv(columnas.get(i).encabezado()));
        }
        csv.append('\n');

        for (T fila : modelo.getFilas()) {
            for (int i = 0; i < columnas.size(); i++) {
                if (i > 0) {
                    csv.append(',');
                }
                ReportColumn<T> columna = columnas.get(i);
                Object valor = columna.extractor().apply(fila);
                csv.append(CsvUtil.escapeCsv(ValueFormatter.texto(valor, columna.tipo())));
            }
            csv.append('\n');
        }

        byte[] contenido = csv.toString().getBytes(StandardCharsets.UTF_8);
        return new GeneratedDocument(contenido, formato().contentType(), null);
    }
}
