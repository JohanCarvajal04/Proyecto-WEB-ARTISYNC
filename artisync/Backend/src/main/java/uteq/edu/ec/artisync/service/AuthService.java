package uteq.edu.ec.artisync.service;

import uteq.edu.ec.artisync.dto.request.*;
import uteq.edu.ec.artisync.dto.response.MessageResponse;
import uteq.edu.ec.artisync.dto.response.TokenResponse;
import uteq.edu.ec.artisync.dto.response.UserResponse;

public interface AuthService {
    UserResponse register(RegisterRequest request);
    TokenResponse login(LoginRequest request);
    TokenResponse verify2Fa(TwoFactorRequest request);
    TokenResponse refreshToken(String refreshToken);
    MessageResponse logout(String tokenHeader, String refreshToken);
    MessageResponse forgotPassword(ForgotPasswordRequest request);
    MessageResponse resetPassword(ResetPasswordRequest request);
}
