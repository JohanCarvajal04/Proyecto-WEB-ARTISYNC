package uteq.edu.ec.artisync.service.seguridad.impl;
import uteq.edu.ec.artisync.service.seguridad.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import uteq.edu.ec.artisync.dto.seguridad.request.CreateRoleRequest;
import uteq.edu.ec.artisync.dto.seguridad.request.UpdateRoleRequest;
import uteq.edu.ec.artisync.dto.seguridad.response.PermisoResponse;
import uteq.edu.ec.artisync.dto.seguridad.response.RolResponse;
import uteq.edu.ec.artisync.entity.seguridad.Permiso;
import uteq.edu.ec.artisync.entity.seguridad.Rol;
import uteq.edu.ec.artisync.repository.seguridad.PermisoRepository;
import uteq.edu.ec.artisync.repository.seguridad.RolRepository;
import uteq.edu.ec.artisync.repository.seguridad.UsuarioRolRepository;
import uteq.edu.ec.artisync.security.CustomUserDetails;
import uteq.edu.ec.artisync.service.seguridad.RolePermissionService;
import uteq.edu.ec.artisync.service.shared.SessionRevocationService;
import uteq.edu.ec.artisync.service.shared.StoredProcedureExceptionTranslator;

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
    private final SessionRevocationService sessionRevocationService;

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
        // REQ-F-003: fn_sincronizar_permisos_rol reemplaza atomicamente el set
        // completo de rol_permisos (DELETE+INSERT) en el motor, en vez de que
        // Hibernate calcule el diff de la coleccion @ManyToMany al hacer save().
        String nombreRolUpper = roleName.toUpperCase();
        String[] codigos = permissionCodes != null ? permissionCodes.toArray(new String[0]) : new String[0];
        Integer totalAsignado;
        try {
            totalAsignado = rolRepository.sincronizarPermisos(nombreRolUpper, codigos);
        } catch (RuntimeException e) {
            throw StoredProcedureExceptionTranslator.traducir(e, HttpStatus.NOT_FOUND);
        }
        log.info("Permisos sincronizados correctamente para el rol: {}. Total asignados: {}", nombreRolUpper, totalAsignado);

        // Los permisos viajan en el claim `permisos` del JWT, así que cambiarlos
        // en la base no afecta a quien ya tiene sesión abierta: seguiría con el
        // menú y los accesos anteriores hasta que su access token caducara.
        // assignRoles() ya revoca por este mismo motivo cuando cambian los roles
        // de un usuario; aquí toca hacerlo para todos los usuarios del rol.
        revocarSesionesDeUsuariosConRol(nombreRolUpper);
    }

    private void revocarSesionesDeUsuariosConRol(String roleName) {
        // NUNCA se revoca la sesión de quien está haciendo el cambio. El panel de
        // "Roles y Permisos" preselecciona el primer rol de la lista, que es
        // ADMIN, así que sin esta exclusión el administrador se cerraba la sesión
        // a sí mismo al primer guardado: su jti quedaba en la lista negra de
        // Redis y su fila de sesiones borrada, de modo que la siguiente petición
        // daba 401, el refresh fallaba y la UI acababa mostrando que no tenía los
        // permisos necesarios. Él ya conoce los permisos nuevos porque acaba de
        // fijarlos; el frontend recarga los suyos con GET /api/permissions/me.
        Long idActual = idUsuarioAutenticado();

        List<Long> afectados = usuarioRolRepository.findIdsUsuarioByNombreRol(roleName).stream()
                .filter(id -> !id.equals(idActual))
                .toList();

        for (Long idUsuario : afectados) {
            sessionRevocationService.revocarSesionesUsuario(idUsuario);
        }
        if (!afectados.isEmpty()) {
            log.info("Revocadas las sesiones de {} usuario(s) con el rol {} para que recarguen sus permisos",
                    afectados.size(), roleName);
        }
    }

    /** Id del usuario autenticado, o null si la petición no lleva un principal propio. */
    private Long idUsuarioAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof CustomUserDetails detalles) {
            return detalles.getIdUsuario();
        }
        return null;
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
        // REQ-F-004: fn_eliminar_rol valida (en la misma transaccion que el
        // DELETE) que el rol no sea uno de los roles base protegidos y que no
        // tenga usuarios activos asignados, antes de aplicar el borrado.
        try {
            rolRepository.eliminarRol(idRol);
        } catch (RuntimeException e) {
            throw StoredProcedureExceptionTranslator.traducir(e, HttpStatus.BAD_REQUEST);
        }
        log.info("Rol personalizado eliminado exitosamente (ID {})", idRol);
    }
}
