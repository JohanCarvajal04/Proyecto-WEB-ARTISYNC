package uteq.edu.ec.artisync.entity.pedido;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Entidad del modelo de dominio que representa Razonamiento estandarizado cuando un creador declina un encargo.
 * 
 * Ciclo de vida: Catalogo maestro gestionado por administradores. Ciclo de vida cuasi-estatico.
 * 
 * Relaciones principales: Vinculada a un pedido rechazado para metricas de cancelacion.
 */
@Entity
@Table(name = "motivos_rechazo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MotivoRechazo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_motivo")
    private Long idMotivo;

    @NotBlank(message = "La descripcion del motivo es obligatoria")
    @Size(max = 150, message = "La descripcion del motivo no puede superar los 150 caracteres")
    @Column(name = "descripcion_motivo", nullable = false, unique = true, length = 150)
    private String descripcionMotivo;
}


