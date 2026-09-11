package uteq.edu.ec.artisync.repository.pedido;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.pedido.Pedido;

import java.util.List;


/**
 * Repositorio de acceso a datos para la entidad de dominio {@link Pedido}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
@Repository
public interface PedidoRepository extends JpaRepository<Pedido, Long> {

    List<Pedido> findByUsuarioClienteIdUsuario(Long idUsuario);

    List<Pedido> findByServicioPerfilIdPerfil(Long idPerfil);

    List<Pedido> findByServicioPerfilUsuarioIdUsuario(Long idUsuario);
}


