package uteq.edu.ec.artisync.entity.legal;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "documentos_adjuntos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentoAdjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_adjunto")
    private Long idAdjunto;

    @NotNull(message = "El mensaje es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_mensaje", nullable = false)
    private Mensaje mensaje;

    @NotBlank(message = "La URL del archivo es obligatoria")
    @Size(max = 255, message = "La URL del archivo no puede superar los 255 caracteres")
    @Column(name = "url_archivo", nullable = false, length = 255)
    private String urlArchivo;

    @Size(max = 50, message = "El tipo MIME no puede superar los 50 caracteres")
    @Column(name = "tipo_mime", length = 50)
    private String tipoMime;

    @Column(name = "peso_bytes")
    private Integer pesoBytes;
}
