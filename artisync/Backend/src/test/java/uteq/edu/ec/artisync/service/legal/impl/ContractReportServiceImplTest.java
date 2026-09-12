package uteq.edu.ec.artisync.service.legal.impl;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import uteq.edu.ec.artisync.dto.peticion.legal.ContractReportFilter;
import uteq.edu.ec.artisync.dto.respuesta.legal.ContractReportRow;
import uteq.edu.ec.artisync.exception.BusinessRuleException;
import uteq.edu.ec.artisync.repository.legal.ContractRepository;
import uteq.edu.ec.artisync.service.shared.reporte.GeneratedDocument;
import uteq.edu.ec.artisync.service.shared.reporte.ReportFormat;
import uteq.edu.ec.artisync.service.shared.reporte.IExportService;
import uteq.edu.ec.artisync.service.shared.reporte.ReportModel;
import uteq.edu.ec.artisync.util.PagedResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ContractReportServiceImplTest {

    @Mock
    private ContractRepository contratoRepository;

    @Mock
    private IExportService servicioExportacion;

    @InjectMocks
    private ContractReportServiceImpl reporteContratoServicio;

    private ContractReportRow filaDe(Long idContrato, BigDecimal precio) {
        return new ContractReportRow(idContrato, 100L, "Ilustracion", "Cliente X", "Creador Y",
                precio, 2, LocalDateTime.now(), true, true);
    }

    @Test
    @DisplayName("list delega en el repositorio y mapea la pagina a PagedResponse")
    void listar_delegaYMapea() {
        ContractReportRow fila = filaDe(1L, new BigDecimal("100.00"));
        Page<ContractReportRow> pagina = new PageImpl<>(List.of(fila));
        given(contratoRepository.buscarParaReporte(any(), any(), any(), any(), any(Pageable.class)))
                .willReturn(pagina);

        PagedResponse<ContractReportRow> resultado = reporteContratoServicio.list(new ContractReportFilter(), 0, 20);

        assertThat(resultado.getContent()).containsExactly(fila);
    }

    @Test
    @DisplayName("export lanza BusinessRuleException cuando el filtro supera el tope de filas del formato")
    void exportar_excedeTope_lanzaExcepcion() {
        Page<ContractReportRow> paginaEnorme = new PageImpl<>(
                List.of(filaDe(1L, BigDecimal.TEN)), PageRequest.of(0, ReportFormat.CSV.topeFilas()), 50_001);
        given(contratoRepository.buscarParaReporte(any(), any(), any(), any(), any(Pageable.class)))
                .willReturn(paginaEnorme);

        assertThatThrownBy(() -> reporteContratoServicio.export(new ContractReportFilter(), ReportFormat.CSV, "admin@artisync.dev"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("50001")
                .hasMessageContaining("Acote el rango de fechas");
    }

    @Test
    @DisplayName("export suma el precio pactado de las filas y construye el modelo con todos los filtros")
    void exportar_construyeModeloConTotalesYFiltrosCompletos() {
        ContractReportRow fila1 = filaDe(1L, new BigDecimal("100.00"));
        ContractReportRow fila2 = filaDe(2L, null);
        Page<ContractReportRow> pagina = new PageImpl<>(List.of(fila1, fila2));
        given(contratoRepository.buscarParaReporte(any(), any(), any(), any(), any(Pageable.class)))
                .willReturn(pagina);
        GeneratedDocument esperado = new GeneratedDocument(new byte[]{1}, "text/csv", "contratos.csv");
        given(servicioExportacion.exportar(any(ReportModel.class), eq(ReportFormat.CSV))).willReturn(esperado);

        ContractReportFilter filtro = new ContractReportFilter();
        filtro.setDesde(LocalDateTime.of(2026, 1, 1, 0, 0));
        filtro.setHasta(LocalDateTime.of(2026, 1, 31, 23, 59));
        filtro.setIdPerfilCreador(7L);
        filtro.setSoloFirmados(true);

        GeneratedDocument resultado = reporteContratoServicio.export(filtro, ReportFormat.CSV, "admin@artisync.dev");

        assertThat(resultado).isSameAs(esperado);
        ArgumentCaptor<ReportModel> captor = ArgumentCaptor.forClass(ReportModel.class);
        verify(servicioExportacion).exportar(captor.capture(), eq(ReportFormat.CSV));
        ReportModel<ContractReportRow> modelo = captor.getValue();
        assertThat(modelo.getTotales()).hasSize(1);
        assertThat((BigDecimal) modelo.getTotales().get(0).valor()).isEqualByComparingTo("100.00");
        assertThat(modelo.getFiltrosAplicados())
                .containsEntry("Id. perfil del creador", "7")
                .containsEntry("Solo firmados", "Sí")
                .containsKey("Desde")
                .containsKey("Hasta");
    }

    @Test
    @DisplayName("export reporta 'No' cuando soloFirmados es false y omite los filtros no informados")
    void exportar_soloFirmadosFalse_yFiltrosVacios() {
        Page<ContractReportRow> pagina = new PageImpl<>(List.of(filaDe(1L, BigDecimal.ONE)));
        given(contratoRepository.buscarParaReporte(any(), any(), any(), any(), any(Pageable.class)))
                .willReturn(pagina);
        given(servicioExportacion.exportar(any(ReportModel.class), eq(ReportFormat.CSV)))
                .willReturn(new GeneratedDocument(new byte[]{1}, "text/csv", "contratos.csv"));

        ContractReportFilter filtro = new ContractReportFilter();
        filtro.setSoloFirmados(false);

        reporteContratoServicio.export(filtro, ReportFormat.CSV, "admin@artisync.dev");

        ArgumentCaptor<ReportModel> captor = ArgumentCaptor.forClass(ReportModel.class);
        verify(servicioExportacion).exportar(captor.capture(), eq(ReportFormat.CSV));
        assertThat(captor.getValue().getFiltrosAplicados()).isEqualTo(Map.of("Solo firmados", "No"));
    }

    @Test
    @DisplayName("export con page y size permite export por lotes sin exceder el tope")
    void exportar_conPaginacion_permiteExportarPorLotes() {
        ContractReportRow fila1 = filaDe(1L, new BigDecimal("50.00"));
        Page<ContractReportRow> paginaParte = new PageImpl<>(
                List.of(fila1), PageRequest.of(0, 5000), 80_000);
        given(contratoRepository.buscarParaReporte(any(), any(), any(), any(), any(Pageable.class)))
                .willReturn(paginaParte);
        GeneratedDocument esperado = new GeneratedDocument(new byte[]{1, 2}, "application/pdf", "contratos_parte_1.pdf");
        given(servicioExportacion.exportar(any(ReportModel.class), eq(ReportFormat.PDF))).willReturn(esperado);

        GeneratedDocument resultado = reporteContratoServicio.export(
                new ContractReportFilter(), ReportFormat.PDF, 0, 5000, "admin@artisync.dev");

        assertThat(resultado).isSameAs(esperado);
        ArgumentCaptor<ReportModel> captor = ArgumentCaptor.forClass(ReportModel.class);
        verify(servicioExportacion).exportar(captor.capture(), eq(ReportFormat.PDF));
        assertThat(captor.getValue().getTitulo()).isEqualTo("Contratos - Parte 1");
        assertThat(captor.getValue().getSubtitulo()).contains("Parte 1 de 16");
    }
}
