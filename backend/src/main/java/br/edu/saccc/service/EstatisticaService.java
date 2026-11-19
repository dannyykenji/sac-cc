package br.edu.saccc.service;

import br.edu.saccc.dto.DistribuicaoItem;
import br.edu.saccc.dto.EvolucaoItem;
import br.edu.saccc.dto.ResumoEstatisticas;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * UC10/RF44–RF55 — fonte ÚNICA de contabilização (nenhum controller monta conta própria).
 * Regras (defaults #N1/#N2, validáveis com a coordenação):
 * - Realizado: atendimento.realizado=true (status AUSENCIA nunca entra — RN12)
 * - Período: data AGENDADA (sem remarcação no MVP, equivale à data_realizacao)
 * - % resolução = resolvidos / realizados × 100 (cancelados e ausências fora do denominador — RN10/12)
 * SQL padrão H2/PostgreSQL (sem FILTER/PIVOT/To_char).
 */
@Service
public class EstatisticaService {

    private final JdbcTemplate jdbc;

    public EstatisticaService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Filtros comuns do bloco de atendimentos realizados. */
    private String filtrosAt(StringBuilder sql, List<Object> args, LocalDate de, LocalDate ate,
                             String modalidade, Long categoriaId, Long responsavelId) {
        if (de != null) { sql.append(" AND a.data >= ?"); args.add(java.sql.Date.valueOf(de)); }
        if (ate != null) { sql.append(" AND a.data <= ?"); args.add(java.sql.Date.valueOf(ate)); }
        if (modalidade != null) { sql.append(" AND a.modalidade = ?"); args.add(modalidade); }
        if (categoriaId != null) { sql.append(" AND a.categoria_id = ?"); args.add(categoriaId); }
        if (responsavelId != null) { sql.append(" AND t.responsavel_id = ?"); args.add(responsavelId); }
        return sql.toString();
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('CONSULTAR_ESTATISTICAS')")
    public ResumoEstatisticas resumo(LocalDate de, LocalDate ate, String modalidade,
                                     Long categoriaId, Long responsavelId) {
        List<Object> args = new ArrayList<>();
        String fAt = filtrosAt(new StringBuilder(), args, de, ate, modalidade, categoriaId, responsavelId);
        List<Object> argsAg = new ArrayList<>();
        StringBuilder fAg = new StringBuilder();
        if (de != null) { fAg.append(" AND a.data >= ?"); argsAg.add(java.sql.Date.valueOf(de)); }
        if (ate != null) { fAg.append(" AND a.data <= ?"); argsAg.add(java.sql.Date.valueOf(ate)); }
        if (modalidade != null) { fAg.append(" AND a.modalidade = ?"); argsAg.add(modalidade); }
        if (categoriaId != null) { fAg.append(" AND a.categoria_id = ?"); argsAg.add(categoriaId); }

        var sql = """
                SELECT
                  (SELECT COUNT(*) FROM atendimento t JOIN agendamento a ON a.id = t.agendamento_id
                     WHERE t.realizado = TRUE AND t.confirmado_aluno = TRUE AND t.status_resolucao <> 'AUSENCIA'%s) AS realizados,
                  (SELECT COUNT(*) FROM atendimento t JOIN agendamento a ON a.id = t.agendamento_id
                     WHERE t.realizado = TRUE AND t.confirmado_aluno = TRUE AND t.status_resolucao = 'RESOLVIDO'%s) AS resolvidos,
                  (SELECT COUNT(*) FROM atendimento t JOIN agendamento a ON a.id = t.agendamento_id
                     WHERE t.realizado = TRUE AND t.confirmado_aluno = TRUE AND t.status_resolucao = 'NAO_RESOLVIDO'%s) AS nao_resolvidos,
                  (SELECT COUNT(*) FROM atendimento t JOIN agendamento a ON a.id = t.agendamento_id
                     WHERE t.realizado = TRUE AND t.confirmado_aluno = TRUE AND t.status_resolucao = 'EM_TRATATIVA'%s) AS em_tratativa,
                  (SELECT COUNT(*) FROM agendamento a WHERE a.status = 'CANCELADO'%s) AS cancelados,
                  (SELECT COUNT(*) FROM agendamento a WHERE a.status = 'NAO_COMPARECEU'%s) AS ausencias
                """.formatted(fAt, fAt, fAt, fAt, fAg, fAg);

        // fAt aparece 4× no SQL e fAg 2× → 4 cópias de args + 2 de argsAg (mesma ordem dos placeholders)
        var all = new ArrayList<>(args);
        all.addAll(args);
        all.addAll(args);
        all.addAll(args);
        all.addAll(argsAg);
        all.addAll(argsAg);

        var r = jdbc.queryForMap(sql, all.toArray());
        long realizados = ((Number) r.get("REALIZADOS")).longValue();
        long resolvidos = ((Number) r.get("RESOLVIDOS")).longValue();
        long naoResolvidos = ((Number) r.get("NAO_RESOLVIDOS")).longValue();
        long emTratativa = ((Number) r.get("EM_TRATATIVA")).longValue();
        long cancelados = ((Number) r.get("CANCELADOS")).longValue();
        long ausencias = ((Number) r.get("AUSENCIAS")).longValue();
        double pct = realizados == 0 ? 0.0 : Math.round(resolvidos * 1000.0 / realizados) / 10.0;

        return new ResumoEstatisticas(realizados, resolvidos, naoResolvidos, emTratativa,
                pct, cancelados, ausencias, de, ate);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('CONSULTAR_ESTATISTICAS')")
    public List<EvolucaoItem> evolucao(LocalDate de, LocalDate ate, String modalidade,
                                       Long categoriaId, Long responsavelId) {
        List<Object> args = new ArrayList<>();
        String f = filtrosAt(new StringBuilder(), args, de, ate, modalidade, categoriaId, responsavelId);
        var sql = """
                SELECT EXTRACT(YEAR FROM a.data) AS ano, EXTRACT(MONTH FROM a.data) AS mes,
                       COUNT(*) AS realizados,
                       SUM(CASE WHEN t.status_resolucao = 'RESOLVIDO' THEN 1 ELSE 0 END) AS resolvidos
                FROM atendimento t JOIN agendamento a ON a.id = t.agendamento_id
                WHERE t.realizado = TRUE AND t.confirmado_aluno = TRUE AND t.status_resolucao <> 'AUSENCIA'%s
                GROUP BY EXTRACT(YEAR FROM a.data), EXTRACT(MONTH FROM a.data)
                ORDER BY 1, 2
                """.formatted(f);
        return jdbc.query(sql, (rs, i) -> new EvolucaoItem(
                rs.getInt("ANO"), rs.getInt("MES"),
                rs.getLong("REALIZADOS"), rs.getLong("RESOLVIDOS")),
                args.toArray());
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('CONSULTAR_ESTATISTICAS')")
    public List<DistribuicaoItem> distribuicao(String dimensao, LocalDate de, LocalDate ate,
                                               String modalidade, Long categoriaId, Long responsavelId) {
        boolean porCategoria = "CATEGORIA".equalsIgnoreCase(dimensao);
        String dim = porCategoria
                ? "COALESCE(c.nome, 'Sem categoria')"
                : "a.modalidade";
        String join = porCategoria
                ? " LEFT JOIN categoria_atendimento c ON c.id = a.categoria_id"
                : "";
        List<Object> args = new ArrayList<>();
        String f = filtrosAt(new StringBuilder(), args, de, ate, modalidade, categoriaId, responsavelId);
        var sql = """
                SELECT %s AS chave, COUNT(*) AS total
                FROM atendimento t JOIN agendamento a ON a.id = t.agendamento_id%s
                WHERE t.realizado = TRUE AND t.confirmado_aluno = TRUE AND t.status_resolucao <> 'AUSENCIA'%s
                GROUP BY %s
                ORDER BY 2 DESC
                """.formatted(dim, join, f, dim);
        return jdbc.query(sql, (rs, i) -> new DistribuicaoItem(rs.getString("CHAVE"), rs.getLong("TOTAL")),
                args.toArray());
    }
}
