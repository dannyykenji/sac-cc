package br.edu.saccc.dto;

import br.edu.saccc.model.Atendimento;
import br.edu.saccc.model.HistoricoAtendimento;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Visão da COORDENAÇÃO (inclui observações internas — RF43). */
public record AtendimentoResponse(
        Long agendamentoId,
        Long responsavelId,
        boolean realizado,
        String statusResolucao,
        LocalDateTime dataRealizacao,
        String observacoes,
        String solucaoRegistrada,
        String encaminhamento,
        LocalDate prazoRetorno,
        LocalDateTime dataEncerramento,
        boolean confirmadoAluno) {

    public static AtendimentoResponse de(Atendimento a) {
        return new AtendimentoResponse(
                a.getAgendamentoId(), a.getResponsavelId(), a.isRealizado(),
                a.getStatusResolucao().name(), a.getDataRealizacao(), a.getObservacoes(),
                a.getSolucaoRegistrada(), a.getEncaminhamento(), a.getPrazoRetorno(),
                a.getDataEncerramento(), a.isConfirmadoAluno());
    }

    public record HistoricoItem(Long id, String statusAnterior, String statusNovo,
                                String descricao, Long usuarioId, LocalDateTime dataRegistro) {
        public static HistoricoItem de(HistoricoAtendimento h) {
            return new HistoricoItem(h.getId(), h.getStatusAnterior(), h.getStatusNovo(),
                    h.getDescricao(), h.getUsuarioId(), h.getDataRegistro());
        }
    }
}
