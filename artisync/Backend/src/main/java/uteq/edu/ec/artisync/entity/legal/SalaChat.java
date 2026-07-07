package uteq.edu.ec.artisync.entity.legal;

import uteq.edu.ec.artisync.entity.pedido.Pedido;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "salas_chat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalaChat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sala")
    private Long idSala;

    @NotNull(message = "El pedido es obligatorio")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido", nullable = false, unique = true)
    private Pedido pedido;

    @CreationTimestamp
    @Column(name = "fecha_apertura", updatable = false)
    private LocalDateTime fechaApertura;

    @Builder.Default
    @Column(name = "sala_activa", nullable = false)
    private Boolean salaActiva = true;
}
