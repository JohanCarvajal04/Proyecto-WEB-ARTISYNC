package uteq.edu.ec.artisync.service.legal.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import uteq.edu.ec.artisync.dto.peticion.legal.FiltroReporteFinanciero;
import uteq.edu.ec.artisync.dto.respuesta.legal.DetalleComision;
import uteq.edu.ec.artisync.dto.respuesta.legal.RespuestaReporteComisiones;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.legal.TransaccionPagoRepository;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;
import uteq.edu.ec.artisync.service.shared.reporte.IServicioExportacion;
import uteq.edu.ec.artisync.service.shared.reporte.ModeloReporte;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReporteFinancieroServicioImplTest {

    private static final String JSON_REPORTE = """
            {
              "idPerfil": 7,
              "fechaDesde": "2026-01-01T00:00:00",
              "fechaHasta": "2026-01-31T23:59:59",
              "tasaComision": 0.1000,
              "totalPedidos": 2,
              "totalOperaciones": 3,
              "montoBruto": 300.00,
              "comision": 30.00,
              "montoNeto": 270.00,
              "detalle": [
                {"idTransaccion": 1, "idPedido": 10, "servicio": "Logo", "tipo": "Ingreso",
                 "monto": 100.00, "fechaEjecucion": "2026-01-05T10:00:00"},
                {"idTransaccion": 2, "idPedido": null, "servicio": null, "tipo": "Comision",
                 "monto": 200.00, "fechaEjecucion": "2026-01-10T09:30:00"}
              ]
            }
            """;

    @Mock
    private TransaccionPagoRepository transaccionPagoRepository;

    @Mock
    private IServicioExportacion servicioExportacion;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ReporteFinancieroServicioImpl servicio;

    private ReporteFinancieroServicioImpl crearServicio() {
        return new ReporteFinancieroServicioImpl(transaccionPagoRepository, servicioExportacion, objectMapper);
    }

    @Test
    @DisplayName("obtenerReporteComisiones() parsea el JSONB de fn_reporte_comisiones_creador")
    void obtenerReporteComisiones_ParseaJson() {
        when(transaccionPagoRepository.reporteComisionesJson(eq(7L), any(), any(), any())).thenReturn(JSON_REPORTE);
        servicio = crearServicio();

        FiltroReporteFinanciero filtro = new FiltroReporteFinanciero();
        filtro.setIdPerfil(7L);
        RespuestaReporteComisiones reporte = servicio.obtenerReporteComisiones(filtro);

        assertThat(reporte.idPerfil()).isEqualTo(7L);
        assertThat(reporte.montoBruto()).isEqualByComparingTo("300.00");
        assertThat(reporte.comision()).isEqualByComparingTo("30.00");
        assertThat(reporte.montoNeto()).isEqualByComparingTo("270.00");
        assertThat(reporte.detalle()).hasSize(2);
        assertThat(reporte.detalle().get(0).fechaEjecucion()).isEqualTo(LocalDateTime.of(2026, 1, 5, 10, 0, 0));
    }

    @Test
    @DisplayName("obtenerReporteComisiones() tolera idPedido/servicio nulos en el detalle (transacciones internas sin pedido asociado)")
    void obtenerReporteComisiones_ToleraCamposNulos() {
        when(transaccionPagoRepository.reporteComisionesJson(eq(7L), any(), any(), any())).thenReturn(JSON_REPORTE);
        servicio = crearServicio();

        FiltroReporteFinanciero filtro = new FiltroReporteFinanciero();
        filtro.setIdPerfil(7L);
        RespuestaReporteComisiones reporte = servicio.obtenerReporteComisiones(filtro);

        DetalleComision segunda = reporte.detalle().get(1);
        assertThat(segunda.idPedido()).isNull();
        assertThat(segunda.servicio()).isNull();
        assertThat(segunda.monto()).isEqualByComparingTo("200.00");
    }

    @Test
    @DisplayName("obtenerReporteComisiones() usa plataforma.comision-tasa cuando el filtro no trae una tasa explicita")
    void obtenerReporteComisiones_SinTasaEnFiltro_UsaLaConfigurada() {
        when(transaccionPagoRepository.reporteComisionesJson(eq(7L), any(), any(), any())).thenReturn(JSON_REPORTE);
        servicio = crearServicio();
        ReflectionTestUtils.setField(servicio, "tasaComisionPorDefecto", new BigDecimal("0.15"));

        FiltroReporteFinanciero filtro = new FiltroReporteFinanciero();
        filtro.setIdPerfil(7L);
        servicio.obtenerReporteComisiones(filtro);

        ArgumentCaptor<BigDecimal> tasaCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(transaccionPagoRepository).reporteComisionesJson(eq(7L), any(), any(), tasaCaptor.capture());
        assertThat(tasaCaptor.getValue()).isEqualByComparingTo("0.15");
    }

    @Test
    @DisplayName("obtenerReporteComisiones() respeta la tasa explicita del filtro por encima del default")
    void obtenerReporteComisiones_ConTasaEnFiltro_IgnoraElDefault() {
        when(transaccionPagoRepository.reporteComisionesJson(eq(7L), any(), any(), any())).thenReturn(JSON_REPORTE);
        servicio = crearServicio();
        ReflectionTestUtils.setField(servicio, "tasaComisionPorDefecto", new BigDecimal("0.15"));

        FiltroReporteFinanciero filtro = new FiltroReporteFinanciero();
        filtro.setIdPerfil(7L);
        filtro.setTasaComision(new BigDecimal("0.25"));
        servicio.obtenerReporteComisiones(filtro);

        ArgumentCaptor<BigDecimal> tasaCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(transaccionPagoRepository).reporteComisionesJson(eq(7L), any(), any(), tasaCaptor.capture());
        assertThat(tasaCaptor.getValue()).isEqualByComparingTo("0.25");
    }

    @Test
    @DisplayName("exportar() lanza ExcepcionReglaNegocio si el detalle supera el tope de filas del formato")
    void exportar_ExcedeTope_LanzaExcepcion() {
        StringBuilder detalle = new StringBuilder();
        for (int i = 0; i < FormatoReporte.PDF.topeFilas() + 1; i++) {
            if (i > 0) {
                detalle.append(',');
            }
            detalle.append("{\"idTransaccion\": ").append(i)
                    .append(", \"idPedido\": 1, \"servicio\": \"S\", \"tipo\": \"Ingreso\", \"monto\": 1.00, ")
                    .append("\"fechaEjecucion\": \"2026-01-01T00:00:00\"}");
        }
        String jsonEnorme = "{\"idPerfil\":7,\"fechaDesde\":null,\"fechaHasta\":null,\"tasaComision\":0.10,"
                + "\"totalPedidos\":1,\"totalOperaciones\":" + (FormatoReporte.PDF.topeFilas() + 1)
                + ",\"montoBruto\":1,\"comision\":0.1,\"montoNeto\":0.9,\"detalle\":[" + detalle + "]}";
        when(transaccionPagoRepository.reporteComisionesJson(eq(7L), any(), any(), any())).thenReturn(jsonEnorme);
        servicio = crearServicio();

        FiltroReporteFinanciero filtro = new FiltroReporteFinanciero();
        filtro.setIdPerfil(7L);

        assertThatThrownBy(() -> servicio.exportar(filtro, FormatoReporte.PDF, "admin@artisync.dev"))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining(String.valueOf(FormatoReporte.PDF.topeFilas()));
    }

    @Test
    @DisplayName("exportar() construye el ModeloReporte con totales bruto/comisión/neto y delega en el común")
    void exportar_ConstruyeModeloConTotalesYDelega() {
        when(transaccionPagoRepository.reporteComisionesJson(eq(7L), any(), any(), any())).thenReturn(JSON_REPORTE);
        servicio = crearServicio();
        DocumentoGenerado esperado = new DocumentoGenerado(new byte[]{1}, "text/csv", "comisiones.csv");
        when(servicioExportacion.exportar(any(ModeloReporte.class), eq(FormatoReporte.CSV))).thenReturn(esperado);

        FiltroReporteFinanciero filtro = new FiltroReporteFinanciero();
        filtro.setIdPerfil(7L);
        DocumentoGenerado resultado = servicio.exportar(filtro, FormatoReporte.CSV, "admin@artisync.dev");

        assertThat(resultado).isSameAs(esperado);
        ArgumentCaptor<ModeloReporte> captor = ArgumentCaptor.forClass(ModeloReporte.class);
        verify(servicioExportacion).exportar(captor.capture(), eq(FormatoReporte.CSV));
        ModeloReporte<DetalleComision> modelo = captor.getValue();
        assertThat(modelo.getFilas()).hasSize(2);
        assertThat(modelo.getTotales()).hasSize(3);
        assertThat((BigDecimal) modelo.getTotales().get(0).valor()).isEqualByComparingTo("300.00");
        assertThat(modelo.getGeneradoPor()).isEqualTo("admin@artisync.dev");
    }

    @Test
    @DisplayName("exportar() con page y size pagina el detalle sin exceder el tope")
    void exportar_conPaginacion_permiteExportarPorLotes() {
        when(transaccionPagoRepository.reporteComisionesJson(eq(7L), any(), any(), any())).thenReturn(JSON_REPORTE);
        servicio = crearServicio();
        DocumentoGenerado esperado = new DocumentoGenerado(new byte[]{1}, "application/pdf", "comisiones_7_parte_1.pdf");
        when(servicioExportacion.exportar(any(ModeloReporte.class), eq(FormatoReporte.PDF))).thenReturn(esperado);

        FiltroReporteFinanciero filtro = new FiltroReporteFinanciero();
        filtro.setIdPerfil(7L);
        DocumentoGenerado resultado = servicio.exportar(filtro, FormatoReporte.PDF, 0, 1, "admin@artisync.dev");

        assertThat(resultado).isSameAs(esperado);
        ArgumentCaptor<ModeloReporte> captor = ArgumentCaptor.forClass(ModeloReporte.class);
        verify(servicioExportacion).exportar(captor.capture(), eq(FormatoReporte.PDF));
        ModeloReporte<DetalleComision> modelo = captor.getValue();
        assertThat(modelo.getFilas()).hasSize(1);
        assertThat(modelo.getTitulo()).isEqualTo("Comisiones - Parte 1");
        assertThat(modelo.getSubtitulo()).contains("Parte 1 de 2");
    }
}
