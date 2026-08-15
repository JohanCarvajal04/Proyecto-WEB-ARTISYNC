package uteq.edu.ec.artisync.entity.seguridad;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "sesiones_usuario")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SesionUsuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sesion")
    private Long idSesion;

    @NotNull(message = "El usuario es obligatorio")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    // §2.5 / OBS-AUTO-06: se guarda unicamente el jti (identificador del token),
    // nunca el JWT completo — una lectura de esta tabla ya no entrega tokens
    // utilizables. Ver V8__sesiones_usuario_jti.sql.
    @NotBlank(message = "El jti de la sesion es obligatorio")
    @Column(name = "jti", nullable = false, unique = true, length = 36)
    private String jti;

    @Size(max = 45, message = "La direccion IP no puede superar los 45 caracteres")
    @Column(name = "direccion_ip", length = 45)
    private String direccionIp;

    @CreationTimestamp
    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @NotNull(message = "La fecha de expiracion es obligatoria")
    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;
}
