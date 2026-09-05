package uteq.edu.ec.artisync.repository.seguridad;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.seguridad.CodigoRespaldo2Fa;

@Repository
public interface CodigoRespaldo2FaRepository extends JpaRepository<CodigoRespaldo2Fa, Long> {

    // CR-02 (revision de codigo): findByUsuarioIdUsuarioAndUsadoFalse y
    // deleteByUsuarioIdUsuario vivian aqui hasta el refactor de concurrencia.
    // Eran, respectivamente, la mitad del patron read-modify-write que
    // permitia consumir dos veces el mismo codigo de respaldo (bypass de 2FA,
    // A1) y el borrado sin revocacion previa (A6) que sustituyo
    // consumirCodigoRespaldo/fn_consumir_codigo_respaldo_2fa mas abajo. Se
    // eliminan -no se dejan como codigo muerto- para que nadie los reintroduzca
    // sin darse cuenta de que reabren esas dos anomalias.

    /**
     * Fase 1 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §2) -
     * fn_consumir_codigo_respaldo_2fa: UPDATE atomico ({@code WHERE usado = FALSE})
     * que consume un codigo de respaldo una sola vez, eliminando la actualizacion
     * perdida del patron anterior (SELECT de todos los codigos + comparacion en
     * Java + save()). Devuelve TRUE solo para el primer llamante concurrente.
     */
    @Procedure(procedureName = "fn_consumir_codigo_respaldo_2fa")
    Boolean consumirCodigoRespaldo(
            @Param("p_id_usuario") Long idUsuario,
            @Param("p_codigo_hash") String codigoHash);
}

