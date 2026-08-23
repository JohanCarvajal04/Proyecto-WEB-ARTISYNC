package uteq.edu.ec.artisync.entity.auditoria;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Bitácora inmutable de eventos (REQ-NF-013). Ver V15__modulo_auditoria.sql
 * para la tabla, el trigger de inmutabilidad y los GRANTs restringidos.
 *
 * fechaEvento NO lleva @CreationTimestamp: la estampa AspectoAuditoria en el
 * hilo de la petición original, antes de que la escritura pase a su propia
 * transacción REQUIRES_NEW, para que el orden temporal de los eventos refleje
 * el orden real en que ocurrieron y no el orden en que se persistieron.
 *
 * idUsuarioActor es un Long plano, sin @ManyToOne: no hay FK a usuarios a
 * propósito (ver cabecera de la migración) para que borrar un usuario nunca
 * dispare un UPDATE que el trigger de inmutabilidad rechazaría.
 */
@Entity
@Table(name = "auditoria_eventos")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EventoAuditoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento_auditoria")
    private Long idEventoAuditoria;

    @Column(name = "fecha_evento", nullable = false)
    private LocalDateTime fechaEvento;

    @Column(name = "id_usuario_actor")
    private Long idUsuarioActor;

    @Column(name = "correo_actor", nullable = false, length = 150)
    private String correoActor;

    @Column(name = "modulo_auditoria", nullable = false, length = 20)
    private String moduloAuditoria;

    @Column(name = "accion_auditoria", nullable = false, length = 60)
    private String accionAuditoria;

    @Column(name = "resultado_evento", nullable = false, length = 10)
    private String resultadoEvento;

    @Column(name = "entidad_afectada", length = 60)
    private String entidadAfectada;

    @Column(name = "id_entidad_afectada")
    private Long idEntidadAfectada;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detalle_cambio", columnDefinition = "jsonb")
    private Map<String, Object> detalleCambio;

    @Column(name = "mensaje_error", length = 500)
    private String mensajeError;

    @Column(name = "direccion_ip", length = 45)
    private String direccionIp;

    @Column(name = "agente_usuario")
    private String agenteUsuario;

    @Column(name = "metodo_http", length = 10)
    private String metodoHttp;

    @Column(name = "ruta_solicitud")
    private String rutaSolicitud;

    @Column(name = "duracion_ms")
    private Integer duracionMs;
}
