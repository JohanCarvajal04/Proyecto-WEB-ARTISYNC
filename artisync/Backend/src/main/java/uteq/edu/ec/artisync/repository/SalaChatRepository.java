package uteq.edu.ec.artisync.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.legal.SalaChat;

import java.util.Optional;

@Repository
public interface SalaChatRepository extends JpaRepository<SalaChat, Long> {

    Optional<SalaChat> findByPedidoIdPedido(Long idPedido);
}
