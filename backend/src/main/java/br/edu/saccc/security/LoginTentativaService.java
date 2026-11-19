package br.edu.saccc.security;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Bloqueio temporário após N tentativas inválidas (fluxo alt. UC01, RNF22).
 * MVP: em memória (instância única). Para múltiplas instâncias, mover para cache compartilhado.
 */
@Service
public class LoginTentativaService {

    static final int MAX_TENTATIVAS = 5;
    static final Duration BLOQUEIO = Duration.ofMinutes(15);

    private record Tentativas(int contador, Instant bloqueadoAte) {}

    private final Map<String, Tentativas> tentativas = new ConcurrentHashMap<>();

    public boolean bloqueado(String chave) {
        Tentativas t = tentativas.get(chave);
        return t != null && t.bloqueadoAte() != null && Instant.now().isBefore(t.bloqueadoAte());
    }

    public long segundosRestantes(String chave) {
        Tentativas t = tentativas.get(chave);
        if (t == null || t.bloqueadoAte() == null) return 0;
        return Math.max(0, Duration.between(Instant.now(), t.bloqueadoAte()).toSeconds());
    }

    public void registrarFalha(String chave) {
        tentativas.compute(chave, (k, t) -> {
            int n = (t == null ? 0 : t.contador()) + 1;
            Instant bloqueio = n >= MAX_TENTATIVAS ? Instant.now().plus(BLOQUEIO) : (t == null ? null : t.bloqueadoAte());
            return new Tentativas(n, bloqueio);
        });
    }

    public void limpar(String chave) {
        tentativas.remove(chave);
    }
}
