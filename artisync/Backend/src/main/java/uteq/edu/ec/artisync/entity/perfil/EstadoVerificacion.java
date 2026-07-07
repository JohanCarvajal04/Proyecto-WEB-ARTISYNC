package uteq.edu.ec.artisync.entity.perfil;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "estados_verificacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadoVerificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_estado_verificacion")
    private Long idEstadoVerificacion;

    @NotBlank(message = "El nombre del estado es obligatorio")
    @Size(max = 50, message = "El nombre del estado no puede superar los 50 caracteres")
    @Column(name = "nombre_estado", nullable = false, unique = true, length = 50)
    private String nombreEstado;
}
