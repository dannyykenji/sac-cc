package br.edu.saccc.dto;

import java.time.LocalDate;

/** RF44–RF47 + RF55: KPIs consolidados de um período com filtros. */
public record ResumoEstatisticas(
        long totalRealizados,
        long totalResolvidos,
        long totalNaoResolvidos,
        long totalEmTratativa,
        double percentualResolucao,
        long totalCancelamentos,
        long totalAusencias,
        LocalDate periodoDe,
        LocalDate periodoAte) {
}
