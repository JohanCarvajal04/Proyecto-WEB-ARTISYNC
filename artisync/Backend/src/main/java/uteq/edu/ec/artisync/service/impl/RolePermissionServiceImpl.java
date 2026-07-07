package uteq.edu.ec.artisync.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.dto.request.CreateRoleRequest;
import uteq.edu.ec.artisync.dto.request.UpdateRoleRequest;
import uteq.edu.ec.artisync.dto.response.PermisoResponse;
import uteq.edu.ec.artisync.dto.response.RolResponse;
import uteq.edu.ec.artisync.entity.seguridad.Permiso;
import uteq.edu.ec.artisync.entity.seguridad.Rol;
import uteq.edu.ec.artisync.repository.PermisoRepository;
import uteq.edu.ec.artisync.repository.RolRepository;
import uteq.edu.ec.artisync.repository.UsuarioRolRepository;
import uteq.edu.ec.artisync.service.RolePermissionService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RolePermissionServiceImpl implements RolePermissionService {

    private final RolRepository rolRepository;
    private final PermisoRepository permisoRepository;
    private final UsuarioRolRepository usuarioRolRepository;

    @Override
    @Transactional(readOnly = true)
    public List<RolResponse> getAllRoles() {
        return rolRepository.findAll().stream()
                .map(r -> RolResponse.builder()
                        .idRol(r.getIdRol())
                        .nombreRol(r.getNombreRol())
                        .descripcionRol(r.getDescripcionRol())
                        .permisos(r.getPermisos() != null ? r.getPermisos().stream().map(Permiso::getNombrePermiso).toList() : List.of())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PermisoResponse> getAllPermisos() {
        return permisoRepository.findAll().stream()
                .map(p -> PermisoResponse.builder()
                        .idPermiso(p.getIdPermiso())
                        .nombrePermiso(p.getNombrePermiso())
                        .moduloAplicacion(p.getModuloAplicacion())
                        .build())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getPermissionsByRole(String roleName) {
        Rol rol = rolRepository.findByNombreRol(roleName.toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado: " + roleName));
        if (rol.getPermisos() == null) {
            return List.of();
        }
        return rol.getPermisos().stream()
                .map(Permiso::getNombrePermiso)
                .toList();
    }

    @Override
    @Transactional
    public void syncPermissions(String roleName, List<String> permissionCodes) {
        Rol rol = rolRepository.findByNombreRol(roleName.toUpperCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado: " + roleName));

        Set<Permiso> nuevosPermisos = new HashSet<>();
        if (permissionCodes != null) {
            for (String code : permissionCodes) {
                Permiso p = permisoRepository.findByNombrePermiso(code.toUpperCase())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Permiso inexistente: " + code));
                nuevosPermisos.add(p);
            }
        }

        rol.setPermisos(nuevosPermisos);
        rolRepository.save(rol);
        log.info("Permisos sincronizados correctamente para el rol: {}. Total asignados: {}", roleName, nuevosPermisos.size());
    }

    @Override
    @Transactional
    public RolResponse createRole(CreateRoleRequest request) {
        String nombreRolUpper = request.getNombreRol().trim().toUpperCase();
        if (rolRepository.findByNombreRol(nombreRolUpper).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un rol con el nombre: " + nombreRolUpper);
        }

        Set<Permiso> permisosIniciales = new HashSet<>();
        if (request.getPermisosIniciales() != null) {
            for (String code : request.getPermisosIniciales()) {
                Permiso p = permisoRepository.findByNombrePermiso(code.toUpperCase())
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Permiso inexistente: " + code));
                permisosIniciales.add(p);
            }
        }

        Rol nuevoRol = Rol.builder()
                .nombreRol(nombreRolUpper)
                .descripcionRol(request.getDescripcionRol())
                .permisos(permisosIniciales)
                .build();

        nuevoRol = rolRepository.save(nuevoRol);
        log.info("Rol personalizado creado exitosamente: {}", nombreRolUpper);

        return RolResponse.builder()
                .idRol(nuevoRol.getIdRol())
                .nombreRol(nuevoRol.getNombreRol())
                .descripcionRol(nuevoRol.getDescripcionRol())
                .permisos(nuevoRol.getPermisos().stream().map(Permiso::getNombrePermiso).toList())
                .build();
    }

    @Override
    @Transactional
    public RolResponse updateRole(Long idRol, UpdateRoleRequest request) {
        Rol rol = rolRepository.findById(idRol)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado con ID: " + idRol));

        if (request.getDescripcionRol() != null) {
            rol.setDescripcionRol(request.getDescripcionRol());
        }

        rol = rolRepository.save(rol);
        log.info("Descripción del rol ID {} ({}) actualizada", idRol, rol.getNombreRol());

        return RolResponse.builder()
                .idRol(rol.getIdRol())
                .nombreRol(rol.getNombreRol())
                .descripcionRol(rol.getDescripcionRol())
                .permisos(rol.getPermisos() != null ? rol.getPermisos().stream().map(Permiso::getNombrePermiso).toList() : List.of())
                .build();
    }

    @Override
    @Transactional
    public void deleteRole(Long idRol) {
        Rol rol = rolRepository.findById(idRol)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rol no encontrado con ID: " + idRol));

        Set<String> rolesProtegidos = Set.of("ADMIN", "CLIENTE", "CREADOR", "MODERADOR", "SOPORTE", "AUDITOR_FINANCIERO");
        if (rolesProtegidos.contains(rol.getNombreRol().toUpperCase())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede eliminar un rol base del sistema: " + rol.getNombreRol());
        }

        if (usuarioRolRepository.existsByRolIdRol(idRol)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No se puede eliminar el rol porque tiene usuarios activos asignados");
        }

        rolRepository.delete(rol);
        log.info("Rol personalizado eliminado exitosamente: {} (ID {})", rol.getNombreRol(), idRol);
    }
}
