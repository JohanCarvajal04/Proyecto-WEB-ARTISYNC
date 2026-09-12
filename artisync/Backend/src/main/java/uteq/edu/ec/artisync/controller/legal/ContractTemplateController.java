package uteq.edu.ec.artisync.controller.legal;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uteq.edu.ec.artisync.dto.respuesta.legal.ContractTemplateSummaryResponse;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.legal.IContractTemplateAdminService;

import java.util.List;

/** Lectura para el creador: elegir una plantilla activa al create/update su servicio. */
@RestController
@RequestMapping("/api/v1/plantillas-contrato")
@RequiredArgsConstructor
public class ContractTemplateController {

    private final IContractTemplateAdminService plantillaContratoAdminServicio;

    /**
     * Lista las plantillas de contrato activas, disponibles para que el creador elija una al create o update su servicio:
     * el catálogo general (ADMIN) más sus propias plantillas privadas (V45).
     *
     * @param userDetails usuario autenticado que consulta
     * @return listado resumido de las plantillas de contrato activas visibles para ese usuario
     */
    @GetMapping("/activas")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ContractTemplateSummaryResponse>> listActive(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.ok(plantillaContratoAdminServicio.listActiveVisibleTo(userDetails.getIdUsuario()));
    }
}
