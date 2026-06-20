package uteq.edu.ec.artisync.model.legal;

import uteq.edu.ec.artisync.model.pedido.Pedido;
import uteq.edu.ec.artisync.model.pedido.PlantillaContrato;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

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
}
