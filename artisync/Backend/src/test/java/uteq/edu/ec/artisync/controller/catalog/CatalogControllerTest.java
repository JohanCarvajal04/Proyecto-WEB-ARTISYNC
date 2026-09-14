package uteq.edu.ec.artisync.controller.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.response.catalog.OfferingSummaryResponse;
import uteq.edu.ec.artisync.service.catalog.IOfferingCatalogService;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CatalogControllerTest {

    @Mock private IOfferingCatalogService servicioCatalogoServicio;

    @InjectMocks
    private CatalogController controller;

    @Test
    void searchCatalog_devuelveLaPaginaQueDaElServicio() {
        Page<OfferingSummaryResponse> pagina = new PageImpl<>(List.of());
        given(servicioCatalogoServicio.searchCatalogOfferings(
                1L, 2L, BigDecimal.TEN, BigDecimal.valueOf(100), List.of(3L), "retrato",
                "idServicio,desc", 0, 10)).willReturn(pagina);

        ResponseEntity<Page<OfferingSummaryResponse>> respuesta = controller.searchCatalog(
                1L, 2L, BigDecimal.TEN, BigDecimal.valueOf(100), List.of(3L), "retrato",
                "idServicio,desc", 0, 10);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isSameAs(pagina);
    }
}
