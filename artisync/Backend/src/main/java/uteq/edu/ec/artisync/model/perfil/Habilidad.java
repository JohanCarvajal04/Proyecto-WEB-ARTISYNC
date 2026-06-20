package uteq.edu.ec.artisync.model.perfil;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "habilidades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Habilidad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_habilidad")
    private Long idHabilidad;

    @NotBlank(message = "El nombre de la habilidad es obligatorio")
    @Size(max = 100, message = "El nombre de la habilidad no puede superar los 100 caracteres")
    @Column(name = "nombre_habilidad", nullable = false, unique = true, length = 100)
    private String nombreHabilidad;
}
