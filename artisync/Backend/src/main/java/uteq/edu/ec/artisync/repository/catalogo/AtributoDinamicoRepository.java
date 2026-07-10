package uteq.edu.ec.artisync.repository.catalogo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.catalogo.AtributoDinamico;

import java.util.Optional;

@Repository
public interface AtributoDinamicoRepository extends JpaRepository<AtributoDinamico, Long> {

    Optional<AtributoDinamico> findByNombreAtributoIgnoreCase(String nombreAtributo);

    boolean existsByNombreAtributoIgnoreCase(String nombreAtributo);
}
