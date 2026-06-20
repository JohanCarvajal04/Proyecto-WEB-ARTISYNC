package uteq.edu.ec.artisync.model.catalogo;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "flujos_trabajo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlujoTrabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_flujo")
    private Long idFlujo;

    @NotBlank(message = "El nombre del flujo es obligatorio")
    @Size(max = 100, message = "El nombre del flujo no puede superar los 100 caracteres")
    @Column(name = "nombre_flujo", nullable = false, length = 100)
    private String nombreFlujo;

    @Column(name = "descripcion_flujo", columnDefinition = "TEXT")
    private String descripcionFlujo;
}
