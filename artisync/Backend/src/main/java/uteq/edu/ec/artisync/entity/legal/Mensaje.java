package uteq.edu.ec.artisync.entity.legal;

import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "mensajes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_mensaje")
    private Long idMensaje;

    @NotNull(message = "La sala de chat es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sala", nullable = false)
    private SalaChat sala;

    @NotNull(message = "El remitente es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_remitente", nullable = false)
    private Usuario remitente;

    @Column(name = "cuerpo_mensaje", columnDefinition = "TEXT")
    private String cuerpoMensaje;

    @CreationTimestamp
    @Column(name = "fecha_hora_envio", updatable = false)
    private LocalDateTime fechaHoraEnvio;

    @Builder.Default
    @Column(name = "leido", nullable = false)
    private Boolean leido = false;
}
