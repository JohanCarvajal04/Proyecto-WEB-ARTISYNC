package uteq.edu.ec.artisync.dto.seguridad.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Objeto de transferencia de datos (DTO) utilizado como carga útil de respuesta (Response).
 * 
 * Propósito: Proyeccion de los datos principales del usuario, omitiendo contrasenas y credenciales.
 * 
 * Este DTO se encarga de serializar la información hacia el cliente, enmascarando 
 * el modelo de dominio interno (Entidades JPA) y exponiendo estrictamente los 
 * atributos necesarios para cumplir con el contrato de esta vista del API.
 */
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
    private String urlFotoPerfil;
}



