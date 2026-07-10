package uteq.edu.ec.artisync.service.seguridad;
import uteq.edu.ec.artisync.repository.seguridad.*;
import uteq.edu.ec.artisync.repository.perfil.*;

import uteq.edu.ec.artisync.dto.seguridad.request.ChangePasswordRequest;
import uteq.edu.ec.artisync.dto.seguridad.request.UpdateUserRequest;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.response.UserResponse;

public interface UserService {
    UserResponse getCurrentUser(String correo);
    UserResponse updateCurrentUser(String correo, UpdateUserRequest request);
    RespuestaMensaje changePassword(String correo, ChangePasswordRequest request);
    RespuestaMensaje deleteOwnAccount(String correo);
    RespuestaMensaje revokeAllMySessions(String correo);
}

