package uteq.edu.ec.artisync.repository.pedido;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.pedido.EtapaFlujo;

import java.util.Optional;

@Repository
public interface EtapaFlujoRepository extends JpaRepository<EtapaFlujo, Long> {

    Optional<EtapaFlujo> findByNombreEtapa(String nombreEtapa);

    boolean existsByNombreEtapa(String nombreEtapa);
}
