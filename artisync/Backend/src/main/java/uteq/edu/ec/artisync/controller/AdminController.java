package uteq.edu.ec.artisync.controller;

import uteq.edu.ec.artisync.dto.response.UsuarioResponse;
import uteq.edu.ec.artisync.model.seguridad.*;
import uteq.edu.ec.artisync.repository.*;
import uteq.edu.ec.artisync.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@CrossOrigin(origins = "*")
public class AdminController {

    @Autowired private UsuarioRepository usuarioRepo;
    @Autowired private RolRepository rolRepo;
    @Autowired private PermisoRepository permisoRepo;
    @Autowired private AuthService authService;


    @GetMapping("/usuarios")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Usuario>> listarUsuarios() {
        return ResponseEntity.ok(usuarioRepo.findAll());
    }

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

    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Rol>> listarRoles() {
        return ResponseEntity.ok(rolRepo.findAll());
    }


    @GetMapping("/permisos")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Permiso>> listarPermisos() {
        return ResponseEntity.ok(permisoRepo.findAll());
    }
}
