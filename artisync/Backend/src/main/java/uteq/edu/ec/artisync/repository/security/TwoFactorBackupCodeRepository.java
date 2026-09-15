package uteq.edu.ec.artisync.repository.security;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.security.TwoFactorBackupCode;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link TwoFactorBackupCode}.
 *
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 *
 * Responsabilidad de consultas: Contiene consultas personalizadas (JPQL/Nativas) mediante @Query para resolver proyecciones complejas, agregaciones o evitar el problema N+1 (FETCH JOIN).
 */
@Repository
public interface TwoFactorBackupCodeRepository extends JpaRepository<TwoFactorBackupCode, Long>, TwoFactorBackupCodeRepositoryCustom {

    // CR-02 (revision de codigo): findByUsuarioIdUsuarioAndUsadoFalse y
    // deleteByUsuarioIdUsuario vivian aqui hasta el refactor de concurrencia.
    // Eran, respectivamente, la mitad del patron read-modify-write que
    // permitia consumir dos veces el mismo codigo de respaldo (bypass de 2FA,
    // A1) y el borrado sin revocacion previa (A6) que sustituyo
    // consumirCodigoRespaldo/fn_consumir_codigo_respaldo_2fa mas abajo. Se
    // eliminan -no se dejan como codigo muerto- para que nadie los reintroduzca
    // sin darse cuenta de que reabren esas dos anomalias.
}


