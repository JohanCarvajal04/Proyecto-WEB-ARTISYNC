package uteq.edu.ec.artisync.controller.audit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import uteq.edu.ec.artisync.dto.request.audit.AuditFilter;
import uteq.edu.ec.artisync.dto.response.audit.AuditEventResponse;
import uteq.edu.ec.artisync.dto.response.audit.AuditEventSummaryResponse;
import uteq.edu.ec.artisync.service.audit.IAuditService;
import uteq.edu.ec.artisync.service.shared.report.GeneratedDocument;
import uteq.edu.ec.artisync.service.shared.report.ReportFormat;
import uteq.edu.ec.artisync.util.PagedResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

    @Mock private IAuditService auditoriaServicio;
    @Mock private Authentication authentication;

    @InjectMocks
    private AuditController controller;

    @Test
    void list_devuelveLaPaginaQueDaElServicio() {
        AuditFilter filtro = new AuditFilter();
        PagedResponse<AuditEventSummaryResponse> pagina =
                new PagedResponse<>(List.of(), 0, 20, 0, 0, true);
        given(auditoriaServicio.list(any(), any())).willReturn(pagina);

        ResponseEntity<PagedResponse<AuditEventSummaryResponse>> respuesta =
                controller.list(filtro, PageRequest.of(0, 20));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isSameAs(pagina);
    }

    @Test
    void getById_devuelveElDetalleDelEvento() {
        AuditEventResponse detalle = AuditEventResponse.builder().idEventoAuditoria(1L).build();
        given(auditoriaServicio.getById(1L)).willReturn(detalle);

        ResponseEntity<AuditEventResponse> respuesta = controller.getById(1L);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(detalle);
    }

    @Test
    void listActions_devuelveElCatalogoDeAcciones() {
        given(auditoriaServicio.listAvailableActions()).willReturn(List.of("USUARIO_CREAR", "USUARIO_EXPORTAR"));

        ResponseEntity<List<String>> respuesta = controller.listActions();

        assertThat(respuesta.getBody()).containsExactly("USUARIO_CREAR", "USUARIO_EXPORTAR");
    }

    @Test
    void export_sinPageNiSize_llamaAlExportSinPaginar() {
        AuditFilter filtro = new AuditFilter();
        GeneratedDocument documento = new GeneratedDocument("csv".getBytes(), "text/csv", "auditoria.csv");
        given(authentication.getName()).willReturn("admin@artisync.com");
        given(auditoriaServicio.export(filtro, ReportFormat.CSV, "admin@artisync.com")).willReturn(documento);

        ResponseEntity<byte[]> respuesta = controller.export(filtro, ReportFormat.CSV, null, null, authentication);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(auditoriaServicio, org.mockito.Mockito.never()).export(any(), any(), any(Integer.class), any(), any());
    }

    @Test
    void export_conPage_llamaAlExportPaginado() {
        AuditFilter filtro = new AuditFilter();
        GeneratedDocument documento = new GeneratedDocument("csv".getBytes(), "text/csv", "auditoria.csv");
        given(authentication.getName()).willReturn("admin@artisync.com");
        given(auditoriaServicio.export(eq(filtro), eq(ReportFormat.CSV), eq(0), eq(50), eq("admin@artisync.com")))
                .willReturn(documento);

        ResponseEntity<byte[]> respuesta = controller.export(filtro, ReportFormat.CSV, 0, 50, authentication);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void export_conSizeSinPage_tambienLlamaAlExportPaginado() {
        AuditFilter filtro = new AuditFilter();
        GeneratedDocument documento = new GeneratedDocument("csv".getBytes(), "text/csv", "auditoria.csv");
        given(authentication.getName()).willReturn("admin@artisync.com");
        given(auditoriaServicio.export(eq(filtro), eq(ReportFormat.CSV), isNull(), eq(50), eq("admin@artisync.com")))
                .willReturn(documento);

        ResponseEntity<byte[]> respuesta = controller.export(filtro, ReportFormat.CSV, null, 50, authentication);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void export_sobrecargaDeConveniencia_delegaEnLaVersionSinPaginar() {
        AuditFilter filtro = new AuditFilter();
        GeneratedDocument documento = new GeneratedDocument("pdf".getBytes(), "application/pdf", "auditoria.pdf");
        given(authentication.getName()).willReturn("admin@artisync.com");
        given(auditoriaServicio.export(filtro, ReportFormat.PDF, "admin@artisync.com")).willReturn(documento);

        ResponseEntity<byte[]> respuesta = controller.export(filtro, ReportFormat.PDF, authentication);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
    }
}
