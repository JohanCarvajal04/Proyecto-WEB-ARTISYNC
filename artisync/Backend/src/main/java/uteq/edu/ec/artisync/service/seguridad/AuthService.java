package uteq.edu.ec.artisync.service.seguridad;
import uteq.edu.ec.artisync.repository.seguridad.*;
import uteq.edu.ec.artisync.repository.perfil.*;

import uteq.edu.ec.artisync.dto.seguridad.request.*;
import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.response.TokenResponse;
import uteq.edu.ec.artisync.dto.seguridad.response.UserResponse;

public interface AuthService {
    UserResponse register(RegisterRequest request);
    TokenResponse login(LoginRequest request);
    TokenResponse verify2Fa(TwoFactorRequest request);
    TokenResponse refreshToken(String refreshToken);
    RespuestaMensaje logout(String tokenHeader, String refreshToken);
    RespuestaMensaje forgotPassword(ForgotPasswordRequest request);
    RespuestaMensaje resetPassword(ResetPasswordRequest request);
}

