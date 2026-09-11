package uteq.edu.ec.artisync.entity.respaldo;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Metadatos de una ejecución de respaldo (FULL o INCREMENTAL). El archivo en
 * sí vive en disco (rutaArchivo, dentro de BackupProperties.rutaBase), esta
 * fila es el registro de qué se generó, cuándo, y con qué resultado.
 */
@Entity
@Table(name = "respaldos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Respaldo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_respaldo")
    private Long idRespaldo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_respaldo", nullable = false, length = 20)
    private TipoRespaldo tipoRespaldo;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_respaldo", nullable = false, length = 20)
    private EstadoRespaldo estadoRespaldo = EstadoRespaldo.EN_PROGRESO;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen", nullable = false, length = 20)
    private OrigenRespaldo origen;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_programacion")
    private RespaldoProgramacion programacion;

    /**
     * Long plano (no @ManyToOne autorreferenciado): se usa casi siempre en
     * consultas de existencia/conteo (retención, guardia de eliminación), no
     * para navegar el grafo de entidades.
     */
    @Column(name = "id_respaldo_full_base")
    private Long idRespaldoFullBase;

    @Column(name = "nombre_archivo", length = 255)
    private String nombreArchivo;

    @Column(name = "ruta_archivo", length = 500)
    private String rutaArchivo;

    @Column(name = "tamano_bytes")
    private Long tamanoBytes;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "duracion_ms")
    private Integer duracionMs;

    @Column(name = "mensaje_error", length = 500)
    private String mensajeError;

    @Column(name = "correo_solicitante", nullable = false, length = 150)
    private String correoSolicitante;

    /** Corte usado por el INCREMENTAL (fecha del último respaldo de la cadena). */
    @Column(name = "fecha_desde_incremental")
    private LocalDateTime fechaDesdeIncremental;
}
