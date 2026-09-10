package uteq.edu.ec.artisync.dto.peticion.sistema;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.scheduling.support.CronExpression;

public record PeticionActualizarPoliticaRespaldo(
    @NotBlank(message = "La expresión cron del respaldo full es requerida")
    String cronFull,

    @NotNull(message = "Los días de retención del respaldo full son requeridos")
    @Min(value = 0, message = "La retención no puede ser negativa")
    @Max(value = 365, message = "La retención máxima es de 1 año (365 días)")
    Integer retencionDiasFull,

    @NotBlank(message = "La expresión cron del respaldo diario es requerida")
    String cronDiario,

    @NotNull(message = "Los días de retención del respaldo diario son requeridos")
    @Min(value = 0, message = "La retención no puede ser negativa")
    @Max(value = 365, message = "La retención máxima es de 1 año (365 días)")
    Integer retencionDiasDiario
) {
    @AssertTrue(message = "La expresión cron del respaldo full no es válida (formato: segundo minuto hora día-mes mes día-semana)")
    public boolean isCronFullValido() {
        return cronFull == null || cronFull.isBlank()
                || CronExpression.isValidExpression(cronFull.trim());
    }

    @AssertTrue(message = "La expresión cron del respaldo diario no es válida (formato: segundo minuto hora día-mes mes día-semana)")
    public boolean isCronDiarioValido() {
        return cronDiario == null || cronDiario.isBlank()
                || CronExpression.isValidExpression(cronDiario.trim());
    }
}
