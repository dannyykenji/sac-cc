import api from './client'
import type { Atendimento, HistoricoAtendimento, RegistrarAtendimentoPayload } from './tipos'

export const atendimentoApi = {
  /** POST /api/atendimentos — registra realização ou ausência (UC08, RN07–RN12). */
  async registrar(payload: RegistrarAtendimentoPayload): Promise<Atendimento> {
    const { data } = await api.post<Atendimento>('/atendimentos', payload)
    return data
  },
  /** POST /api/atendimentos/{agendamentoId}/status — transição de status (Fase 4). */
  async atualizarStatus(
    agendamentoId: number,
    payload: { statusResolucao?: string; justificativa?: string; solucao?: string; prazoRetorno?: string },
  ): Promise<Atendimento> {
    const { data } = await api.post<Atendimento>(`/atendimentos/${agendamentoId}/status`, payload)
    return data
  },
  /** GET /api/atendimentos/{agendamentoId} — visão da coordenação (com interno). */
  async detalhe(agendamentoId: number): Promise<Atendimento> {
    const { data } = await api.get<Atendimento>(`/atendimentos/${agendamentoId}`)
    return data
  },
  /** POST /api/atendimentos/{agendamentoId}/confirmar-aluno — #N1c confirmação dupla. */
  async confirmarAluno(agendamentoId: number): Promise<Atendimento> {
    const { data } = await api.post<Atendimento>(`/atendimentos/${agendamentoId}/confirmar-aluno`)
    return data
  },
  /** GET /api/atendimentos/{agendamentoId}/historico — trilha de transições. */
  async historico(agendamentoId: number): Promise<HistoricoAtendimento[]> {
    const { data } = await api.get<HistoricoAtendimento[]>(`/atendimentos/${agendamentoId}/historico`)
    return data
  },
}
