package uteq.edu.ec.artisync.entity.catalogo;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/**
 * Entidad del modelo de dominio que representa Categorizacion libre para mejorar la busqueda de servicios.
 * 
 * Ciclo de vida: Su persistencia es gestionada por JPA. Es inmutable tras su creacion y funciona como un tag de busqueda.
 * 
 * Relaciones principales: Actua como entidad independiente, mapeada a multiples servicios mediante la tabla puente ServicioEtiqueta.
 */
@Entity
@Table(name = "etiquetas")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Etiqueta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_etiqueta")
    private Long idEtiqueta;

    @NotBlank(message = "El nombre de la etiqueta es obligatorio")
    @Size(max = 50, message = "El nombre de la etiqueta no puede superar los 50 caracteres")
    @Column(name = "nombre_etiqueta", nullable = false, unique = true, length = 50)
    private String nombreEtiqueta;

    @org.hibernate.annotations.UpdateTimestamp
    @Column(name = "actualizado_en")
    private java.time.LocalDateTime actualizadoEn;
}


