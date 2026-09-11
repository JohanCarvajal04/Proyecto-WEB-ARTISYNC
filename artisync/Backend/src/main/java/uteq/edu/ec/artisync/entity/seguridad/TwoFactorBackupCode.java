package uteq.edu.ec.artisync.entity.seguridad;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

/**
 * Entidad del modelo de dominio que representa Codigos de un solo uso en caso de perdida del dispositivo 2FA.
 * 
 * Ciclo de vida: Un solo uso (quemados tras ser verificados). Inmutables una vez generados.
 * 
 * Relaciones principales: Lista asociada fuertemente a la configuracion TwoFactorAuthentication.
 */
@Entity
@Table(name = "codigos_respaldo_2fa")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TwoFactorBackupCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_codigo")
    private Long idCodigo;

    @NotNull(message = "El usuario es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private User usuario;

    @NotBlank(message = "El hash del cÃ³digo es obligatorio")
    @Column(name = "codigo_hash", nullable = false, length = 255)
    private String codigoHash;

    @Builder.Default
    @Column(name = "usado", nullable = false)
    private Boolean usado = false;
}


