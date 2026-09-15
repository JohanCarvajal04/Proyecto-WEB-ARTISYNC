package uteq.edu.ec.artisync.repository.support;

import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.sql.Array;

/**
 * Crea un {@link Array} de Postgres ({@code text[]}) a partir de un {@code String[]}, para
 * pasarlo como parámetro con {@link NamedParameterJdbcTemplate} al invocar funciones SQL que
 * declaran un parámetro {@code TEXT[]}. Evita el bug de Hibernate 7.4.x con {@code @Procedure}
 * y retorno no-void documentado en {@code docs/basedatos/CATALOGO-SP.md} §14 sin repetir la
 * creación del array en cada repositorio afectado.
 */
public final class PgArrays {

    private PgArrays() {
    }

    /**
     * @param jdbcTemplate plantilla usada para tomar la conexión JDBC que crea el array
     * @param valores valores del array
     * @return un {@code java.sql.Array} de tipo {@code text} con esos valores
     */
    public static Array textArray(NamedParameterJdbcTemplate jdbcTemplate, String[] valores) {
        return jdbcTemplate.getJdbcOperations()
                .execute((ConnectionCallback<Array>) con -> con.createArrayOf("text", valores));
    }
}
