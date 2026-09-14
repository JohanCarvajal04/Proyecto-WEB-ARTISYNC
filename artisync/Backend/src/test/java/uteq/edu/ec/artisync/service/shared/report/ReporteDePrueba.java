package uteq.edu.ec.artisync.service.shared.report;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fila de prueba compartida por los tests de los tres generadores. */
public record ReporteDePrueba(String nombre, BigDecimal monto, LocalDateTime fecha, Long id) {

    public static ReportModel<ReporteDePrueba> modeloBasico() {
        return ReportModel.<ReporteDePrueba>builder()
                .titulo("Reporte de Prueba")
                .subtitulo("Subtítulo")
                .filtrosAplicados(java.util.Map.of("Desde", "2026-01-01"))
                .columnas(java.util.List.of(
                        ReportColumn.texto("Nombre", ReporteDePrueba::nombre),
                        ReportColumn.moneda("Monto", ReporteDePrueba::monto),
                        ReportColumn.dateTime("Fecha", ReporteDePrueba::fecha),
                        ReportColumn.entero("Id", ReporteDePrueba::id)))
                .filas(java.util.List.of(
                        new ReporteDePrueba("Juan Pérez", new BigDecimal("1234.5"),
                                LocalDateTime.of(2026, 1, 15, 10, 30, 0), 1L),
                        new ReporteDePrueba("Ana, \"la\" jefa\ncon salto", new BigDecimal("999.999"),
                                LocalDateTime.of(2026, 2, 1, 8, 0, 0), 2L)))
                .totales(java.util.List.of(
                        new ReportTotal("Monto total", new BigDecimal("2234.499"), ColumnType.MONEDA)))
                .generadoPor("admin@artisync.dev")
                .generadoEn(LocalDateTime.of(2026, 8, 24, 12, 0, 0))
                .build();
    }

    /**
     * Modelo con KPIs y gráficas, para ejercitar la hoja "Resumen" de XLSX y las
     * secciones equivalentes de PDF: un KPI con descripción y otro sin ella, una
     * gráfica con datos e imagen, y otra sin datos ni imagen (ambas ramas de
     * "si hay contenido que dibujar").
     */
    public static ReportModel<ReporteDePrueba> modeloConGraficasYKpis() {
        return ReportModel.<ReporteDePrueba>builder()
                .titulo("Reporte con gráficas")
                .filtrosAplicados(java.util.Map.of())
                .columnas(java.util.List.of(
                        ReportColumn.texto("Nombre", ReporteDePrueba::nombre)))
                .filas(java.util.List.of(
                        new ReporteDePrueba("Juan Pérez", new BigDecimal("100"),
                                LocalDateTime.of(2026, 1, 15, 10, 30, 0), 1L)))
                .kpis(java.util.List.of(
                        new ReportKpi("Total de pedidos", "42", "Últimos 30 días"),
                        new ReportKpi("Tasa de conversión", "12%", null)))
                .graficas(java.util.List.of(
                        new ReportChart("Distribución por estado", null,
                                new byte[]{(byte) 0x89, 'P', 'N', 'G'},
                                new java.util.LinkedHashMap<>(java.util.Map.of("Activo", 8L, "Cerrado", 2L))),
                        new ReportChart("Gráfica sin datos ni imagen", null, new byte[0], java.util.Map.of())))
                .generadoPor("admin@artisync.dev")
                .generadoEn(LocalDateTime.of(2026, 8, 24, 12, 0, 0))
                .build();
    }
}
