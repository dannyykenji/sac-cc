package br.edu.saccc.service;

import br.edu.saccc.model.ParametroSistema;
import br.edu.saccc.repository.ParametroSistemaRepository;
import org.springframework.stereotype.Service;

/** Leitura de parâmetros do sistema (RNF42) com defaults de segurança. */
@Service
public class ParametroService {

    public static final String ANTECEDENCIA_MINIMA = "ANTECEDENCIA_MINIMA_HORAS";
    public static final String PRAZO_CANCELAMENTO = "PRAZO_CANCELAMENTO_HORAS";
    public static final String LIMITE_FUTUROS = "LIMITE_AGENDAMENTOS_FUTUROS";

    private final ParametroSistemaRepository repository;

    public ParametroService(ParametroSistemaRepository repository) {
        this.repository = repository;
    }

    public long longo(String chave, long defaultValor) {
        return repository.findById(chave)
                .map(ParametroSistema::getValor)
                .map(v -> {
                    try { return Long.parseLong(v.trim()); }
                    catch (NumberFormatException e) { return null; }
                })
                .orElse(defaultValor);
    }

    public long antecedenciaMinimaHoras() { return longo(ANTECEDENCIA_MINIMA, 24); }
    public long prazoCancelamentoHoras() { return longo(PRAZO_CANCELAMENTO, 24); }
    public long limiteAgendamentosFuturos() { return longo(LIMITE_FUTUROS, 1); }

    /** String de parâmetro (ex.: LOCAL_PRESENCIAL_FIXO #N7). */
    public String texto(String chave, String defaultValor) {
        return repository.findById(chave)
                .map(ParametroSistema::getValor)
                .filter(v -> v != null && !v.isBlank())
                .orElse(defaultValor);
    }

    /** Booleano de parâmetro (ex.: CHAT_MODO_TESTE — valores true/1/sim). */
    public boolean booleano(String chave, boolean defaultValor) {
        return repository.findById(chave)
                .map(ParametroSistema::getValor)
                .map(v -> v != null && (v.trim().equalsIgnoreCase("true")
                        || v.trim().equals("1") || v.trim().equalsIgnoreCase("sim")))
                .orElse(defaultValor);
    }

    /** #N7: local fixo de atendimento presencial (coordenação 2026-11-19). */
    public String localPresencialFixo() {
        return texto("LOCAL_PRESENCIAL_FIXO", "Coordenação do Curso de Ciência da Computação");
    }
}
