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
import uteq.edu.ec.artisync.dto.peticion.legal.FiltroReporteContrato;
import uteq.edu.ec.artisync.dto.respuesta.legal.FilaReporteContrato;
import uteq.edu.ec.artisync.exception.ExcepcionReglaNegocio;
import uteq.edu.ec.artisync.repository.legal.ContratoRepository;
import uteq.edu.ec.artisync.service.shared.reporte.DocumentoGenerado;
import uteq.edu.ec.artisync.service.shared.reporte.FormatoReporte;
import uteq.edu.ec.artisync.service.shared.reporte.IServicioExportacion;
import uteq.edu.ec.artisync.service.shared.reporte.ModeloReporte;
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
class ReporteContratoServicioImplTest {

    @Mock
    private ContratoRepository contratoRepository;

    @Mock
    private IServicioExportacion servicioExportacion;

    @InjectMocks
    private ReporteContratoServicioImpl reporteContratoServicio;

    private FilaReporteContrato filaDe(Long idContrato, BigDecimal precio) {
        return new FilaReporteContrato(idContrato, 100L, "Ilustracion", "Cliente X", "Creador Y",
                precio, 2, LocalDateTime.now(), true, true);
    }

    @Test
    @DisplayName("listar delega en el repositorio y mapea la pagina a PagedResponse")
    void listar_delegaYMapea() {
        FilaReporteContrato fila = filaDe(1L, new BigDecimal("100.00"));
        Page<FilaReporteContrato> pagina = new PageImpl<>(List.of(fila));
        given(contratoRepository.buscarParaReporte(any(), any(), any(), any(), any(Pageable.class)))
                .willReturn(pagina);

        PagedResponse<FilaReporteContrato> resultado = reporteContratoServicio.listar(new FiltroReporteContrato(), 0, 20);

        assertThat(resultado.getContent()).containsExactly(fila);
    }

    @Test
    @DisplayName("exportar lanza ExcepcionReglaNegocio cuando el filtro supera el tope de filas del formato")
    void exportar_excedeTope_lanzaExcepcion() {
        Page<FilaReporteContrato> paginaEnorme = new PageImpl<>(
                List.of(filaDe(1L, BigDecimal.TEN)), PageRequest.of(0, FormatoReporte.CSV.topeFilas()), 50_001);
        given(contratoRepository.buscarParaReporte(any(), any(), any(), any(), any(Pageable.class)))
                .willReturn(paginaEnorme);

        assertThatThrownBy(() -> reporteContratoServicio.exportar(new FiltroReporteContrato(), FormatoReporte.CSV, "admin@artisync.dev"))
                .isInstanceOf(ExcepcionReglaNegocio.class)
                .hasMessageContaining("50001")
                .hasMessageContaining("Acote el rango de fechas");
    }

    @Test
    @DisplayName("exportar suma el precio pactado de las filas y construye el modelo con todos los filtros")
    void exportar_construyeModeloConTotalesYFiltrosCompletos() {
        FilaReporteContrato fila1 = filaDe(1L, new BigDecimal("100.00"));
        FilaReporteContrato fila2 = filaDe(2L, null);
        Page<FilaReporteContrato> pagina = new PageImpl<>(List.of(fila1, fila2));
        given(contratoRepository.buscarParaReporte(any(), any(), any(), any(), any(Pageable.class)))
                .willReturn(pagina);
        DocumentoGenerado esperado = new DocumentoGenerado(new byte[]{1}, "text/csv", "contratos.csv");
        given(servicioExportacion.exportar(any(ModeloReporte.class), eq(FormatoReporte.CSV))).willReturn(esperado);

        FiltroReporteContrato filtro = new FiltroReporteContrato();
        filtro.setDesde(LocalDateTime.of(2026, 1, 1, 0, 0));
        filtro.setHasta(LocalDateTime.of(2026, 1, 31, 23, 59));
        filtro.setIdPerfilCreador(7L);
        filtro.setSoloFirmados(true);

        DocumentoGenerado resultado = reporteContratoServicio.exportar(filtro, FormatoReporte.CSV, "admin@artisync.dev");

        assertThat(resultado).isSameAs(esperado);
        ArgumentCaptor<ModeloReporte> captor = ArgumentCaptor.forClass(ModeloReporte.class);
        verify(servicioExportacion).exportar(captor.capture(), eq(FormatoReporte.CSV));
        ModeloReporte<FilaReporteContrato> modelo = captor.getValue();
        assertThat(modelo.getTotales()).hasSize(1);
        assertThat((BigDecimal) modelo.getTotales().get(0).valor()).isEqualByComparingTo("100.00");
        assertThat(modelo.getFiltrosAplicados())
                .containsEntry("Id. perfil del creador", "7")
                .containsEntry("Solo firmados", "Sí")
                .containsKey("Desde")
                .containsKey("Hasta");
    }

    @Test
    @DisplayName("exportar reporta 'No' cuando soloFirmados es false y omite los filtros no informados")
    void exportar_soloFirmadosFalse_yFiltrosVacios() {
        Page<FilaReporteContrato> pagina = new PageImpl<>(List.of(filaDe(1L, BigDecimal.ONE)));
        given(contratoRepository.buscarParaReporte(any(), any(), any(), any(), any(Pageable.class)))
                .willReturn(pagina);
        given(servicioExportacion.exportar(any(ModeloReporte.class), eq(FormatoReporte.CSV)))
                .willReturn(new DocumentoGenerado(new byte[]{1}, "text/csv", "contratos.csv"));

        FiltroReporteContrato filtro = new FiltroReporteContrato();
        filtro.setSoloFirmados(false);

        reporteContratoServicio.exportar(filtro, FormatoReporte.CSV, "admin@artisync.dev");

        ArgumentCaptor<ModeloReporte> captor = ArgumentCaptor.forClass(ModeloReporte.class);
        verify(servicioExportacion).exportar(captor.capture(), eq(FormatoReporte.CSV));
        assertThat(captor.getValue().getFiltrosAplicados()).isEqualTo(Map.of("Solo firmados", "No"));
    }
}
