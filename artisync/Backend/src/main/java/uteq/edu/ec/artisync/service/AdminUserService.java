package uteq.edu.ec.artisync.service;

import org.springframework.data.domain.Pageable;
import uteq.edu.ec.artisync.dto.request.*;
import uteq.edu.ec.artisync.dto.response.MessageResponse;
import uteq.edu.ec.artisync.dto.response.UserResponse;
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
