package uteq.edu.ec.artisync.repository.seguridad;

/**
 * Repositorio de acceso a datos para la entidad de dominio {@link SesionRevocadaProyeccion}.
 * 
 * Propósito: Actúa como capa de abstracción (DAO) gestionada por Spring Data JPA 
 * para realizar operaciones CRUD sobre la tabla correspondiente en la base de datos.
 * 
 * Responsabilidad de consultas: Delega la responsabilidad de persistencia a los métodos estándar y autogenerados por convención (Derived Queries) de Spring Data.
 */
public interface SesionRevocadaProyeccion {
    String getJti();
    Integer getSegundosRestantes();
}

