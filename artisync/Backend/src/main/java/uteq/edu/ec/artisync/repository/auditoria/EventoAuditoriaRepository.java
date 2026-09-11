package uteq.edu.ec.artisync.repository.auditoria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.auditoria.EventoAuditoria;

import java.util.List;


/**
 * Repositorio de acceso a datos para la entidad de dominio {@link EventoAuditoria}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Contiene consultas personalizadas (JPQL/Nativas) mediante @Query para resolver proyecciones complejas, agregaciones o evitar el problema N+1 (FETCH JOIN).
 */
@Repository
public interface EventoAuditoriaRepository
        extends JpaRepository<EventoAuditoria, Long>, JpaSpecificationExecutor<EventoAuditoria> {

    @Query("SELECT DISTINCT e.accionAuditoria FROM EventoAuditoria e ORDER BY e.accionAuditoria")
    List<String> listarAccionesDistintas();
}


