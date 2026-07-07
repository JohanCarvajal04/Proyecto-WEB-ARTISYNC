package uteq.edu.ec.artisync.entity.seguridad;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "pais")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pais {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_pais")
    private Long idPais;

    @NotBlank(message = "El nombre del pais es obligatorio")
    @Size(max = 100, message = "El nombre del pais no puede superar los 100 caracteres")
    @Column(name = "nombre_pais", nullable = false, unique = true, length = 100)
    private String nombrePais;
}
