package uteq.edu.ec.artisync.repository.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.security.TwoFactorAuthentication;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link TwoFactorAuthentication}.
 *
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 *
 * Responsabilidad de consultas: Contiene consultas personalizadas (JPQL/Nativas) mediante @Query para resolver proyecciones complejas, agregaciones o evitar el problema N+1 (FETCH JOIN).
 */
@Repository
public interface TwoFactorAuthenticationRepository extends JpaRepository<TwoFactorAuthentication, Long>, TwoFactorAuthenticationRepositoryCustom {

    /**
     * Configuración 2FA de un usuario, si la tiene.
     * @param idUsuario el identificador de usuario
     * @return un Optional con TwoFactorAuthentication si existe, vacio en caso contrario
     */
    Optional<TwoFactorAuthentication> findByUsuarioIdUsuario(Long idUsuario);

    /**
     * Configuración 2FA de un usuario, identificado por su correo.
     * @param correo el correo
     * @return un Optional con TwoFactorAuthentication si existe, vacio en caso contrario
     */
    Optional<TwoFactorAuthentication> findByUsuarioCorreo(String correo);

    /**
     * Fase 2 rendimiento (docs/basedatos/PLAN-CONCURRENCIA-SP.md §8) - carga en
     * UNA sola consulta el estado de 2FA de TODOS los usuarios de
     * {@code idsUsuario}. La usa UserMapper.toUserResponseList para eliminar
     * el N+1 de invocar findByUsuarioIdUsuario por cada fila de una pagina de
     * administracion de usuarios.
     * @param idsUsuario el ids usuario
     * @return la lista de TwoFactorAuthentication encontrados
     */
    List<TwoFactorAuthentication> findByUsuarioIdUsuarioIn(List<Long> idsUsuario);
}


