package uteq.edu.ec.artisync.entity.sistema;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity
@Table(name = "respaldos_bd")
@Getter
@Setter
public class RespaldoBd {

    public enum TipoRespaldo { AUTOMATICO, MANUAL }
    public enum CategoriaRespaldo { FULL, DIARIO }
    public enum EstadoRespaldo { EN_PROGRESO, COMPLETADO, FALLIDO }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long idRespaldo;

    @Column(nullable = false, unique = true)
    private String nombreArchivo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoRespaldo tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CategoriaRespaldo categoria;

    @Column(nullable = false)
    private Long tamanoBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoRespaldo estado = EstadoRespaldo.COMPLETADO;

    @Column(columnDefinition = "TEXT")
    private String mensajeError;

    private String creadoPor;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    private LocalDateTime fechaExpiracion;

    private String hashSha256;
}
