package br.edu.saccc.service;

import java.time.ZoneId;

/**
 * Fuso do sistema (RNF62). CONFIGURÁVEL via env SACCC_TIMEZONE (padrão
 * America/Sao_Paulo) — nunca mais hard-code: a coordenação define o fuso da
 * instituição no deploy (ex.: SACCC_TIMEZONE=America/Manaus).
 *
 * Regra: datas/horas agendadas são "hora local da instituição" (wall clock);
 * comparações com "agora" SEMPRE usam este fuso, o mesmo que abre/fecha a
 * sala de chat e valida prazos — um único relógio, zero conflito entre
 * telas e fluxos.
 */
public final class ZoneSaoPaulo {

    public static final ZoneId ZONE =
            ZoneId.of(System.getenv().getOrDefault("SACCC_TIMEZONE", "America/Sao_Paulo"));

    private ZoneSaoPaulo() {}
}
