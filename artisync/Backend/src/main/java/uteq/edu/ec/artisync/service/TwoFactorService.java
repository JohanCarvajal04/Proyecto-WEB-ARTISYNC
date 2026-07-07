package uteq.edu.ec.artisync.service;

import uteq.edu.ec.artisync.dto.response.MessageResponse;
import uteq.edu.ec.artisync.dto.response.TwoFactorSetupResponse;

public interface TwoFactorService {
    TwoFactorSetupResponse setup2Fa(String correo);
    MessageResponse confirm2Fa(String correo, String codigo);
    MessageResponse disable2Fa(String correo, String codigo);
    boolean validarCodigoOBackup(String correo, String codigoIngresado);
}
