package uteq.edu.ec.artisync.model.catalogo;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "servicio_atributos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ServicioAtributo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servicio_atributo")
    private Long idServicioAtributo;

    @NotNull(message = "El servicio es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_servicio", nullable = false)
    private Servicio servicio;

    @NotNull(message = "El atributo dinamico es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_atributo", nullable = false)
    private AtributoDinamico atributo;

    @NotBlank(message = "El valor asignado es obligatorio")
    @Size(max = 255, message = "El valor asignado no puede superar los 255 caracteres")
    @Column(name = "valor_asignado", nullable = false, length = 255)
    private String valorAsignado;
}
