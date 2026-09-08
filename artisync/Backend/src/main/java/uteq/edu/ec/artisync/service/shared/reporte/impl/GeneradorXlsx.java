package uteq.edu.ec.artisync.service.shared.reporte.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import uteq.edu.ec.artisync.service.shared.reporte.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Generador XLSX vía Apache POI {@link SXSSFWorkbook} con branding de Artisync:
 * - Color morado corporativo (#7B39B2 / RGB: 123, 57, 178) en encabezados y acentos.
 * - Hoja "Datos" con tipos de celda nativos (fecha, moneda, número).
 * - Hoja "Resumen" (cuando el reporte incluye gráficas o KPIs) con tablas de distribución y gráficas incrustadas.
 * - Hoja "Info" con metadatos de auditoría y filtros aplicados.
 */
@Slf4j
@Component
public class GeneradorXlsx implements GeneradorReporte {

    private static final int FILAS_EN_MEMORIA = 100;
    private static final DateTimeFormatter FORMATO_FECHA_HORA = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String RUTA_LOGO = "reportes/logo-artisync.png";

    // RGB Morado Artisync #7B39B2
    private static final byte[] RGB_MORADO_ARTISYNC = new byte[]{(byte) 123, (byte) 57, (byte) 178};
    // RGB Morado Oscuro #5B21B6
    private static final byte[] RGB_MORADO_OSCURO = new byte[]{(byte) 91, (byte) 33, (byte) 182};

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

                // Si el reporte incluye gráficas o métricas clave, creamos primero la hoja "Resumen" para que sea visible de inmediato
                if (!modelo.getGraficas().isEmpty() || !modelo.getKpis().isEmpty()) {
                    escribirHojaResumen(libro, modelo, estiloEncabezado);
                }

                // Hoja Datos
                escribirHojaDatos(libro, modelo, estiloEncabezado, estilosPorTipo, estiloTotal);

                // Hoja Info con metadatos
                escribirHojaInfo(libro, modelo, estiloEncabezado);

                // Activar siempre la primera hoja
                libro.setActiveSheet(0);
                libro.setSelectedTab(0);

                ByteArrayOutputStream salida = new ByteArrayOutputStream();
                libro.write(salida);
                return new DocumentoGenerado(salida.toByteArray(), formato().contentType(), null);
            } finally {
                libro.dispose();
            }
        } catch (IOException e) {
            throw new RuntimeException("Error al generar el documento XLSX: " + e.getMessage(), e);
        }
    }

    private <T> void escribirHojaDatos(SXSSFWorkbook libro, ModeloReporte<T> modelo, CellStyle estiloEncabezado,
                                        Map<TipoColumna, CellStyle> estilosPorTipo, CellStyle estiloTotal) {
        SXSSFSheet hoja = libro.createSheet(WorkbookUtil.createSafeSheetName("Datos"));
        List<ColumnaReporte<T>> columnas = modelo.getColumnas();

        SXSSFRow filaEncabezado = hoja.createRow(0);
        filaEncabezado.setHeightInPoints(24);
        for (int c = 0; c < columnas.size(); c++) {
            SXSSFCell celda = filaEncabezado.createCell(c);
            celda.setCellValue(columnas.get(c).encabezado());
            celda.setCellStyle(estiloEncabezado);
            hoja.setColumnWidth(c, columnas.get(c).anchoCaracteres() * 256);
        }
        hoja.createFreezePane(0, 1);
        hoja.setAutoFilter(new CellRangeAddress(0, 0, 0, Math.max(0, columnas.size() - 1)));

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

    private <T> void escribirHojaResumen(SXSSFWorkbook libro, ModeloReporte<T> modelo, CellStyle estiloEncabezado) {
        SXSSFSheet hoja = libro.createSheet(WorkbookUtil.createSafeSheetName("Resumen"));
        int filaActual = 0;

        // Estilo título resumen
        CellStyle estiloTitulo = crearEstiloTitulo(libro);
        SXSSFRow filaTitulo = hoja.createRow(filaActual++);
        filaTitulo.setHeightInPoints(28);
        SXSSFCell celdaTit = filaTitulo.createCell(0);
        celdaTit.setCellValue("ArtiSync — Resumen Ejecutivo y Estadísticas");
        celdaTit.setCellStyle(estiloTitulo);

        // Subtítulo
        SXSSFRow filaSub = hoja.createRow(filaActual++);
        filaSub.createCell(0).setCellValue("Reporte generado el " + modelo.getGeneradoEn().format(FORMATO_FECHA_HORA)
                + " por " + modelo.getGeneradoPor());

        filaActual++; // Espacio

        // Sección KPIs si existen
        if (!modelo.getKpis().isEmpty()) {
            SXSSFRow filaKpiHeader = hoja.createRow(filaActual++);
            SXSSFCell cKpi = filaKpiHeader.createCell(0);
            cKpi.setCellValue("INDICADORES CLAVE (KPIs)");
            cKpi.setCellStyle(estiloEncabezado);

            CellStyle estiloKpiEtiqueta = libro.createCellStyle();
            Font fuenteBold = libro.createFont();
            fuenteBold.setBold(true);
            estiloKpiEtiqueta.setFont(fuenteBold);

            for (KpiReporte kpi : modelo.getKpis()) {
                SXSSFRow r = hoja.createRow(filaActual++);
                SXSSFCell c1 = r.createCell(0);
                c1.setCellValue(kpi.etiqueta());
                c1.setCellStyle(estiloKpiEtiqueta);

                SXSSFCell c2 = r.createCell(1);
                c2.setCellValue(kpi.valor());

                if (kpi.descripcion() != null) {
                    r.createCell(2).setCellValue(kpi.descripcion());
                }
            }
            filaActual++; // Espacio
        }

        // Sección Gráficas y tablas de desglose
        if (!modelo.getGraficas().isEmpty()) {
            Drawing<?> drawing = hoja.createDrawingPatriarch();

            for (GraficaReporte grafica : modelo.getGraficas()) {
                SXSSFRow filaSec = hoja.createRow(filaActual++);
                filaSec.setHeightInPoints(20);
                SXSSFCell cSec = filaSec.createCell(0);
                cSec.setCellValue(grafica.titulo().toUpperCase());
                cSec.setCellStyle(estiloEncabezado);

                // Tabla de datos de la gráfica a la izquierda
                if (grafica.datos() != null && !grafica.datos().isEmpty()) {
                    long totalDatos = grafica.datos().values().stream().mapToLong(Long::longValue).sum();

                    SXSSFRow rHead = hoja.createRow(filaActual++);
                    rHead.createCell(0).setCellValue("Categoría");
                    rHead.createCell(1).setCellValue("Cantidad");
                    rHead.createCell(2).setCellValue("Porcentaje");

                    for (Map.Entry<String, Long> entry : grafica.datos().entrySet()) {
                        SXSSFRow rDato = hoja.createRow(filaActual++);
                        rDato.createCell(0).setCellValue(entry.getKey());
                        rDato.createCell(1).setCellValue(entry.getValue());
                        double pct = totalDatos > 0 ? ((double) entry.getValue() / totalDatos) * 100.0 : 0.0;
                        rDato.createCell(2).setCellValue(String.format("%.1f%%", pct));
                    }
                }

                // Incrustar imagen de la gráfica a la derecha
                if (grafica.imagenPng() != null && grafica.imagenPng().length > 0) {
                    try {
                        int pictureIdx = libro.addPicture(grafica.imagenPng(), Workbook.PICTURE_TYPE_PNG);
                        ClientAnchor anchor = libro.getCreationHelper().createClientAnchor();
                        anchor.setCol1(4); // Columna E
                        anchor.setRow1(Math.max(0, filaActual - 10));
                        anchor.setCol2(11); // Columna L
                        anchor.setRow2(filaActual + 6);
                        drawing.createPicture(anchor, pictureIdx);
                    } catch (Exception e) {
                        log.warn("No se pudo incrustar imagen de la gráfica en Excel", e);
                    }
                }

                filaActual += 3; // Espaciado entre gráficas
            }
        }

        hoja.setColumnWidth(0, 30 * 256);
        hoja.setColumnWidth(1, 16 * 256);
        hoja.setColumnWidth(2, 16 * 256);
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
        estilo.setAlignment(HorizontalAlignment.LEFT);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);

        // Color morado característico Artisync (#7B39B2)
        if (estilo instanceof XSSFCellStyle xssfEstilo) {
            XSSFColor moradoArtisync = new XSSFColor(RGB_MORADO_ARTISYNC, new DefaultIndexedColorMap());
            xssfEstilo.setFillForegroundColor(moradoArtisync);
        } else {
            estilo.setFillForegroundColor(IndexedColors.VIOLET.getIndex());
        }
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return estilo;
    }

    private CellStyle crearEstiloTitulo(SXSSFWorkbook libro) {
        Font fuente = libro.createFont();
        fuente.setBold(true);
        fuente.setFontHeightInPoints((short) 14);
        fuente.setColor(IndexedColors.WHITE.getIndex());
        CellStyle estilo = libro.createCellStyle();
        estilo.setFont(fuente);
        estilo.setAlignment(HorizontalAlignment.LEFT);
        estilo.setVerticalAlignment(VerticalAlignment.CENTER);

        if (estilo instanceof XSSFCellStyle xssfEstilo) {
            XSSFColor moradoOscuro = new XSSFColor(RGB_MORADO_OSCURO, new DefaultIndexedColorMap());
            xssfEstilo.setFillForegroundColor(moradoOscuro);
        } else {
            estilo.setFillForegroundColor(IndexedColors.INDIGO.getIndex());
        }
        estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return estilo;
    }

    private CellStyle crearEstiloTotal(SXSSFWorkbook libro) {
        Font fuente = libro.createFont();
        fuente.setBold(true);
        CellStyle estilo = libro.createCellStyle();
        estilo.setFont(fuente);
        estilo.setBorderTop(BorderStyle.MEDIUM);
        if (estilo instanceof XSSFCellStyle xssfEstilo) {
            XSSFColor morado = new XSSFColor(RGB_MORADO_ARTISYNC, new DefaultIndexedColorMap());
            xssfEstilo.setTopBorderColor(morado);
        }
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
