package uteq.edu.ec.artisync.service.shared.reporte.impl;

import org.springframework.stereotype.Component;
import uteq.edu.ec.artisync.service.shared.reporte.ColumnaReporte;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormateadorValores;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;
import uteq.edu.ec.artisync.service.shared.reporte.GeneradorReporte;
import uteq.edu.ec.artisync.service.shared.reporte.ModeloReporte;
import uteq.edu.ec.artisync.util.CsvUtil;

import java.nio.charset.StandardCharsets;

/**
 * CSV plano: solo cabecera + filas. Totales y metadatos (filtros, quién/cuándo lo
 * generó) se quedan fuera a propósito — el CSV es el formato que otros sistemas
 * vuelven a parsear, así que se mantiene como dato puro (RFC 4180). Quien quiera
 * un documento con totales y contexto tiene XLSX o PDF.
 */
@Component
public class GeneradorCsv implements GeneradorReporte {

    @Override
    public FormatoReporte formato() {
        return FormatoReporte.CSV;
    }

    @Override
    public <T> DocumentoGenerado generar(ModeloReporte<T> modelo) {
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
                ColumnaReporte<T> columna = columnas.get(i);
                Object valor = columna.extractor().apply(fila);
                csv.append(CsvUtil.escapeCsv(FormateadorValores.texto(valor, columna.tipo())));
            }
            csv.append('\n');
        }

        byte[] contenido = csv.toString().getBytes(StandardCharsets.UTF_8);
        return new DocumentoGenerado(contenido, formato().contentType(), null);
    }
}
