package uteq.edu.ec.artisync.service.seguridad;
import uteq.edu.ec.artisync.repository.seguridad.*;
import uteq.edu.ec.artisync.repository.perfil.*;

import uteq.edu.ec.artisync.dto.seguridad.request.ChangePasswordRequest;
import uteq.edu.ec.artisync.dto.seguridad.request.UpdateUserRequest;
import uteq.edu.ec.artisync.dto.shared.MessageResponse;
import uteq.edu.ec.artisync.dto.seguridad.response.UserResponse;

public interface UserService {
    UserResponse getCurrentUser(String correo);
    UserResponse updateCurrentUser(String correo, UpdateUserRequest request);
    MessageResponse changePassword(String correo, ChangePasswordRequest request);
    MessageResponse deleteOwnAccount(String correo);
    MessageResponse revokeAllMySessions(String correo);
}
