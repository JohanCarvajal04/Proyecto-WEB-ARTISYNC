package uteq.edu.ec.artisync.controller;

import uteq.edu.ec.artisync.dto.response.UsuarioResponse;
import uteq.edu.ec.artisync.model.seguridad.*;
import uteq.edu.ec.artisync.repository.*;
import uteq.edu.ec.artisync.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
@Tag(name = "Administración", description = "Endpoints de gestión de usuarios, roles y permisos (Requiere rol ADMIN)")
public class AdminController {

    @Autowired private UsuarioRepository usuarioRepo;
    @Autowired private RolRepository rolRepo;
    @Autowired private PermisoRepository permisoRepo;
    @Autowired private AuthService authService;

    @Operation(summary = "Listar usuarios", description = "Devuelve la lista completa de usuarios registrados en el sistema")
    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Usuario>> listarUsuarios() {
        return ResponseEntity.ok(usuarioRepo.findAll());
    }

    @Operation(summary = "Cambiar estado de cuenta", description = "Activa o desactiva la cuenta de un usuario específico")
    @PutMapping("/usuarios/{id}/estado")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> cambiarEstado(
            @PathVariable Integer id,
            @RequestParam boolean estado) {
        Usuario u = usuarioRepo.findById(id)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        u.setEstadoCuenta(estado);
        usuarioRepo.save(u);
        return ResponseEntity.ok("Estado actualizado a: " + estado);
    }

    @Operation(summary = "Asignar rol a usuario", description = "Asigna un rol específico a un usuario por sus IDs")
    @PostMapping("/usuarios/{idUsuario}/roles/{idRol}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> asignarRol(
            @PathVariable Integer idUsuario,
            @PathVariable Integer idRol) {
        Usuario usuario = usuarioRepo.findById(idUsuario)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        Rol rol = rolRepo.findById(idRol)
            .orElseThrow(() -> new RuntimeException("Rol no encontrado"));

        if (!usuario.getRoles().contains(rol)) {
            usuario.getRoles().add(rol);
            usuarioRepo.save(usuario);
        }
        return ResponseEntity.ok("Rol asignado correctamente");
    }

    @Operation(summary = "Remover rol a usuario", description = "Elimina un rol asignado a un usuario por sus IDs")
    @DeleteMapping("/usuarios/{idUsuario}/roles/{idRol}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> removerRol(
            @PathVariable Integer idUsuario,
            @PathVariable Integer idRol) {
        Usuario usuario = usuarioRepo.findById(idUsuario)
            .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        usuario.getRoles().removeIf(r -> r.getIdRol().equals(idRol));
        usuarioRepo.save(usuario);
        return ResponseEntity.ok("Rol removido correctamente");
    }

    @Operation(summary = "Listar roles", description = "Devuelve la lista de roles disponibles en el sistema")
    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Rol>> listarRoles() {
        return ResponseEntity.ok(rolRepo.findAll());
    }

    @Operation(summary = "Listar permisos", description = "Devuelve la lista de permisos disponibles en el sistema")
    @GetMapping("/permisos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Permiso>> listarPermisos() {
        return ResponseEntity.ok(permisoRepo.findAll());
    }
}
