package uteq.edu.ec.artisync.controller.catalog;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uteq.edu.ec.artisync.dto.response.catalog.OfferingSummaryResponse;
import uteq.edu.ec.artisync.service.catalog.IOfferingCatalogService;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class CreatorOfferingControllerTest {

    @Mock private IOfferingCatalogService servicioCatalogoServicio;

    @InjectMocks
    private CreatorOfferingController controller;

    @Test
    void listOfferingsByCreator_devuelveElListadoDelServicio() {
        List<OfferingSummaryResponse> listado = List.of(OfferingSummaryResponse.builder().idServicio(1L).build());
        given(servicioCatalogoServicio.listOfferingsByCreator(7L, "ACTIVO")).willReturn(listado);

        ResponseEntity<List<OfferingSummaryResponse>> respuesta =
                controller.listOfferingsByCreator(7L, "ACTIVO");

        assertThat(respuesta.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(respuesta.getBody()).isEqualTo(listado);
    }
}
