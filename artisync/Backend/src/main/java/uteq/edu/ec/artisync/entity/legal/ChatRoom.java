package uteq.edu.ec.artisync.entity.legal;

import uteq.edu.ec.artisync.entity.pedido.Order;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad del modelo de dominio que representa Espacio de comunicacion aislado y cifrado entre cliente y creador para un pedido especifico.
 * 
 * Ciclo de vida: Su ciclo de vida inicia al confirmar el pedido y se cierra/archiva al finalizar o cancelar el mismo.
 * 
 * Relaciones principales: Contenedor maestro que agrupa un historial inmutable de Mensajes.
 */
@Entity
@Table(name = "salas_chat")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChatRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sala")
    private Long idSala;

    @NotNull(message = "El pedido es obligatorio")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido", nullable = false, unique = true)
    private Order pedido;

    @CreationTimestamp
    @Column(name = "fecha_apertura", updatable = false)
    private LocalDateTime fechaApertura;

    @Builder.Default
    @Column(name = "sala_activa", nullable = false)
    private Boolean salaActiva = true;
}


