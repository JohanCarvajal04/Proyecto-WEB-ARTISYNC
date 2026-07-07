package uteq.edu.ec.artisync.entity.pedido;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "plantillas_contrato")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlantillaContrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plantilla")
    private Long idPlantilla;

    @NotBlank(message = "La version legal es obligatoria")
    @Size(max = 50, message = "La version legal no puede superar los 50 caracteres")
    @Column(name = "version_legal", nullable = false, unique = true, length = 50)
    private String versionLegal;

    @NotBlank(message = "El cuerpo HTML de la plantilla es obligatorio")
    @Column(name = "cuerpo_html_plantilla", nullable = false, columnDefinition = "TEXT")
    private String cuerpoHtmlPlantilla;
}
