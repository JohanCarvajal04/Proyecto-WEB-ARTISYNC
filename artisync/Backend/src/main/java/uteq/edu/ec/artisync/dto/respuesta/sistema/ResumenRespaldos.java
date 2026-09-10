package uteq.edu.ec.artisync.dto.respuesta.sistema;

public record ResumenRespaldos(
    long totalRespaldos,
    long totalAutomaticos,
    long totalManuales,
    long totalFull,
    long totalDiarios,
    String ultimoRespaldoFecha,
    String proximoFullEstimado,
    String proximoDiarioEstimado,
    String cronFull,
    int retencionDiasFull,
    String cronDiario,
    int retencionDiasDiario,
    String espacioTotalUsado,
    // Último respaldo AUTOMATICO exitoso de cada categoría, y si ya se pasó del
    // margen esperado según su propio cron (indicio de que el scheduler no
    // corrió — p.ej. el proceso estuvo apagado/dormido a esa hora).
    String ultimoFullAutomaticoFecha,
    String ultimoDiarioAutomaticoFecha,
    boolean fullAtrasado,
    boolean diarioAtrasado
) {}
