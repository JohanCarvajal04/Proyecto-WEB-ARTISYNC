package uteq.edu.ec.artisync.repository.sistema;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.sistema.RespaldoPolitica;

@Repository
public interface RespaldoPoliticaRepository extends JpaRepository<RespaldoPolitica, Integer> {
}
