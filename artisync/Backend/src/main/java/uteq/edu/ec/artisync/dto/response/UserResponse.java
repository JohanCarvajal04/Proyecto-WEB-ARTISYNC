package uteq.edu.ec.artisync.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long idUsuario;
    private String nombres;
    private String apellidos;
    private String correo;
    private LocalDate fechaNacimiento;
    private Long idPais;
    private String nombrePais;
    private LocalDateTime fechaRegistro;
    private Boolean estadoCuenta;
    private List<String> roles;
    private List<String> permisos;
    private boolean dosFactoresHabilitado;
}
