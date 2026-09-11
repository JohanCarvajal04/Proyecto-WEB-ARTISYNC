package uteq.edu.ec.artisync.entity.catalogo;

import uteq.edu.ec.artisync.entity.comunicacion.BriefingPlantilla;
import uteq.edu.ec.artisync.entity.perfil.PerfilCreador;
import uteq.edu.ec.artisync.entity.pedido.ContractTemplate;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "servicios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Offering {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_servicio")
    private Long idServicio;

    @NotNull(message = "El perfil del creador es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_perfil", nullable = false)
    private PerfilCreador perfil;

    @NotBlank(message = "El titulo del servicio es obligatorio")
    @Size(max = 150, message = "El titulo del servicio no puede superar los 150 caracteres")
    @Column(name = "titulo_servicio", nullable = false, length = 150)
    private String tituloServicio;

    @NotBlank(message = "La descripcion detallada es obligatoria")
    @Size(min = 20, max = 2000, message = "La descripcion debe tener entre 20 y 2000 caracteres")
    @Column(name = "descripcion_detallada", nullable = false, columnDefinition = "TEXT")
    private String descripcionDetallada;

    @NotNull(message = "El precio base es obligatorio")
    @DecimalMin(value = "0.01", message = "El precio debe ser de al menos 0.01 USD")
    @Column(name = "precio_base", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioBase;

    @Size(max = 255, message = "La URL de la miniatura no puede superar los 255 caracteres")
    @Column(name = "url_miniatura", length = 255)
    private String urlMiniatura;

    @NotBlank(message = "El tipo de item es obligatorio")
    @Builder.Default
    @Column(name = "tipo_item", nullable = false, length = 20)
    private String tipoItem = "SERVICIO";

    @NotBlank(message = "El estado de publicacion es obligatorio")
    @Builder.Default
    @Column(name = "estado_publicacion", nullable = false, length = 20)
    private String estadoPublicacion = "ACTIVO";

    @Builder.Default
    @Column(name = "cargo_revision_adicional", precision = 10, scale = 2)
    private BigDecimal cargoRevisionAdicional = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "limite_revisiones_base")
    private Integer limiteRevisionesBase = 0;

    /**
     * Flujo de trabajo elegido por el creador para este servicio, entre los
     * suyos propios. Nullable a propósito: si queda sin asignar, el servicio
     * de pedidos cae a un flujo por defecto en vez de bloquear el encargo.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_flujo")
    private Workflow flujo;

    /**
     * Plantilla de contrato elegida por el creador entre el catálogo curado
     * por ADMIN. Nullable a propósito: si queda sin asignar,
     * ContractServiceImpl cae a la plantilla marcada como predeterminada en
     * vez de bloquear la generación del contrato.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plantilla_contrato")
    private ContractTemplate plantillaContrato;

    /**
     * Cuestionario que el cliente debe responder al crear un pedido para este
     * servicio, entre las plantillas propias del creador. Nullable a
     * propósito: un servicio sin cuestionario asignado no pide nada extra al
     * crear el pedido (ver OrderServiceImpl.crearPedido).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_briefing_plantilla")
    private BriefingPlantilla briefingPlantilla;

    @org.hibernate.annotations.UpdateTimestamp
    @Column(name = "actualizado_en")
    private java.time.LocalDateTime actualizadoEn;
}
