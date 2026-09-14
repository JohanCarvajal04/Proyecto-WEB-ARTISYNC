package uteq.edu.ec.artisync.controller.legal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.request.legal.EscrowPaymentFilter;
import uteq.edu.ec.artisync.dto.response.legal.EscrowPaymentDetailResponse;
import uteq.edu.ec.artisync.dto.response.legal.EscrowPaymentResponse;
import uteq.edu.ec.artisync.dto.response.legal.EscrowSummaryResponse;
import uteq.edu.ec.artisync.service.legal.IEscrowPaymentAuditService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class EscrowPaymentAuditControllerTest {

    @Mock private IEscrowPaymentAuditService pagoGarantiaAuditoriaServicio;

    @InjectMocks
    private EscrowPaymentAuditController controller;

    @Test
    void list_devuelveLaPaginaQueDaElServicio() {
        EscrowPaymentFilter filtro = new EscrowPaymentFilter();
        Page<EscrowPaymentResponse> pagina = new PageImpl<>(List.of());
        given(pagoGarantiaAuditoriaServicio.list(filtro, PageRequest.of(0, 20))).willReturn(pagina);

        ResponseEntity<Page<EscrowPaymentResponse>> respuesta = controller.list(filtro, PageRequest.of(0, 20));

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isSameAs(pagina);
    }

    @Test
    void getDetail_devuelveElDetalleDelPago() {
        EscrowPaymentDetailResponse detalle = EscrowPaymentDetailResponse.builder().idPago(1L).build();
        given(pagoGarantiaAuditoriaServicio.getDetail(1L)).willReturn(detalle);

        ResponseEntity<EscrowPaymentDetailResponse> respuesta = controller.getDetail(1L);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(detalle);
    }

    @Test
    void getSummary_devuelveElResumenPorEstado() {
        List<EscrowSummaryResponse> resumen = List.of(
                EscrowSummaryResponse.builder().estadoFondos("Retenido").cantidad(3).build());
        given(pagoGarantiaAuditoriaServicio.getSummary()).willReturn(resumen);

        ResponseEntity<List<EscrowSummaryResponse>> respuesta = controller.getSummary();

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(resumen);
    }
}
