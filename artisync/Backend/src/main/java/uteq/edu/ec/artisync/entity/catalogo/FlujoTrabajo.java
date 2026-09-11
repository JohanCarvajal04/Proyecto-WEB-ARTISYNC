package uteq.edu.ec.artisync.entity.catalogo;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Entidad del modelo de dominio que representa Plantilla de hitos o etapas asociadas a la prestacion de un servicio.
 * 
 * Ciclo de vida: Gestionada por JPA. Representa una plantilla reutilizable que dicta como debe progresar un pedido.
 * 
 * Relaciones principales: Entidad raiz que agrupa multiples etapas de configuracion de flujo.
 */
@Entity
@Table(name = "flujos_trabajo")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlujoTrabajo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_flujo")
    private Long idFlujo;

    @NotBlank(message = "El nombre del flujo es obligatorio")
    @Size(max = 100, message = "El nombre del flujo no puede superar los 100 caracteres")
    @Column(name = "nombre_flujo", nullable = false, length = 100)
    private String nombreFlujo;

    @Column(name = "descripcion_flujo", columnDefinition = "TEXT")
    private String descripcionFlujo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario_creador", nullable = false)
    private uteq.edu.ec.artisync.entity.seguridad.Usuario creador;
}


