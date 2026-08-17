package uteq.edu.ec.artisync.service.shared;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;

/**
 * Traduce las excepciones que lanzan las funciones PL/pgSQL de {@code db/procs/}
 * (RAISE EXCEPTION ... USING ERRCODE = '...') a {@link ResponseStatusException}
 * con el mismo mensaje de negocio que antes lanzaba la capa de servicio en Java,
 * para que el cambio de JPA a procedimiento almacenado sea transparente para los
 * controladores y el frontend.
 *
 * Se apoya en {@link SQLException#getSQLState()} (JDBC estandar, en java.sql) en
 * vez de en org.postgresql.util.PSQLException, porque el driver de Postgres se
 * declara con scope "runtime" en pom.xml y no esta disponible en el classpath
 * de compilacion de este modulo.
 */
public final class StoredProcedureExceptionTranslator {

    private StoredProcedureExceptionTranslator() {
    }

    public static ResponseStatusException traducir(RuntimeException origen, HttpStatus porDefecto) {
        SQLException sql = buscarSQLException(origen);
        if (sql == null) {
            throw origen;
        }
        String mensaje = limpiarMensaje(sql.getMessage());
        HttpStatus status = switch (String.valueOf(sql.getSQLState())) {
            case "23505" -> HttpStatus.CONFLICT;
            case "23514", "22004", "23503" -> HttpStatus.BAD_REQUEST;
            case "P0002" -> HttpStatus.NOT_FOUND;
            default -> porDefecto;
        };
        return new ResponseStatusException(status, mensaje);
    }

    private static SQLException buscarSQLException(Throwable t) {
        while (t != null) {
            if (t instanceof SQLException sql) {
                return sql;
            }
            t = t.getCause();
        }
        return null;
    }

    private static String limpiarMensaje(String mensajeOriginal) {
        if (mensajeOriginal == null) {
            return "Error al ejecutar la operacion";
        }
        String primeraLinea = mensajeOriginal.split("\\R", 2)[0];
        return primeraLinea.replaceFirst("^ERROR:\\s*", "");
    }
}
