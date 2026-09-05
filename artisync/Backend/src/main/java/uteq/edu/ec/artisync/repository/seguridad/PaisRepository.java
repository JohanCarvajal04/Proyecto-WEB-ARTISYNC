package uteq.edu.ec.artisync.repository.seguridad;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.seguridad.Pais;

import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Sort;

@Repository
public interface PaisRepository extends JpaRepository<Pais, Long> {
    Optional<Pais> findByNombrePais(String nombrePais);
    List<Pais> findByEstadoTrue(Sort sort);

    /**
     * Fase 3 concurrencia (docs/basedatos/PLAN-CONCURRENCIA-SP.md §4) -
     * fn_guardar_pais: crea (p_id_pais NULL) o renombra (p_id_pais con valor)
     * un pais, capturando unique_violation sobre el nombre en vez de una
     * comprobacion findByNombrePais no atomica (A9). Devuelve el id_pais
     * afectado.
     */
    @Procedure(procedureName = "fn_guardar_pais")
    Long guardarPais(
            @Param("p_id_pais") Long idPais,
            @Param("p_nombre_pais") String nombrePais);
}

