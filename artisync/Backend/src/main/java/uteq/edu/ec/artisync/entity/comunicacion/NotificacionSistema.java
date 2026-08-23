package uteq.edu.ec.artisync.entity.comunicacion;

import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notificaciones_sistema")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificacionSistema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_notificacion")
    private Long idNotificacion;

    @NotNull(message = "El usuario destinatario es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @NotNull(message = "El tipo de notificacion es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_tipo_notificacion", nullable = false)
    private TipoNotificacion tipoNotificacion;

    /**
     * Texto propio de esta notificación, no el compartido de
     * {@link TipoNotificacion#getFormatoMensaje()}: ese campo se fija una
     * sola vez, la primera vez que se dispara el tipo de evento, así que
     * usarlo para listar notificaciones pasadas mostraba el mismo texto para
     * todas las notificaciones de un mismo tipo (ver migración V14).
     */
    @Column(name = "mensaje", columnDefinition = "TEXT")
    private String mensaje;

    @CreationTimestamp
    @Column(name = "fecha_emision", updatable = false)
    private LocalDateTime fechaEmision;

    @Builder.Default
    @Column(name = "esta_leida", nullable = false)
    private Boolean estaLeida = false;
}
