package uteq.edu.ec.artisync.repository.profile;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.math.BigDecimal;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Invoca {@code fn_listar_cola_verificacion} con {@link NamedParameterJdbcTemplate} en vez de
 * {@code @Query(nativeQuery = true)} (P6 de la guía del examen suspenso). Ver
 * {@code docs/basedatos/CATALOGO-SP.md} §14 para el bug de Hibernate que esto evita. La función
 * devuelve una tabla (id_certificado, id_usuario, nombre_usuario, tipo_documento, nombre_estado,
 * veredicto_ia, puntaje_confianza_ia, fecha_analisis) — columnas confirmadas contra el esquema
 * real vía {@code pg_get_functiondef}, no contra la migración V7 original: V21 renombró
 * id_perfil/nombre_creador a id_usuario/nombre_usuario. Se mapea fila a fila a
 * {@link VerificationQueueProjection} con un {@code RowMapper} explícito porque Spring Data no
 * puede resolver una interfaz-proyección de este tamaño contra una consulta JDBC directa.
 */
@RequiredArgsConstructor
public class AiCertificateRepositoryImpl implements AiCertificateRepositoryCustom {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    private record QueueRow(
            Long idCertificado, Long idUsuario, String nombreUsuario, String tipoDocumento,
            String nombreEstado, String veredictoIa, BigDecimal puntajeConfianzaIa,
            LocalDateTime fechaAnalisis) implements VerificationQueueProjection {

        /** @return el identificador del certificado */
        @Override
        public Long getIdCertificado() {
            return idCertificado;
        }

        /** @return el identificador del usuario dueño del certificado */
        @Override
        public Long getIdUsuario() {
            return idUsuario;
        }

        /** @return el nombre del usuario dueño del certificado */
        @Override
        public String getNombreUsuario() {
            return nombreUsuario;
        }

        /** @return el tipo de documento analizado */
        @Override
        public String getTipoDocumento() {
            return tipoDocumento;
        }

        /** @return el nombre del estado de verificación actual */
        @Override
        public String getNombreEstado() {
            return nombreEstado;
        }

        /** @return el veredicto emitido por el análisis de IA */
        @Override
        public String getVeredictoIa() {
            return veredictoIa;
        }

        /** @return el puntaje de confianza del análisis de IA */
        @Override
        public BigDecimal getPuntajeConfianzaIa() {
            return puntajeConfianzaIa;
        }

        /** @return la fecha en que se realizó el análisis de IA */
        @Override
        public LocalDateTime getFechaAnalisis() {
            return fechaAnalisis;
        }
    }

    /**
     * @param estado nombre del estado de verificación por el que filtrar, o {@code null} para no filtrar
     * @param limite máximo de filas a devolver
     * @param offset filas a saltar (paginación)
     * @return la cola de verificación filtrada y paginada
     */
    @Override
    public List<VerificationQueueProjection> listQueue(String estado, int limite, int offset) {
        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("estado", estado, Types.VARCHAR)
                .addValue("limite", limite)
                .addValue("offset", offset);
        return jdbcTemplate.query(
                "SELECT * FROM fn_listar_cola_verificacion(:estado, :limite, :offset)",
                params,
                (rs, rowNum) -> new QueueRow(
                        rs.getLong("id_certificado"),
                        rs.getLong("id_usuario"),
                        rs.getString("nombre_usuario"),
                        rs.getString("tipo_documento"),
                        rs.getString("nombre_estado"),
                        rs.getString("veredicto_ia"),
                        rs.getBigDecimal("puntaje_confianza_ia"),
                        rs.getTimestamp("fecha_analisis") == null
                                ? null : rs.getTimestamp("fecha_analisis").toLocalDateTime()));
    }
}
