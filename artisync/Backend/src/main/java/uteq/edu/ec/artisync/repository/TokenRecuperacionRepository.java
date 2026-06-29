package uteq.edu.ec.artisync.repository;

import uteq.edu.ec.artisync.model.seguridad.TokenRecuperacion;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface TokenRecuperacionRepository extends JpaRepository<TokenRecuperacion, Integer> {
    Optional<TokenRecuperacion> findByHashTokenAndUsadoFalse(String hashToken);
}
