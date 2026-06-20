package uteq.edu.ec.artisync.model.seguridad;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(name = "autenticacion_dos_factores")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AutenticacionDosFactores {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_2fa")
    private Long id2fa;

    @NotNull(message = "El usuario es obligatorio")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private Usuario usuario;

    @NotBlank(message = "La llave secreta es obligatoria")
    @Size(max = 255, message = "La llave secreta no puede superar los 255 caracteres")
    @Column(name = "llave_secreta", nullable = false, length = 255)
    private String llaveSecreta;

    @Builder.Default
    @Column(name = "esta_habilitado", nullable = false)
    private Boolean estaHabilitado = false;
}
