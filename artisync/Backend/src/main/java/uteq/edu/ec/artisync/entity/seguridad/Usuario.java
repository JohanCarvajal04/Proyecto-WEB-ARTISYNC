package uteq.edu.ec.artisync.entity.seguridad;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad del modelo de dominio que representa Entidad principal de identidad, credenciales y estado (activo, bloqueado).
 * 
 * Ciclo de vida: Ciclo vital continuo (CREADO, ACTIVO, BANEADO, ELIMINADO_LOGICAMENTE). Nucleo del sistema.
 * 
 * Relaciones principales: Entidad apex del sistema. Todo registro transaccional desciende estructuralmente de ella.
 */
@Entity
@Table(name = "usuarios")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "nombres", nullable = false, length = 100)
    private String nombres;

    @Column(name = "apellidos", nullable = false, length = 100)
    private String apellidos;

    @Column(name = "correo", nullable = false, unique = true, length = 150)
    private String correo;

    @JsonIgnore
    @Column(name = "contrasena_hash", nullable = false, length = 255)
    private String contrasenaHash;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_pais")
    private Pais pais;

    @CreationTimestamp
    @Column(name = "fecha_registro", updatable = false)
    private LocalDateTime fechaRegistro;

    @UpdateTimestamp
    @Column(name = "actualizado_en")
    private LocalDateTime actualizadoEn;

    @Builder.Default
    @Column(name = "estado_cuenta", nullable = false)
    private Boolean estadoCuenta = true;

    @Column(name = "fecha_nacimiento")
    private LocalDate fechaNacimiento;

    @Column(name = "url_foto_perfil")
    private String urlFotoPerfil;
}


