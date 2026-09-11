package uteq.edu.ec.artisync.entity.legal;

import uteq.edu.ec.artisync.entity.pedido.Pedido;
import uteq.edu.ec.artisync.entity.pedido.PlantillaContrato;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Entidad del modelo de dominio que representa Acuerdo legal formal generado a partir de una propuesta de terminos aceptada.
 *
 * Ciclo de vida: Registro critico. Su ciclo de vida avanza desde CREADO hasta FIRMADO y CERRADO. Tras la firma es inmutable.
 *
 * Relaciones principales: Entidad raiz del flujo legal, vinculada a un Pedido y a firmas criptograficas.
 */
@Entity
@Table(name = "contratos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contrato {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_contrato")
    private Long idContrato;

    @NotNull(message = "El pedido es obligatorio")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pedido", nullable = false, unique = true)
    private Pedido pedido;

    @NotNull(message = "La plantilla de contrato es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_plantilla", nullable = false)
    private PlantillaContrato plantilla;

    @Size(max = 255, message = "El hash de la firma del cliente no puede superar los 255 caracteres")
    @Column(name = "hash_firma_cliente", length = 255)
    private String hashFirmaCliente;

    @Size(max = 255, message = "El hash de la firma del creador no puede superar los 255 caracteres")
    @Column(name = "hash_firma_creador", length = 255)
    private String hashFirmaCreador;

    @Builder.Default
    @Column(name = "limite_revisiones", nullable = false)
    private Integer limiteRevisiones = 0;

    @CreationTimestamp
    @Column(name = "fecha_formalizacion", updatable = false)
    private LocalDateTime fechaFormalizacion;

    @Size(max = 255, message = "La URL del documento PDF no puede superar los 255 caracteres")
    @Column(name = "url_documento_pdf", length = 255)
    private String urlDocumentoPdf;

    /** REQ-NF-020: snapshot del HTML ya renderizado en el momento en que quedó firmado por ambas partes. */
    @Column(name = "contenido_congelado", columnDefinition = "TEXT")
    private String contenidoCongelado;

    /** Hash SHA-256 (hex) de {@link #contenidoCongelado}; null hasta que el contrato esté firmado por ambas partes. */
    @Size(max = 64, message = "El hash del contenido no puede superar los 64 caracteres")
    @Column(name = "hash_contenido", length = 64)
    private String hashContenido;

    @Column(name = "fecha_hash_contenido")
    private LocalDateTime fechaHashContenido;

    @Column(name = "fecha_limite_retencion")
    private LocalDateTime fechaLimiteRetencion;
}
