package uteq.edu.ec.artisync.dto.response;

import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {
    private String token;
    private String tipo = "Bearer";
    private Long idUsuario;
    private String nombres;
    private String apellidos;
    private String correo;
    private List<String> roles;
    private List<String> permisos;
}
