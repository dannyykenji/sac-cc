package br.edu.saccc.model;

/** Ciclo de vida do agendamento (classe 3.8). REALIZADO/NAO_COMPARECEU/ENCERRADO entram na Fase 4. */
public enum StatusAgendamento {
    SOLICITADO,
    CONFIRMADO,
    CANCELADO,
    REALIZADO,
    NAO_COMPARECEU,
    ENCERRADO
}
