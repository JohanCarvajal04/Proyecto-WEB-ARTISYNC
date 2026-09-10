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

    /** REQ-NF-018: ¿la etapa actual de un pedido es la etapa final de su flujo? Usado para bloquear la supresión de datos mientras el pedido sigue en curso. */
    boolean existsByFlujoIdFlujoAndEtapaIdEtapaAndEsEtapaFinalTrue(Long idFlujo, Long idEtapa);

    boolean existsByFlujoIdFlujoAndNumeroOrden(Long idFlujo, Integer numeroOrden);

    void deleteByFlujoIdFlujo(Long idFlujo);
}
