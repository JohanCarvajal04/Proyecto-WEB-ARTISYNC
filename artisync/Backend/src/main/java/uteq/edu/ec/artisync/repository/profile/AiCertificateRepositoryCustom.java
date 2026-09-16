package uteq.edu.ec.artisync.repository.profile;

import java.util.List;

/**
 * Invocación de {@code fn_listar_cola_verificacion} vía JDBC directo, fuera del mecanismo de
 * {@code @Query} de Spring Data JPA (ver {@link AiCertificateRepositoryImpl}).
 */
public interface AiCertificateRepositoryCustom {

    /**
     * Cola de verificación de identidad (certificados de IA pendientes de revisión por un
     * moderador), filtrada por estado y paginada manualmente.
     * @param estado el estado
     * @param limite el limite
     * @param offset el offset
     * @return la lista de VerificationQueueProjection encontrados
     */
    List<VerificationQueueProjection> listQueue(String estado, int limite, int offset);
}
