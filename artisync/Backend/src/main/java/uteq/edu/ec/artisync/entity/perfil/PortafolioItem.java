package uteq.edu.ec.artisync.entity.perfil;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "portafolio_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PortafolioItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_item_portafolio")
    private Long idItemPortafolio;

    @NotNull(message = "El portafolio es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_portafolio", nullable = false)
    private Portafolio portafolio;

    @NotBlank(message = "El titulo de la obra es obligatorio")
    @Size(max = 150, message = "El titulo de la obra no puede superar los 150 caracteres")
    @Column(name = "titulo_obra", nullable = false, length = 150)
    private String tituloObra;

    @Column(name = "descripcion_obra", columnDefinition = "TEXT")
    private String descripcionObra;

    @NotBlank(message = "La URL del archivo multimedia es obligatoria")
    @Size(max = 255, message = "La URL del archivo multimedia no puede superar los 255 caracteres")
    @Column(name = "url_archivo_multimedia", nullable = false, length = 255)
    private String urlArchivoMultimedia;

    @CreationTimestamp
    @Column(name = "fecha_subida", updatable = false)
    private LocalDateTime fechaSubida;
}
