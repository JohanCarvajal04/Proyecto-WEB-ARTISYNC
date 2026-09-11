package uteq.edu.ec.artisync.dto.seguridad.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.List;

/**
 * Objeto de transferencia de datos (DTO) utilizado como carga útil de respuesta (Response).
 * 
 * Propósito: Respuesta critica que empaqueta el JWT de acceso y el token de refresco seguro de la sesion.
 * 
 * Este DTO se encarga de serializar la información hacia el cliente, enmascarando 
 * el modelo de dominio interno (Entidades JPA) y exponiendo estrictamente los 
 * atributos necesarios para cumplir con el contrato de esta vista del API.
 */
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

    // Â§2.1 (OBS-AUTO-05): igual que refreshToken, nunca se serializa al body â€”
    // AuthController lo mueve a una cookie HttpOnly (preAuth2fa), asÃ­ queda
    // fuera del alcance de JavaScript en el cliente.
    @JsonIgnore
    private String preAuthTicket;
}



