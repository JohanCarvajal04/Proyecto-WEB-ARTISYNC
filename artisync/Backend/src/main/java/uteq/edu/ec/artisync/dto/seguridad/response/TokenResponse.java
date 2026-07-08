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
}
