package uteq.edu.ec.artisync.entity.comunicacion;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "tipos_notificacion")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TipoNotificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_tipo_notificacion")
    private Long idTipoNotificacion;

    @NotBlank(message = "El nombre del evento es obligatorio")
    @Size(max = 100, message = "El nombre del evento no puede superar los 100 caracteres")
    @Column(name = "nombre_evento", nullable = false, unique = true, length = 100)
    private String nombreEvento;

    @Column(name = "formato_mensaje", columnDefinition = "TEXT")
    private String formatoMensaje;
}
