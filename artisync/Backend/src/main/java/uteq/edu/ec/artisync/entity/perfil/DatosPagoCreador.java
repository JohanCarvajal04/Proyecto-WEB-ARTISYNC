package uteq.edu.ec.artisync.entity.perfil;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;
import uteq.edu.ec.artisync.entity.seguridad.User;

import java.time.LocalDateTime;

/**
 * Correo de PayPal donde un creador recibe sus retiros. Tabla separada de
 * {@link PerfilCreador} a propósito: ese perfil es público (se sirve sin auth
 * en GET /api/v1/perfiles/*), así que un dato de cobro no debe vivir ahí por
 * el riesgo de que un futuro DTO lo exponga por descuido.
 */
@Entity
@Table(name = "datos_pago_creador")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DatosPagoCreador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_datos_pago")
    private Long idDatosPago;

    @NotNull(message = "El usuario es obligatorio")
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_usuario", nullable = false, unique = true)
    private User usuario;

    @NotBlank(message = "El correo de PayPal es obligatorio")
    @Email(message = "El correo de PayPal no tiene un formato válido")
    @Size(max = 150, message = "El correo de PayPal no puede superar los 150 caracteres")
    @Column(name = "correo_paypal", nullable = false, length = 150)
    private String correoPaypal;

    @UpdateTimestamp
    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;
}
