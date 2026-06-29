package uteq.edu.ec.artisync.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class UsuarioResponse {
    private Long idUsuario;
    private String nombres;
    private String apellidos;
    private String correo;
    private String pais;
    private LocalDateTime fechaRegistro;
    private Boolean estadoCuenta;
    private List<String> roles;
    private List<String> permisos;
}
