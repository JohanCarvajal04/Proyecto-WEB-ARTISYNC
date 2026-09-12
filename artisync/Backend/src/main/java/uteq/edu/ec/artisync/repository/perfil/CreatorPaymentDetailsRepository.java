package uteq.edu.ec.artisync.repository.perfil;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.perfil.CreatorPaymentDetails;

import java.util.Optional;


/**
 * Repositorio de acceso a datos para la entidad de dominio {@link CreatorPaymentDetails}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
@Repository
public interface CreatorPaymentDetailsRepository extends JpaRepository<CreatorPaymentDetails, Long> {

    /** Datos de pago (retiro) de un creador, si los ha configurado. */
    Optional<CreatorPaymentDetails> findByUsuarioIdUsuario(Long idUsuario);
}


