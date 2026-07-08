package uteq.edu.ec.artisync.service.seguridad;
import uteq.edu.ec.artisync.repository.seguridad.*;
import uteq.edu.ec.artisync.repository.perfil.*;

import uteq.edu.ec.artisync.dto.seguridad.request.CreateRoleRequest;
import uteq.edu.ec.artisync.dto.seguridad.request.UpdateRoleRequest;
import uteq.edu.ec.artisync.dto.seguridad.response.PermisoResponse;
import uteq.edu.ec.artisync.dto.seguridad.response.RolResponse;

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
