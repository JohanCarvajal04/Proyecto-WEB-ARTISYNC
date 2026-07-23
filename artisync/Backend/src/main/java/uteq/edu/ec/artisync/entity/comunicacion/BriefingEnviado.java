package uteq.edu.ec.artisync.entity.comunicacion;

import uteq.edu.ec.artisync.entity.pedido.Pedido;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Briefing enviado por el Creador a un Pedido específico.
 * RF-16: Una copia del briefing por pedido; una vez completado, las respuestas son inmutables.
 */
@Entity
@Table(name = "briefing_enviados")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BriefingEnviado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_briefing_enviado")
    private Long idBriefingEnviado;

    @NotNull(message = "El pedido es obligatorio")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido", nullable = false, unique = true)
    private Pedido pedido;

    @NotNull(message = "La plantilla es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_briefing_plantilla", nullable = false)
    private BriefingPlantilla plantilla;

    @CreationTimestamp
    @Column(name = "fecha_envio", updatable = false)
    private LocalDateTime fechaEnvio;

    @Builder.Default
    @Column(name = "completado", nullable = false)
    private Boolean completado = false;

    @Builder.Default
    @OneToMany(mappedBy = "briefingEnviado", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<BriefingRespuesta> respuestas = new ArrayList<>();
}
