package uteq.edu.ec.artisync.repository.comunicacion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uteq.edu.ec.artisync.entity.comunicacion.Seguidor;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeguidorRepository extends JpaRepository<Seguidor, Long> {

    Optional<Seguidor> findByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(Long idUsuario, Long idPerfil);

    boolean existsByUsuarioSeguidorIdUsuarioAndPerfilCreadorIdPerfil(Long idUsuario, Long idPerfil);

    List<Seguidor> findByPerfilCreadorIdPerfil(Long idPerfil);

    long countByPerfilCreadorIdPerfil(Long idPerfil);

    List<Seguidor> findByUsuarioSeguidorIdUsuario(Long idUsuario);

    @Procedure(procedureName = "fn_seguir_creador")
    Boolean ejecutarFnSeguirCreador(@Param("idUsuario") Long idUsuario, @Param("idPerfil") Long idPerfil);

    @Procedure(procedureName = "fn_dejar_de_seguir_creador")
    Boolean ejecutarFnDejarDeSeguirCreador(@Param("idUsuario") Long idUsuario, @Param("idPerfil") Long idPerfil);

    @Procedure(procedureName = "fn_es_seguidor")
    Boolean ejecutarFnEsSeguidor(@Param("idUsuario") Long idUsuario, @Param("idPerfil") Long idPerfil);

    @Procedure(procedureName = "fn_conteo_seguidores")
    Long ejecutarFnConteoSeguidores(@Param("idPerfil") Long idPerfil);
}


