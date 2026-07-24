package uteq.edu.ec.artisync.repository.legal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.legal.DocumentoAdjunto;

import java.util.List;

@Repository
public interface DocumentoAdjuntoRepository extends JpaRepository<DocumentoAdjunto, Long> {

    List<DocumentoAdjunto> findByMensajeIdMensaje(Long idMensaje);
}
