package uteq.edu.ec.artisync.repository.comunicacion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.comunicacion.NotificacionSistema;

@Repository
public interface NotificacionSistemaRepository extends JpaRepository<NotificacionSistema, Long> {

    Page<NotificacionSistema> findByUsuarioIdUsuarioOrderByFechaEmisionDesc(Long idUsuario, Pageable pageable);

    long countByUsuarioIdUsuarioAndEstaLeidaFalse(Long idUsuario);

    @Modifying
    @Query("UPDATE NotificacionSistema n SET n.estaLeida = true WHERE n.usuario.idUsuario = :idUsuario AND n.estaLeida = false")
    int marcarTodasLeidas(Long idUsuario);
}
