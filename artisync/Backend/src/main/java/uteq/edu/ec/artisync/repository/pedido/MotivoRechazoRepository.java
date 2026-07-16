package uteq.edu.ec.artisync.repository.pedido;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.pedido.MotivoRechazo;

@Repository
public interface MotivoRechazoRepository extends JpaRepository<MotivoRechazo, Long> {

    boolean existsByDescripcionMotivo(String descripcionMotivo);
}
