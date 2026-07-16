package uteq.edu.ec.artisync.repository.pedido;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.pedido.PlantillaContrato;

import java.util.Optional;

@Repository
public interface PlantillaContratoRepository extends JpaRepository<PlantillaContrato, Long> {

    Optional<PlantillaContrato> findFirstByOrderByIdPlantillaDesc();

    Optional<PlantillaContrato> findByVersionLegal(String versionLegal);
}
