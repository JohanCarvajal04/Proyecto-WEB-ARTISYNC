package uteq.edu.ec.artisync.service.seguridad;
import uteq.edu.ec.artisync.repository.seguridad.*;
import uteq.edu.ec.artisync.repository.perfil.*;

import org.springframework.data.domain.Pageable;
import uteq.edu.ec.artisync.dto.seguridad.request.*;
import uteq.edu.ec.artisync.dto.shared.MessageResponse;
import uteq.edu.ec.artisync.dto.seguridad.response.UserResponse;
import uteq.edu.ec.artisync.util.PagedResponse;

public interface AdminUserService {
    PagedResponse<UserResponse> getAllUsers(Pageable pageable);
    UserResponse getUserById(Long id);
    UserResponse createUser(CreateUserRequest request);
    UserResponse updateUser(Long id, AdminUpdateUserRequest request);
    UserResponse changeEstado(Long id, ChangeEstadoRequest request);
    UserResponse assignRoles(Long id, AssignRolesRequest request);
    MessageResponse revokeUserSessions(Long id);
    void deleteUser(Long id);
}
