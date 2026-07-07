package uteq.edu.ec.artisync.entity.catalogo;

import uteq.edu.ec.artisync.entity.perfil.Categoria;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "subcategorias")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subcategoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_subcategoria")
    private Long idSubcategoria;

    @NotNull(message = "La categoria es obligatoria")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_categoria", nullable = false)
    private Categoria categoria;

    @NotBlank(message = "El nombre de la subcategoria es obligatorio")
    @Size(max = 100, message = "El nombre de la subcategoria no puede superar los 100 caracteres")
    @Column(name = "nombre_subcategoria", nullable = false, length = 100)
    private String nombreSubcategoria;
}
