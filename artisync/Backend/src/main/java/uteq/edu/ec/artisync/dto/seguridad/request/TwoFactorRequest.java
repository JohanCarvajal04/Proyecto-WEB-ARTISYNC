package uteq.edu.ec.artisync.dto.seguridad.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorRequest {

    // §2.1 (OBS-AUTO-05): el correo YA NO viaja en el body — el usuario se
    // resuelve desde el ticket pre-auth (cookie HttpOnly "preAuth2fa") emitido
    // por login() tras validar la contraseña. Aceptar un correo aquí era
    // exactamente el bypass: cualquiera que conociera un correo podía intentar
    // fuerza bruta contra este endpoint sin haber pasado por login().
    @NotBlank(message = "El código 2FA es obligatorio")
    private String codigo;

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }
}
