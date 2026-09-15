package uteq.edu.ec.artisync.repository.social;

/**
 * Invocaciones de {@code fn_seleccionar_ganadores_sorteo} vía JDBC directo, fuera del
 * mecanismo de {@code @Query} de Spring Data JPA (ver {@link RaffleRepositoryImpl}).
 */
public interface RaffleRepositoryCustom {

    /**
     * REQ-F-023 - fn_seleccionar_ganadores_sorteo: sortea ganadores y actualiza
     * participantes+sorteo en bloque. Devuelve JSONB serializado como texto.
     */
    String seleccionarGanadores(Long idSorteo);
}
