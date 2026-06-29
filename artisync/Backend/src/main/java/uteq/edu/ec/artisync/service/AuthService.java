package uteq.edu.ec.artisync.service;

import uteq.edu.ec.artisync.dto.request.*;
import uteq.edu.ec.artisync.dto.response.*;
import jakarta.servlet.http.HttpServletRequest;

public interface AuthService {
    AuthResponse login(LoginRequest request, HttpServletRequest httpRequest);
    UsuarioResponse registro(RegistroRequest request);
    MensajeResponse logout(String token);
    MensajeResponse solicitarRecuperacion(String correo);
    MensajeResponse cambiarPassword(CambioPasswordRequest request);
    UsuarioResponse obtenerPerfil(String correo);
}
