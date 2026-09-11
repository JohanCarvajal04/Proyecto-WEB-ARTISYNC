package uteq.edu.ec.artisync.entity.comunicacion;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Entidad del modelo de dominio que representa Enumeracion de eventos notificables (ej. NUEVO_PEDIDO, PAGO_APROBADO).
 * 
 * Ciclo de vida: Constante enumerada del sistema, no persistida como tabla independiente.
 * 
 * Relaciones principales: Se incrusta como un valor escalar (@Enumerated) en los registros de notificacion.
 */
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


