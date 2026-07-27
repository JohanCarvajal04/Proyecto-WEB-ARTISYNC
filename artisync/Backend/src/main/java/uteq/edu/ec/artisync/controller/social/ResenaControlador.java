package uteq.edu.ec.artisync.controller.social;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import uteq.edu.ec.artisync.dto.peticion.social.PeticionCrearResena;
import uteq.edu.ec.artisync.dto.respuesta.social.RespuestaResena;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.social.ResenaService;

import java.util.List;
import java.util.Map;

/**
 * Controlador de reseñas y calificaciones de servicios.
 * RF-09: Calificaciones 1-5 estrellas, solo el cliente post-entrega del entregable.
 */
@Tag(name = "Reseñas", description = "Reseñas y calificaciones de servicios 1-5 estrellas")
@RestController
@RequiredArgsConstructor
public class ResenaControlador {

    private final ResenaService resenaService;

    @Operation(summary = "Crear reseña de un pedido entregado (CLIENTE)")
    @PostMapping("/api/v1/pedidos/{idPedido}/resena")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<RespuestaResena> crearResena(
            @PathVariable Long idPedido,
            @Valid @RequestBody PeticionCrearResena peticion,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(resenaService.crearResena(idPedido, peticion, userDetails.getIdUsuario()));
    }

    @Operation(summary = "Listar reseñas de un creador (público)")
    @GetMapping("/api/v1/creadores/{idPerfil}/resenas")
    public ResponseEntity<List<RespuestaResena>> listarResenas(@PathVariable Long idPerfil) {
        return ResponseEntity.ok(resenaService.listarResenasPorCreador(idPerfil));
    }

    @Operation(summary = "Promedio de calificaciones de un creador (público)")
    @GetMapping("/api/v1/creadores/{idPerfil}/resenas/promedio")
    public ResponseEntity<Map<String, Object>> obtenerPromedio(@PathVariable Long idPerfil) {
        Double promedio = resenaService.calcularPromedioPorCreador(idPerfil);
        return ResponseEntity.ok(Map.of(
                "idPerfil", idPerfil,
                "promedio", promedio
        ));
    }
}
