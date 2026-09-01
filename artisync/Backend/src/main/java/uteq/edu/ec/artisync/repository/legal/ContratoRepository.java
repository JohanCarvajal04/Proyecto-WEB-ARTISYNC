package uteq.edu.ec.artisync.repository.legal;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.legal.Contrato;

import java.util.Optional;

@Repository
public interface ContratoRepository extends JpaRepository<Contrato, Long>, ContratoRepositoryCustom {

    Optional<Contrato> findByPedidoIdPedido(Long idPedido);

    /**
     * Igual que findById, pero con bloqueo pesimista de fila. Sin esto, la
     * firma dual (cliente y creador firman columnas distintas de la misma
     * fila) es vulnerable a "lost update": Contrato no tiene @Version ni
     * @DynamicUpdate, así que cada save() de Hibernate reescribe TODAS las
     * columnas mapeadas con el snapshot en memoria -- si ambas firmas llegan
     * casi al mismo tiempo, quien confirme segundo sobrescribe con NULL la
     * firma que el otro acababa de guardar, perdiéndola en silencio. El
     * bloqueo serializa las dos transacciones y fuerza a la segunda a releer
     * el estado ya confirmado por la primera antes de escribir.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Contrato c WHERE c.idContrato = :idContrato")
    Optional<Contrato> findByIdParaFirmar(@Param("idContrato") Long idContrato);
}
