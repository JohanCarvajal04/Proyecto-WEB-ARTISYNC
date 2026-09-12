package uteq.edu.ec.artisync.repository.legal;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.legal.Contract;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link Contract}.
 *
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 *
 * Responsabilidad de consultas: Contiene consultas personalizadas (JPQL/Nativas) mediante @Query para resolver proyecciones complejas, agregaciones o evitar el problema N+1 (FETCH JOIN).
 */
@Repository
public interface ContractRepository extends JpaRepository<Contract, Long>, ContractRepositoryCustom {

    /** El contrato de un pedido (relación 1:1), si existe. */
    Optional<Contract> findByPedidoIdPedido(Long idPedido);

    /** REQ-NF-018: contratos donde el usuario participa como cliente, para evaluar el impedimento legal de supresión. */
    List<Contract> findByPedidoUsuarioClienteIdUsuario(Long idUsuario);

    /** REQ-NF-018: contratos donde el usuario participa como creador (vía servicio -> perfil -> usuario). */
    List<Contract> findByPedidoServicioPerfilUsuarioIdUsuario(Long idUsuario);

    /** REQ-NF-020: contratos ya firmados por ambas partes (con hash de contenido calculado), para el barrido nocturno. */
    List<Contract> findByHashContenidoIsNotNull();

    /**
     * Igual que findById, pero con bloqueo pesimista de fila. Sin esto, la
     * firma dual (cliente y creador firman columnas distintas de la misma
     * fila) es vulnerable a "lost update": Contract no tiene {@code @Version} ni
     * {@code @DynamicUpdate}, así que cada save() de Hibernate reescribe TODAS las
     * columnas mapeadas con el snapshot en memoria -- si ambas firmas llegan
     * casi al mismo tiempo, quien confirme segundo sobrescribe con NULL la
     * firma que el otro acababa de guardar, perdiéndola en silencio. El
     * bloqueo serializa las dos transacciones y fuerza a la segunda a releer
     * el estado ya confirmado por la primera antes de escribir.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Contract c WHERE c.idContrato = :idContrato")
    Optional<Contract> findByIdParaFirmar(@Param("idContrato") Long idContrato);
}
