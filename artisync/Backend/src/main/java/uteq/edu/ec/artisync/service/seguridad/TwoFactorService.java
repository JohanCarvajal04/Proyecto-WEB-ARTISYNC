package uteq.edu.ec.artisync.service.seguridad;
import uteq.edu.ec.artisync.repository.seguridad.*;
import uteq.edu.ec.artisync.repository.perfil.*;

import uteq.edu.ec.artisync.dto.respuesta.comun.RespuestaMensaje;
import uteq.edu.ec.artisync.dto.seguridad.response.TwoFactorSetupResponse;

public interface TwoFactorService {
    TwoFactorSetupResponse setup2Fa(String correo);
    RespuestaMensaje confirm2Fa(String correo, String codigo);
    RespuestaMensaje disable2Fa(String correo, String codigo);
    boolean validarCodigoOBackup(String correo, String codigoIngresado);
}

