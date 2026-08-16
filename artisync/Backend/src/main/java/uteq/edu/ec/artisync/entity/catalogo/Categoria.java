package uteq.edu.ec.artisync.entity.catalogo;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "categorias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_categoria")
    private Long idCategoria;

    @NotBlank(message = "El nombre de la categoria es obligatorio")
    @Size(max = 100, message = "El nombre de la categoria no puede superar los 100 caracteres")
    @Column(name = "nombre_categoria", nullable = false, unique = true, length = 100)
    private String nombreCategoria;

    @Builder.Default
    @Column(name = "estado_activa", nullable = false)
    private Boolean estadoActiva = true;

    /**
     * Flujo de trabajo que heredan los pedidos de esta categoría (RF-19).
     *
     * Nullable a propósito: si queda sin asignar, el servicio de pedidos cae a
     * un flujo por defecto en vez de bloquear la creación del encargo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_flujo")
    private FlujoTrabajo flujo;

    @org.hibernate.annotations.UpdateTimestamp
    @Column(name = "actualizado_en")
    private java.time.LocalDateTime actualizadoEn;
}
