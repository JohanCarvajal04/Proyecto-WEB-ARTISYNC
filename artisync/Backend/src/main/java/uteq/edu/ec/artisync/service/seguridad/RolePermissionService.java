package uteq.edu.ec.artisync.service.seguridad;
import uteq.edu.ec.artisync.repository.seguridad.*;
import uteq.edu.ec.artisync.repository.perfil.*;

import uteq.edu.ec.artisync.dto.seguridad.request.CreateRoleRequest;
import uteq.edu.ec.artisync.dto.seguridad.request.UpdateRoleRequest;
import uteq.edu.ec.artisync.dto.seguridad.response.PermissionResponse;
import uteq.edu.ec.artisync.dto.seguridad.response.RoleResponse;

import java.util.List;

public interface RolePermissionService {

    /**
     * Lista todos los roles del sistema con sus permisos asignados.
     *
     * @return los roles existentes
     */
    List<RoleResponse> getAllRoles();

    /**
     * Lista todos los permisos disponibles en el sistema.
     *
     * @return los permisos existentes
     */
    List<PermissionResponse> getAllPermissions();

    /**
     * Lista los códigos de permiso asignados a un rol.
     *
     * @param roleName nombre del rol a consultar
     * @return los códigos de permiso del rol
     */
    List<String> getPermissionsByRole(String roleName);

    /**
     * Reemplaza el conjunto completo de permisos de un rol y revoca las sesiones de los usuarios
     * que lo tienen asignado (salvo la del propio usuario autenticado), para forzar el refresco de
     * sus permisos en el JWT.
     *
     * @param roleName        nombre del rol a sincronizar
     * @param permissionCodes códigos de permiso que quedarán asignados al rol
     */
    void syncPermissions(String roleName, List<String> permissionCodes);

    /**
     * Crea un rol personalizado, opcionalmente con un conjunto inicial de permisos.
     *
     * @param request nombre, descripción y permisos iniciales del rol
     * @return el rol recién creado
     */
    RoleResponse createRole(CreateRoleRequest request);

    /**
     * Actualiza la descripción de un rol existente.
     *
     * @param idRol   id del rol a actualizar
     * @param request nueva descripción del rol
     * @return el rol ya actualizado
     */
    RoleResponse updateRole(Long idRol, UpdateRoleRequest request);

    /**
     * Elimina un rol personalizado, siempre que no sea un rol base protegido ni tenga usuarios activos asignados.
     *
     * @param idRol id del rol a eliminar
     */
    void deleteRole(Long idRol);
}
