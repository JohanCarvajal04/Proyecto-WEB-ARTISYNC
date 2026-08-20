package uteq.edu.ec.artisync.repository.auditoria;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.auditoria.EventoAuditoria;

import java.util.List;

@Repository
public interface EventoAuditoriaRepository
        extends JpaRepository<EventoAuditoria, Long>, JpaSpecificationExecutor<EventoAuditoria> {

    @Query("SELECT DISTINCT e.accionAuditoria FROM EventoAuditoria e ORDER BY e.accionAuditoria")
    List<String> listarAccionesDistintas();
}
