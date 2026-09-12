package uteq.edu.ec.artisync.util;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PagedResponse<T> {
    private List<T> content;
    @JsonProperty("number")
    private int pageNumber;
    private int pageSize;
    private long totalElements;
    private int totalPages;
    private boolean last;

    /**
     * Getter explícito (en vez de dejar que Lombok genere {@code getPageNumber()})
     * para que el JSON serializado use la clave {@code number}, igual convención
     * que {@code Page} de Spring Data.
     *
     * @return el número de página (0-index)
     */
    @JsonProperty("number")
    public int getNumber() {
        return pageNumber;
    }
}
