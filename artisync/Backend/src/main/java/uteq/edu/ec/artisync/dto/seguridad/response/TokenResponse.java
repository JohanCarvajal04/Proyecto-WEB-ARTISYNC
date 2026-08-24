package uteq.edu.ec.artisync.dto.seguridad.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

    private String accessToken;

    @Builder.Default
    private String tokenType = "Bearer";

    private Long idUsuario;
    private String correo;
    private List<String> roles;
    private List<String> permisos;
    private boolean requiere2fa;

    @Builder.Default
    private Long expiresIn = 3600000L;

    @JsonIgnore
    private String refreshToken;

    // §2.1 (OBS-AUTO-05): igual que refreshToken, nunca se serializa al body —
    // AuthController lo mueve a una cookie HttpOnly (preAuth2fa), así queda
    // fuera del alcance de JavaScript en el cliente.
    @JsonIgnore
    private String preAuthTicket;

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getPreAuthTicket() {
        return preAuthTicket;
    }

    public void setPreAuthTicket(String preAuthTicket) {
        this.preAuthTicket = preAuthTicket;
    }
}
