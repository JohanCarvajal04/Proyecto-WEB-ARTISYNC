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
import uteq.edu.ec.artisync.dto.response.catalog.OfferingResponse;
import uteq.edu.ec.artisync.dto.response.catalog.OfferingSummaryResponse;
import uteq.edu.ec.artisync.service.catalog.IOfferingCatalogService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class OfferingModerationControllerTest {

    @Mock private IOfferingCatalogService servicioCatalogoServicio;

    @InjectMocks
    private OfferingModerationController controller;

    @Test
    void listForModeration_devuelveLaPaginaQueDaElServicio() {
        Page<OfferingSummaryResponse> pagina = new PageImpl<>(List.of());
        given(servicioCatalogoServicio.listForModeration("retrato", 0, 20)).willReturn(pagina);

        ResponseEntity<Page<OfferingSummaryResponse>> respuesta =
                controller.listForModeration("retrato", 0, 20);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isSameAs(pagina);
    }

    @Test
    void removeSubcategory_devuelveElServicioActualizado() {
        OfferingResponse actualizado = OfferingResponse.builder().idServicio(1L).build();
        given(servicioCatalogoServicio.removeSubcategory(1L, 5L)).willReturn(actualizado);

        ResponseEntity<OfferingResponse> respuesta = controller.removeSubcategory(1L, 5L);

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(actualizado);
    }
}
