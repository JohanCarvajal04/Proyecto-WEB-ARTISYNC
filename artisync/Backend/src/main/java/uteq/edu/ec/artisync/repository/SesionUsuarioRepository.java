package uteq.edu.ec.artisync.repository;

import uteq.edu.ec.artisync.model.seguridad.SesionUsuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

public interface SesionUsuarioRepository extends JpaRepository<SesionUsuario, Integer> {
    Optional<SesionUsuario> findByTokenJwt(String tokenJwt);

    @Modifying
    @Transactional
    @Query("DELETE FROM SesionUsuario s WHERE s.usuario.idUsuario = :idUsuario")
    void deleteAllByUsuarioId(Long idUsuario);

    boolean existsByTokenJwt(String tokenJwt);
}
