package uteq.edu.ec.artisync.repository.security;

/**
 * Invocación de {@code fn_guardar_pais} vía JDBC directo, fuera del mecanismo de
 * {@code @Query} de Spring Data JPA (ver {@link CountryRepositoryImpl}).
 */
public interface CountryRepositoryCustom {

    /**
     * fn_guardar_pais: crea ({@code idPais} null) o renombra ({@code idPais} con valor) un país,
     * capturando unique_violation sobre el nombre. Devuelve el id_pais afectado.
     * @param idPais el identificador de pais
     * @param nombrePais el nombre de pais
     * @return el valor numerico calculado
     */
    Long guardarPais(Long idPais, String nombrePais);
}
