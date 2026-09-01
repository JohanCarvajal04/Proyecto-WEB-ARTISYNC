package uteq.edu.ec.artisync.service.shared.reporte;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Fila de prueba compartida por los tests de los tres generadores. */
public record ReporteDePrueba(String nombre, BigDecimal monto, LocalDateTime fecha, Long id) {

    public static ModeloReporte<ReporteDePrueba> modeloBasico() {
        return ModeloReporte.<ReporteDePrueba>builder()
                .titulo("Reporte de Prueba")
                .subtitulo("Subtítulo")
                .filtrosAplicados(java.util.Map.of("Desde", "2026-01-01"))
                .columnas(java.util.List.of(
                        ColumnaReporte.texto("Nombre", ReporteDePrueba::nombre),
                        ColumnaReporte.moneda("Monto", ReporteDePrueba::monto),
                        ColumnaReporte.fechaHora("Fecha", ReporteDePrueba::fecha),
                        ColumnaReporte.entero("Id", ReporteDePrueba::id)))
                .filas(java.util.List.of(
                        new ReporteDePrueba("Juan Pérez", new BigDecimal("1234.5"),
                                LocalDateTime.of(2026, 1, 15, 10, 30, 0), 1L),
                        new ReporteDePrueba("Ana, \"la\" jefa\ncon salto", new BigDecimal("999.999"),
                                LocalDateTime.of(2026, 2, 1, 8, 0, 0), 2L)))
                .totales(java.util.List.of(
                        new TotalReporte("Monto total", new BigDecimal("2234.499"), TipoColumna.MONEDA)))
                .generadoPor("admin@artisync.dev")
                .generadoEn(LocalDateTime.of(2026, 8, 24, 12, 0, 0))
                .build();
    }
}
