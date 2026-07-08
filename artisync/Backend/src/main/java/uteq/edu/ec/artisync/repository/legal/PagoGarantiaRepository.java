package uteq.edu.ec.artisync.repository.legal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.legal.PagoGarantia;

import java.util.Optional;

@Repository
public interface PagoGarantiaRepository extends JpaRepository<PagoGarantia, Long> {

    Optional<PagoGarantia> findByContratoIdContrato(Long idContrato);

    Optional<PagoGarantia> findByIdOrdenPaypal(String idOrdenPaypal);
}
