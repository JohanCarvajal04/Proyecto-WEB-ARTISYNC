package uteq.edu.ec.artisync.repository.communication;

/**
 * Invocación de {@code fn_registrar_infraccion} vía JDBC directo, fuera del mecanismo de
 * {@code @Query} de Spring Data JPA (ver {@link ViolationRepositoryImpl}).
 */
public interface ViolationRepositoryCustom {

    /**
     * REQ-F-015 - fn_registrar_infraccion: inserta la infraccion, cuenta el total en 30 dias y
     * suspende la cuenta al llegar a 3. Devuelve JSONB serializado como texto.
     * @param idUsuario el identificador de usuario
     * @param idPedido el identificador de pedido
     * @param mensajeOriginal el mensaje de original
     * @param patronDetectado el patron detectado
     * @return el resultado en forma de texto
     */
    String registerViolation(Long idUsuario, Long idPedido, String mensajeOriginal, String patronDetectado);
}
