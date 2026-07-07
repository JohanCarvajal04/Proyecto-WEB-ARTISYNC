package uteq.edu.ec.artisync.service;

import uteq.edu.ec.artisync.dto.request.CreateRoleRequest;
import uteq.edu.ec.artisync.dto.request.UpdateRoleRequest;
import uteq.edu.ec.artisync.dto.response.PermisoResponse;
import uteq.edu.ec.artisync.dto.response.RolResponse;

import java.util.List;

public interface RolePermissionService {

    List<RolResponse> getAllRoles();

    List<PermisoResponse> getAllPermisos();

    List<String> getPermissionsByRole(String roleName);

    void syncPermissions(String roleName, List<String> permissionCodes);

    RolResponse createRole(CreateRoleRequest request);

    RolResponse updateRole(Long idRol, UpdateRoleRequest request);

    void deleteRole(Long idRol);
}
