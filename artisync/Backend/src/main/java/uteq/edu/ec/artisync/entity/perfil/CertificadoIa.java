package uteq.edu.ec.artisync.entity.perfil;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import uteq.edu.ec.artisync.entity.seguridad.Usuario;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "certificados_ia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CertificadoIa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_certificado")
    private Long idCertificado;

    @NotNull(message = "El perfil del creador es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_perfil", nullable = false)
    private PerfilCreador perfil;

    @NotNull(message = "El estado de verificacion es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_estado_verificacion", nullable = false)
    private EstadoVerificacion estadoVerificacion;

    @NotBlank(message = "La URL del documento es obligatoria")
    @Size(max = 255, message = "La URL del documento no puede superar los 255 caracteres")
    @Column(name = "url_documento_s3", nullable = false, length = 255)
    private String urlDocumentoS3;

    @DecimalMin(value = "0.00", message = "El puntaje de confianza no puede ser negativo")
    @DecimalMax(value = "1.00", message = "El puntaje de confianza no puede superar 1.00")
    @Column(name = "puntaje_confianza_ia", precision = 5, scale = 2)
    private BigDecimal puntajeConfianzaIa;

    @Column(name = "tipo_documento", nullable = false, length = 20)
    @Builder.Default
    private String tipoDocumento = "IDENTIDAD";

    @Column(name = "hash_documento", length = 64)
    private String hashDocumento;

    @Column(name = "veredicto_ia", length = 30)
    private String veredictoIa;

    @Column(name = "razon_ia", columnDefinition = "TEXT")
    private String razonIa;

    @Column(name = "datos_extraidos_ia", columnDefinition = "TEXT")
    private String datosExtraidosIa;

    @Column(name = "fecha_dictamen_ia")
    private LocalDateTime fechaDictamenIa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_moderador")
    private Usuario moderador;

    @Column(name = "fecha_decision")
    private LocalDateTime fechaDecision;

    @Column(name = "nota_moderador", columnDefinition = "TEXT")
    private String notaModerador;

    @Column(name = "documento_eliminado", nullable = false)
    @Builder.Default
    private boolean documentoEliminado = false;

    @CreationTimestamp
    @Column(name = "fecha_analisis", updatable = false)
    private LocalDateTime fechaAnalisis;
}
