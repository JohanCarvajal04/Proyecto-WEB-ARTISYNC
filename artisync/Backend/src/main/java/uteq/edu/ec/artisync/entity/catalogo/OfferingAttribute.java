package uteq.edu.ec.artisync.entity.catalogo;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Entidad del modelo de dominio que representa Entidad asociativa (relacion NxM) entre Offering y DynamicAttribute con valores por defecto.
 * 
 * Ciclo de vida: Gestionada por JPA. Su ciclo de vida esta ligado a la existencia del servicio principal (CascadeType.ALL).
 * 
 * Relaciones principales: Actua como tabla puente con payload (valorAsignado) uniendo Offering y DynamicAttribute.
 */
@Entity
@Table(name = "servicio_atributos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferingAttribute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servicio_atributo")
    private Long idServicioAtributo;

    @NotNull(message = "El servicio es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_servicio", nullable = false)
    private Offering servicio;

    @NotNull(message = "El atributo dinamico es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_atributo", nullable = false)
    private DynamicAttribute atributo;

    @NotBlank(message = "El valor asignado es obligatorio")
    @Size(max = 255, message = "El valor asignado no puede superar los 255 caracteres")
    @Column(name = "valor_asignado", nullable = false, length = 255)
    private String valorAsignado;

    @org.hibernate.annotations.UpdateTimestamp
    @Column(name = "actualizado_en")
    private java.time.LocalDateTime actualizadoEn;
}


