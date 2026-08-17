package uteq.edu.ec.artisync.repository.perfil;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.perfil.PortafolioItem;

import java.util.List;

@Repository
public interface PortafolioItemRepository extends JpaRepository<PortafolioItem, Long> {

    List<PortafolioItem> findByPortafolioIdPortafolioOrderByFechaSubidaDesc(Long idPortafolio);

    long countByPortafolioIdPortafolio(Long idPortafolio);
}
