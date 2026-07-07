package uteq.edu.ec.artisync.service;

import uteq.edu.ec.artisync.dto.request.ChangePasswordRequest;
import uteq.edu.ec.artisync.dto.request.UpdateUserRequest;
import uteq.edu.ec.artisync.dto.response.MessageResponse;
import uteq.edu.ec.artisync.dto.response.UserResponse;

public interface UserService {
    UserResponse getCurrentUser(String correo);
    UserResponse updateCurrentUser(String correo, UpdateUserRequest request);
    MessageResponse changePassword(String correo, ChangePasswordRequest request);
    MessageResponse deleteOwnAccount(String correo);
    MessageResponse revokeAllMySessions(String correo);
}
