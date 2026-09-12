package uteq.edu.ec.artisync.entity.pedido;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Plantilla de contrato: del catálogo general curado por ADMIN, o privada de un creador (V45). */
@Entity
@Table(name = "plantillas_contrato")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_plantilla")
    private Long idPlantilla;

    @NotBlank(message = "La version legal es obligatoria")
    @Size(max = 50, message = "La version legal no puede superar los 50 caracteres")
    @Column(name = "version_legal", nullable = false, unique = true, length = 50)
    private String versionLegal;

    @NotBlank(message = "El cuerpo HTML de la plantilla es obligatorio")
    @Column(name = "cuerpo_html_plantilla", nullable = false, columnDefinition = "TEXT")
    private String cuerpoHtmlPlantilla;

    @NotBlank(message = "El nombre de la plantilla es obligatorio")
    @Size(max = 150, message = "El nombre de la plantilla no puede superar los 150 caracteres")
    @Column(name = "nombre_plantilla", nullable = false, length = 150)
    private String nombrePlantilla;

    /** Fallback de ContractServiceImpl cuando el servicio del pedido no tiene una plantilla propia asignada. */
    @Builder.Default
    @Column(name = "es_predeterminada", nullable = false)
    private Boolean esPredeterminada = false;

    /** Soft delete: no se borra físicamente porque Contract.plantilla la referencia con FK. */
    @Builder.Default
    @Column(name = "activa", nullable = false)
    private Boolean activa = true;

    /**
     * NULL: plantilla del catálogo general, curada por ADMIN (comportamiento
     * original de V39). No NULL: plantilla privada de ese creador (V45) — solo
     * él puede editarla, desactivarla o asignarla a sus propios servicios;
     * nunca puede ser la predeterminada del catálogo general.
     */
    @Column(name = "id_creador")
    private Long idCreador;
}
