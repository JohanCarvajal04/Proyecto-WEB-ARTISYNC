package uteq.edu.ec.artisync.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Solicitud opcional para refrescar el token de acceso mediante cuerpo JSON si no se utiliza cookie HttpOnly")
public class RefreshTokenRequest {

    @Schema(description = "Refresh Token emitido durante el inicio de sesión", example = "eyJhbGciOiJIUzI1NiIsIn...")
    private String refreshToken;
}
