package uteq.edu.ec.artisync.repository.seguridad;

/**
 * Proyección de fn_revocar_sesiones_usuario y fn_cambiar_estado_cuenta
 * (docs/basedatos/PLAN-CONCURRENCIA-SP.md §5); Spring Data mapea snake_case -> getters.
 * Una fila por sesión efectivamente borrada en el motor, con el tiempo de vida
 * que le quedaba en el momento de la revocación.
 */
public interface SesionRevocadaProyeccion {
    String getJti();
    Integer getSegundosRestantes();
}
