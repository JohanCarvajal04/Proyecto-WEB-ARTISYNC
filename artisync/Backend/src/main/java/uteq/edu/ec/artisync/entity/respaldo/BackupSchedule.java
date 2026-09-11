package uteq.edu.ec.artisync.entity.respaldo;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "respaldo_programaciones")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackupSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_programacion")
    private Long idProgramacion;

    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_respaldo", nullable = false, length = 20)
    private BackupType tipoRespaldo;

    @Column(name = "expresion_cron", nullable = false, length = 100)
    private String expresionCron;

    @Column(name = "retencion_dias", nullable = false)
    private Integer retencionDias;

    @Builder.Default
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "proxima_ejecucion", nullable = false)
    private LocalDateTime proximaEjecucion;

    @Column(name = "ultima_ejecucion")
    private LocalDateTime ultimaEjecucion;

    @Column(name = "creado_por", nullable = false, length = 150)
    private String creadoPor;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    /** Fijado por el servicio en cada mutación, sin trigger de BD (ver V44). */
    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;
}
