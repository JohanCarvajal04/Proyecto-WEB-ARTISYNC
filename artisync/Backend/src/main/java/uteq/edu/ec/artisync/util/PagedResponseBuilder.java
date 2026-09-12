package uteq.edu.ec.artisync.util;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public class PagedResponseBuilder {

    /**
     * Convierte una {@link Page} de Spring Data en un {@link PagedResponse},
     * sin transformar el contenido.
     *
     * @param page página de origen
     * @return el {@link PagedResponse} equivalente, con el mismo contenido
     */
    public static <T> PagedResponse<T> build(Page<T> page) {
        return PagedResponse.<T>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    /**
     * Convierte una {@link Page} en un {@link PagedResponse}, mapeando cada
     * elemento del contenido individualmente (una llamada a {@code mapper} por fila).
     *
     * @param page página de origen
     * @param mapper función de mapeo elemento a elemento (p. ej. entidad → DTO)
     * @return el {@link PagedResponse} con el contenido ya mapeado
     */
    public static <T, R> PagedResponse<R> buildAndMap(Page<T> page, Function<T, R> mapper) {
        List<R> mappedContent = page.getContent().stream()
                .map(mapper)
                .toList();

        return PagedResponse.<R>builder()
                .content(mappedContent)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }

    /**
     * Variante de {@link #buildAndMap} que mapea el contenido de la pagina en
     * un unico lote (p. ej. UserMapper.toUserResponseList) en vez de
     * elemento a elemento. Uso: cuando el mapper hace consultas adicionales
     * por elemento (roles, permisos, flags), pasar la lista completa permite
     * batchearlas con IN (...) en vez de repetirlas por cada fila (N+1).
     */
    public static <T, R> PagedResponse<R> buildAndMapList(Page<T> page, Function<List<T>, List<R>> mapper) {
        List<R> mappedContent = mapper.apply(page.getContent());

        return PagedResponse.<R>builder()
                .content(mappedContent)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
