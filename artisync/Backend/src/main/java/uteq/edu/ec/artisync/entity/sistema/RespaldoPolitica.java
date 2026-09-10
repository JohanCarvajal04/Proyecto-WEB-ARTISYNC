package uteq.edu.ec.artisync.entity.sistema;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "respaldos_politica")
@Getter
@Setter
public class RespaldoPolitica {

    @Id
    private Integer idPolitica = 1;

    @Column(nullable = false)
    private String cronFull = "0 0 3 * * SUN";

    @Column(nullable = false)
    private Integer retencionDiasFull = 90;

    @Column(nullable = false)
    private String cronDiario = "0 0 2 * * ?";

    @Column(nullable = false)
    private Integer retencionDiasDiario = 7;
}
