package uteq.edu.ec.artisync.service.shared.reporte.impl;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.ReporteDePrueba;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class GeneradorXlsxTest {

    private final GeneradorXlsx generador = new GeneradorXlsx();

    @Test
    @DisplayName("Genera un libro XLSX válido con hojas Datos e Info")
    void generar_LibroValidoConDosHojas() throws IOException {
        DocumentoGenerado documento = generador.generar(ReporteDePrueba.modeloBasico());

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(documento.contenido()))) {
            assertThat(libro.getNumberOfSheets()).isEqualTo(2);
            assertThat(libro.getSheetAt(0).getSheetName()).isEqualTo("Datos");
            assertThat(libro.getSheetAt(1).getSheetName()).isEqualTo("Info");
        }
    }

    @Test
    @DisplayName("La celda de fecha tiene tipo numérico real, no texto")
    void generar_CeldaFechaEsTipoNumerico() throws IOException {
        DocumentoGenerado documento = generador.generar(ReporteDePrueba.modeloBasico());

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(documento.contenido()))) {
            Sheet datos = libro.getSheet("Datos");
            Row primeraFila = datos.getRow(1);
            Cell celdaFecha = primeraFila.getCell(2);
            assertThat(celdaFecha.getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(celdaFecha)).isTrue();
        }
    }

    @Test
    @DisplayName("La celda de monto tiene tipo numérico real, no texto")
    void generar_CeldaMontoEsTipoNumerico() throws IOException {
        DocumentoGenerado documento = generador.generar(ReporteDePrueba.modeloBasico());

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(documento.contenido()))) {
            Sheet datos = libro.getSheet("Datos");
            Cell celdaMonto = datos.getRow(1).getCell(1);
            assertThat(celdaMonto.getCellType()).isEqualTo(CellType.NUMERIC);
            assertThat(celdaMonto.getNumericCellValue()).isEqualTo(1234.5);
        }
    }

    @Test
    @DisplayName("Escribe la fila de totales debajo de los datos")
    void generar_EscribeTotales() throws IOException {
        DocumentoGenerado documento = generador.generar(ReporteDePrueba.modeloBasico());

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(documento.contenido()))) {
            Sheet datos = libro.getSheet("Datos");
            boolean encontrado = false;
            for (Row fila : datos) {
                Cell primera = fila.getCell(0);
                if (primera != null && primera.getCellType() == CellType.STRING
                        && "Monto total".equals(primera.getStringCellValue())) {
                    encontrado = true;
                }
            }
            assertThat(encontrado).isTrue();
        }
    }

    @Test
    @DisplayName("La hoja Info incluye el título y los filtros aplicados")
    void generar_HojaInfoConMetadatos() throws IOException {
        DocumentoGenerado documento = generador.generar(ReporteDePrueba.modeloBasico());

        try (XSSFWorkbook libro = new XSSFWorkbook(new ByteArrayInputStream(documento.contenido()))) {
            Sheet info = libro.getSheet("Info");
            StringBuilder contenido = new StringBuilder();
            for (Row fila : info) {
                for (Cell celda : fila) {
                    if (celda.getCellType() == CellType.STRING) {
                        contenido.append(celda.getStringCellValue()).append('|');
                    }
                }
            }
            assertThat(contenido.toString()).contains("Reporte de Prueba").contains("Desde");
        }
    }

    @Test
    @DisplayName("Content-Type es el de spreadsheetml")
    void generar_ContentTypeCorrecto() {
        DocumentoGenerado documento = generador.generar(ReporteDePrueba.modeloBasico());

        assertThat(documento.contentType())
                .isEqualTo("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
    }
}
