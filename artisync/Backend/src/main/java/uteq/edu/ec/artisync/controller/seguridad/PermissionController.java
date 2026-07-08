package uteq.edu.ec.artisync.controller.seguridad;
import uteq.edu.ec.artisync.dto.seguridad.request.*;
import uteq.edu.ec.artisync.dto.seguridad.response.*;
import uteq.edu.ec.artisync.dto.shared.*;
import uteq.edu.ec.artisync.service.seguridad.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/permissions")
@Tag(name = "Consulta de Permisos", description = "Endpoints para consulta dinámica de permisos vigentes del usuario autenticado")
@SecurityRequirement(name = "bearerAuth")
public class PermissionController {

    @Operation(summary = "Obtener códigos de permisos activos para el usuario autenticado (sin prefijo ROLE_)")
    @GetMapping("/me")
    public ResponseEntity<List<String>> getMyPermissions() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return ResponseEntity.status(401).build();
        }

        List<String> permissions = auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .filter(a -> !a.startsWith("ROLE_"))
                .toList();

        return ResponseEntity.ok(permissions);
    }
}
