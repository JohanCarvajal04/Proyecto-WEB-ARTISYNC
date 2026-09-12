package uteq.edu.ec.artisync.repository.legal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.legal.Message;

import java.util.List;


/**
 * Repositorio de acceso a datos para la entidad de dominio {@link Message}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /** Historial de mensajes de una sala de chat, en el orden en que se enviaron. */
    List<Message> findBySalaIdSalaOrderByFechaHoraEnvioAsc(Long idSala);
}


