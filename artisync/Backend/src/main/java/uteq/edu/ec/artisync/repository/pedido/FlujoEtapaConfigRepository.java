package uteq.edu.ec.artisync.repository.pedido;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.pedido.FlujoEtapaConfig;

import java.util.List;

@Repository
public interface FlujoEtapaConfigRepository extends JpaRepository<FlujoEtapaConfig, Long> {

    List<FlujoEtapaConfig> findByFlujoIdFlujoOrderByNumeroOrdenAsc(Long idFlujo);

    List<FlujoEtapaConfig> findByFlujoIdFlujoAndNumeroOrdenGreaterThanOrderByNumeroOrdenAsc(Long idFlujo, Integer numeroOrden);

    boolean existsByFlujoIdFlujoAndEtapaIdEtapa(Long idFlujo, Long idEtapa);

    void deleteByFlujoIdFlujo(Long idFlujo);
}
