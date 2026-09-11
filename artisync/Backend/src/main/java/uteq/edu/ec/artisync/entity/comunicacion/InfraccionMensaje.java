package uteq.edu.ec.artisync.entity.comunicacion;

import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad del modelo de dominio que representa Registro de penalizaciones por mensajes inapropiados detectados en el chat.
 * 
 * Ciclo de vida: Registro inmutable. Una vez persistida, no debe alterarse, garantizando la auditoria de moderacion.
 * 
 * Relaciones principales: Vinculada a un Mensaje especifico y al Usuario infractor.
 */
@Entity
@Table(name = "infracciones_mensaje")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InfraccionMensaje {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_infraccion")
    private Long idInfraccion;

    @NotNull(message = "El usuario infractor es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @NotNull(message = "El pedido es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido", nullable = false)
    private Pedido pedido;

    @Column(name = "mensaje_original", columnDefinition = "TEXT")
    private String mensajeOriginal;

    @Size(max = 50, message = "El patrÃ³n detectado no puede superar los 50 caracteres")
    @Column(name = "patron_detectado", length = 50)
    private String patronDetectado;

    @CreationTimestamp
    @Column(name = "fecha_infraccion", updatable = false)
    private LocalDateTime fechaInfraccion;
}


