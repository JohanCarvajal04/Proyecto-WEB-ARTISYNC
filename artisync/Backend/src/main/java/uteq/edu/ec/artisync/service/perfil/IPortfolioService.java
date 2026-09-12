package uteq.edu.ec.artisync.service.perfil;

import uteq.edu.ec.artisync.dto.peticion.perfil.CreatePortfolioRequest;
import uteq.edu.ec.artisync.dto.peticion.perfil.UpdatePortfolioRequest;
import uteq.edu.ec.artisync.dto.respuesta.perfil.PortfolioResponse;

import java.util.List;

public interface IPortfolioService {

    /**
     * Crea el portafolio de un perfil de creador. Cada perfil admite un único portafolio.
     *
     * @param peticion         id del perfil y datos iniciales del portafolio
     * @param idUsuarioLogueado id del usuario autenticado, debe ser dueño del perfil indicado
     * @return el portafolio recién creado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el perfil indicado no existe
     * @throws uteq.edu.ec.artisync.exception.DuplicateResourceException si el perfil ya tiene un portafolio
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el solicitante no tiene permiso sobre el perfil indicado
     */
    PortfolioResponse createPortfolio(CreatePortfolioRequest peticion, Long idUsuarioLogueado);

    /**
     * Obtiene un portafolio por su id.
     *
     * @param idPortafolio id del portafolio
     * @return el portafolio encontrado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el portafolio no existe
     */
    PortfolioResponse getPortfolioById(Long idPortafolio);

    /**
     * Obtiene el portafolio asociado a un perfil de creador.
     *
     * @param idPerfil id del perfil de creador
     * @return el portafolio del perfil
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el perfil no tiene portafolio
     */
    PortfolioResponse getPortfolioByProfile(Long idPerfil);

    /**
     * Lista todos los portafolios registrados.
     *
     * @return todos los portafolios
     */
    List<PortfolioResponse> listPortfolios();

    /**
     * Actualiza los datos de un portafolio. Solo su dueño puede modificarlo.
     *
     * @param idPortafolio      id del portafolio a actualizar
     * @param peticion          campos a modificar
     * @param idUsuarioLogueado id del usuario autenticado, debe ser el dueño del portafolio
     * @return el portafolio ya actualizado
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el portafolio no existe
     * @throws uteq.edu.ec.artisync.exception.BusinessRuleException si el solicitante no es el dueño del portafolio
     */
    PortfolioResponse updatePortfolio(Long idPortafolio, UpdatePortfolioRequest peticion, Long idUsuarioLogueado);

    /**
     * Incrementa el contador de visitas de un portafolio.
     *
     * @param idPortafolio id del portafolio visitado
     * @param idUsuario    id del usuario visitante, o {@code null} si no hay sesión
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el portafolio no existe
     */
    void incrementarVisitas(Long idPortafolio, Long idUsuario);

    /**
     * Elimina un portafolio.
     *
     * @param idPortafolio id del portafolio a eliminar
     * @throws uteq.edu.ec.artisync.exception.ResourceNotFoundException si el portafolio no existe
     */
    void deletePortfolio(Long idPortafolio);
}
