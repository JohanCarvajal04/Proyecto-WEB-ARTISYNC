package uteq.edu.ec.artisync.entity.pedido;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "etapas_flujo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EtapaFlujo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_etapa")
    private Long idEtapa;

    @NotBlank(message = "El nombre de la etapa es obligatorio")
    @Size(max = 100, message = "El nombre de la etapa no puede superar los 100 caracteres")
    @Column(name = "nombre_etapa", nullable = false, unique = true, length = 100)
    private String nombreEtapa;
}
