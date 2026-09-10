package uteq.edu.ec.artisync.controller.sistema;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import uteq.edu.ec.artisync.dto.peticion.sistema.PeticionActualizarPoliticaRespaldo;
import uteq.edu.ec.artisync.dto.respuesta.sistema.RespuestaRespaldo;
import uteq.edu.ec.artisync.dto.respuesta.sistema.ResumenRespaldos;
import uteq.edu.ec.artisync.entity.sistema.RespaldoBd;
import uteq.edu.ec.artisync.entity.sistema.RespaldoPolitica;
import uteq.edu.ec.artisync.audit.Auditable;
import uteq.edu.ec.artisync.audit.ModuloAuditoria;
import uteq.edu.ec.artisync.service.sistema.IRespaldoBdServicio;
import uteq.edu.ec.artisync.util.PagedResponse;

@RestController
@RequestMapping("/api/v1/admin/respaldos")
@RequiredArgsConstructor
public class RespaldoBdControlador {

    private final IRespaldoBdServicio respaldoServicio;

    @GetMapping
    @PreAuthorize("hasAuthority('RESPALDO_VER') or hasRole('ADMIN')")
    public ResponseEntity<PagedResponse<RespuestaRespaldo>> listar(
            @PageableDefault(size = 20, sort = "fechaCreacion", direction = Sort.Direction.DESC) Pageable pageable,
            @RequestParam(required = false) String tipo,
            @RequestParam(required = false) String categoria) {
        return ResponseEntity.ok(respaldoServicio.listar(pageable, tipo, categoria));
    }

    @GetMapping("/resumen")
    @PreAuthorize("hasAuthority('RESPALDO_VER') or hasRole('ADMIN')")
    public ResponseEntity<ResumenRespaldos> obtenerResumen() {
        return ResponseEntity.ok(respaldoServicio.obtenerResumen());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('RESPALDO_VER') or hasRole('ADMIN')")
    public ResponseEntity<RespuestaRespaldo> obtenerPorId(@PathVariable Long id) {
        return ResponseEntity.ok(respaldoServicio.obtenerPorId(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('RESPALDO_CREAR') or hasRole('ADMIN')")
    @Auditable(accion = "RESPALDO_MANUAL_CREAR", modulo = ModuloAuditoria.SISTEMA, entidad = "respaldos_bd")
    public ResponseEntity<RespuestaRespaldo> crearRespaldoManual(
            java.security.Principal principal,
            @RequestParam(required = false, defaultValue = "FULL") RespaldoBd.CategoriaRespaldo categoria) {
        String correo = principal != null ? principal.getName() : "anonimo";
        return ResponseEntity.status(201).body(respaldoServicio.crearRespaldoManual(correo, categoria));
    }

    @PostMapping(value = "/importar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('RESPALDO_RESTAURAR') or hasRole('ADMIN')")
    @Auditable(accion = "RESPALDO_IMPORTAR", modulo = ModuloAuditoria.SISTEMA, entidad = "respaldos_bd")
    public ResponseEntity<RespuestaRespaldo> importar(
            @RequestParam("archivo") MultipartFile archivo,
            java.security.Principal principal) {
        String correo = principal != null ? principal.getName() : "anonimo";
        return ResponseEntity.status(201).body(respaldoServicio.importarRespaldoExterno(correo, archivo));
    }

    @GetMapping("/{id}/descargar")
    @PreAuthorize("hasAuthority('RESPALDO_DESCARGAR') or hasRole('ADMIN')")
    @Auditable(accion = "RESPALDO_DESCARGAR", modulo = ModuloAuditoria.SISTEMA, entidad = "respaldos_bd")
    public ResponseEntity<Resource> descargar(@PathVariable Long id) {
        Resource resource = respaldoServicio.descargar(id);
        String contentType = "application/octet-stream";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('RESPALDO_ELIMINAR') or hasRole('ADMIN')")
    @Auditable(accion = "RESPALDO_ELIMINAR", modulo = ModuloAuditoria.SISTEMA, entidad = "respaldos_bd")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        respaldoServicio.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/restaurar")
    @PreAuthorize("hasAuthority('RESPALDO_RESTAURAR') or hasRole('ADMIN')")
    @Auditable(accion = "RESPALDO_RESTAURAR", modulo = ModuloAuditoria.SISTEMA, entidad = "respaldos_bd")
    public ResponseEntity<Void> restaurar(@PathVariable Long id) {
        respaldoServicio.restaurar(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/politica")
    @PreAuthorize("hasAuthority('RESPALDO_VER') or hasRole('ADMIN')")
    public ResponseEntity<RespaldoPolitica> obtenerPolitica() {
        return ResponseEntity.ok(respaldoServicio.obtenerPolitica());
    }

    @PutMapping("/politica")
    @PreAuthorize("hasAuthority('RESPALDO_CONFIGURAR') or hasRole('ADMIN')")
    @Auditable(accion = "RESPALDO_POLITICA_CONFIGURAR", modulo = ModuloAuditoria.SISTEMA, entidad = "respaldos_politica")
    public ResponseEntity<RespaldoPolitica> actualizarPolitica(@Valid @RequestBody PeticionActualizarPoliticaRespaldo peticion) {
        return ResponseEntity.ok(respaldoServicio.actualizarPolitica(peticion));
    }
}
