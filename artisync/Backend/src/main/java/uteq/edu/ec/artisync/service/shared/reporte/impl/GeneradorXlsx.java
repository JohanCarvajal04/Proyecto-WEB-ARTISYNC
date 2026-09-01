package uteq.edu.ec.artisync.service.shared.reporte.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.springframework.stereotype.Component;
import uteq.edu.ec.artisync.service.shared.reporte.ColumnaReporte;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;
import uteq.edu.ec.artisync.service.shared.reporte.GeneradorReporte;
import uteq.edu.ec.artisync.service.shared.reporte.ModeloReporte;
import uteq.edu.ec.artisync.service.shared.reporte.TipoColumna;
import uteq.edu.ec.artisync.service.shared.reporte.TotalReporte;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * XLSX real vía Apache POI {@link SXSSFWorkbook} (streaming, memoria acotada — no
 * mantiene todas las filas en memoria como el XSSFWorkbook clásico). Hoja "Datos"
 * con tipos de celda reales (fecha, moneda, entero) + hoja "Info" con título,
 * filtros aplicados y quién/cuándo lo generó.
 */
@Slf4j
@Component
public class GeneradorXlsx implements GeneradorReporte {

    private static final int FILAS_EN_MEMORIA = 100;
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public FormatoReporte formato() {
        return FormatoReporte.XLSX;
    }

    @Override
    public <T> DocumentoGenerado generar(ModeloReporte<T> modelo) {
        try (SXSSFWorkbook libro = new SXSSFWorkbook(FILAS_EN_MEMORIA)) {
            try {
                Map<TipoColumna, CellStyle> estilosPorTipo = crearEstilosPorTipo(libro);
                CellStyle estiloEncabezado = crearEstiloEncabezado(libro);
                CellStyle estiloTotal = crearEstiloTotal(libro);

                escribirHojaDatos(libro, modelo, estiloEncabezado, estilosPorTipo, estiloTotal);
                escribirHojaInfo(libro, modelo, estiloEncabezado);

                ByteArrayOutputStream salida = new ByteArrayOutputStream();
                libro.write(salida);
                return new DocumentoGenerado(salida.toByteArray(), formato().contentType(), null);
            } finally {
                // SXSSFWorkbook escribe filas fuera de memoria en archivos temporales;
                // sin dispose() esos temporales quedan huérfanos en disco.
                libro.dispose();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Error al generar el documento XLSX: " + e.getMessage(), e);
        }
    }

    private <T> void escribirHojaDatos(SXSSFWorkbook libro, ModeloReporte<T> modelo, CellStyle estiloEncabezado,
                                        Map<TipoColumna, CellStyle> estilosPorTipo, CellStyle estiloTotal) {
        SXSSFSheet hoja = libro.createSheet(WorkbookUtil.createSafeSheetName("Datos"));
        List<ColumnaReporte<T>> columnas = modelo.getColumnas();

        SXSSFRow filaEncabezado = hoja.createRow(0);
        for (int c = 0; c < columnas.size(); c++) {
            SXSSFCell celda = filaEncabezado.createCell(c);
            celda.setCellValue(columnas.get(c).encabezado());
            celda.setCellStyle(estiloEncabezado);
            hoja.setColumnWidth(c, columnas.get(c).anchoCaracteres() * 256);
        }
        hoja.createFreezePane(0, 1);
        hoja.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(
                0, 0, 0, Math.max(0, columnas.size() - 1)));

        int numeroFila = 1;
        for (T fila : modelo.getFilas()) {
            SXSSFRow filaHoja = hoja.createRow(numeroFila++);
            for (int c = 0; c < columnas.size(); c++) {
                ColumnaReporte<T> columna = columnas.get(c);
                Object valor = columna.extractor().apply(fila);
                SXSSFCell celda = filaHoja.createCell(c);
                escribirValorCelda(celda, valor, columna.tipo());
                celda.setCellStyle(estilosPorTipo.get(columna.tipo()));
            }
        }

        List<TotalReporte> totales = modelo.getTotales();
        if (!totales.isEmpty()) {
            numeroFila++;
            for (TotalReporte total : totales) {
                SXSSFRow filaTotal = hoja.createRow(numeroFila++);
                SXSSFCell celdaEtiqueta = filaTotal.createCell(0);
                celdaEtiqueta.setCellValue(total.etiqueta());
                celdaEtiqueta.setCellStyle(estiloTotal);
                SXSSFCell celdaValor = filaTotal.createCell(1);
                escribirValorCelda(celdaValor, total.valor(), total.tipo());
                celdaValor.setCellStyle(estiloTotal);
            }
        }
    }

    private void escribirValorCelda(Cell celda, Object valor, TipoColumna tipo) {
        if (valor == null) {
            celda.setBlank();
            return;
        }
        switch (tipo) {
            case ENTERO -> celda.setCellValue(((Number) valor).doubleValue());
            case DECIMAL, MONEDA -> celda.setCellValue(
                    valor instanceof BigDecimal bd ? bd.doubleValue() : ((Number) valor).doubleValue());
            case FECHA -> {
                if (valor instanceof LocalDate fecha) {
                    celda.setCellValue(fecha);
                } else {
                    celda.setCellValue(valor.toString());
                }
            }
            case FECHA_HORA -> {
                if (valor instanceof LocalDateTime fechaHora) {
                    celda.setCellValue(fechaHora);
                } else {
                    celda.setCellValue(valor.toString());
                }
            }
            case BOOLEANO -> celda.setCellValue(Boolean.TRUE.equals(valor));
            default -> celda.setCellValue(valor.toString());
        }
    }

    private <T> void escribirHojaInfo(SXSSFWorkbook libro, ModeloReporte<T> modelo, CellStyle estiloEncabezado) {
        SXSSFSheet hoja = libro.createSheet(WorkbookUtil.createSafeSheetName("Info"));
        int fila = 0;

        fila = escribirParInfo(hoja, fila, "Título", modelo.getTitulo());
        if (modelo.getSubtitulo() != null) {
            fila = escribirParInfo(hoja, fila, "Subtítulo", modelo.getSubtitulo());
        }
        fila = escribirParInfo(hoja, fila, "Generado por", modelo.getGeneradoPor());
        fila = escribirParInfo(hoja, fila, "Generado el", modelo.getGeneradoEn().format(FORMATO_FECHA_HORA));
        fila = escribirParInfo(hoja, fila, "Total de filas", String.valueOf(modelo.getFilas().size()));

        if (!modelo.getFiltrosAplicados().isEmpty()) {
            fila++;
            SXSSFRow filaTitulo = hoja.createRow(fila++);
            SXSSFCell celda = filaTitulo.createCell(0);
            celda.setCellValue("Filtros aplicados");
            celda.setCellStyle(estiloEncabezado);
            for (Map.Entry<String, String> filtro : modelo.getFiltrosAplicados().entrySet()) {
                fila = escribirParInfo(hoja, fila, filtro.getKey(), filtro.getValue());
            }
        }

        hoja.setColumnWidth(0, 24 * 256);
        hoja.setColumnWidth(1, 48 * 256);
    }

    private int escribirParInfo(SXSSFSheet hoja, int numeroFila, String etiqueta, String valor) {
        SXSSFRow fila = hoja.createRow(numeroFila);
        fila.createCell(0).setCellValue(etiqueta);
        fila.createCell(1).setCellValue(valor != null ? valor : "");
        return numeroFila + 1;
    }

    private CellStyle crearEstiloEncabezado(SXSSFWorkbook libro) {
        Font fuente = libro.createFont();
        fuente.setBold(true);
        fuente.setColor(IndexedColors.WHITE.getIndex());
        CellStyle estilo = libro.createCellStyle();
        estilo.setFont(fuente);
        estilo.setFillForegroundColor(IndexedColors.GREY_50_PERCENT.getIndex());
        estilo.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
        return estilo;
    }

    private CellStyle crearEstiloTotal(SXSSFWorkbook libro) {
        Font fuente = libro.createFont();
        fuente.setBold(true);
        CellStyle estilo = libro.createCellStyle();
        estilo.setFont(fuente);
        return estilo;
    }

    private Map<TipoColumna, CellStyle> crearEstilosPorTipo(SXSSFWorkbook libro) {
        Map<TipoColumna, CellStyle> estilos = new EnumMap<>(TipoColumna.class);
        var formato = libro.createDataFormat();

        CellStyle fecha = libro.createCellStyle();
        fecha.setDataFormat(formato.getFormat("yyyy-mm-dd"));
        estilos.put(TipoColumna.FECHA, fecha);

        CellStyle fechaHora = libro.createCellStyle();
        fechaHora.setDataFormat(formato.getFormat("yyyy-mm-dd hh:mm:ss"));
        estilos.put(TipoColumna.FECHA_HORA, fechaHora);

        CellStyle moneda = libro.createCellStyle();
        moneda.setDataFormat(formato.getFormat("\"$\"#,##0.00"));
        estilos.put(TipoColumna.MONEDA, moneda);

        CellStyle decimal = libro.createCellStyle();
        decimal.setDataFormat(formato.getFormat("#,##0.00"));
        estilos.put(TipoColumna.DECIMAL, decimal);

        CellStyle entero = libro.createCellStyle();
        entero.setDataFormat(formato.getFormat("#,##0"));
        estilos.put(TipoColumna.ENTERO, entero);

        estilos.put(TipoColumna.TEXTO, libro.createCellStyle());
        estilos.put(TipoColumna.BOOLEANO, libro.createCellStyle());

        return estilos;
    }
}
