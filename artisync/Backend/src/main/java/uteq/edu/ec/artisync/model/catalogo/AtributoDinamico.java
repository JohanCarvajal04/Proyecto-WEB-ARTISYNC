package uteq.edu.ec.artisync.model.catalogo;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "atributos_dinamicos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AtributoDinamico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_atributo")
    private Long idAtributo;

    @NotBlank(message = "El nombre del atributo es obligatorio")
    @Size(max = 100, message = "El nombre del atributo no puede superar los 100 caracteres")
    @Column(name = "nombre_atributo", nullable = false, unique = true, length = 100)
    private String nombreAtributo;

    @NotBlank(message = "El tipo de dato es obligatorio")
    @Size(max = 50, message = "El tipo de dato no puede superar los 50 caracteres")
    @Column(name = "tipo_dato", nullable = false, length = 50)
    private String tipoDato;
}
