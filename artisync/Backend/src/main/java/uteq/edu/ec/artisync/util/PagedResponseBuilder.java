package uteq.edu.ec.artisync.util;

import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public class PagedResponseBuilder {

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
     * un unico lote (p. ej. UsuarioMapper.toUserResponseList) en vez de
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
