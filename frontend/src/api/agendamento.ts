import api from './client'
import type { Agendamento, Categoria } from './tipos'

export interface AgendarPayload {
  disponibilidadeId: number
  modalidade: 'ONLINE' | 'PRESENCIAL'
  categoriaId?: number
  assunto: string
  descricao?: string
}

export const agendamentoApi = {
  async agendar(payload: AgendarPayload): Promise<Agendamento> {
    const { data } = await api.post<Agendamento>('/agendamentos', payload)
    return data
  },
  async proximos(): Promise<Agendamento[]> {
    const { data } = await api.get<Agendamento[]>('/agendamentos/proximos')
    return data
  },
  async historico(): Promise<Agendamento[]> {
    const { data } = await api.get<Agendamento[]>('/agendamentos/historico')
    return data
  },
  async detalhe(id: number): Promise<Agendamento> {
    const { data } = await api.get<Agendamento>(`/agendamentos/${id}`)
    return data
  },
  async cancelar(id: number, motivo: string): Promise<Agendamento> {
    const { data } = await api.post<Agendamento>(`/agendamentos/${id}/cancelar`, { motivo })
    return data
  },
}

export const categoriaApi = {
  async listar(): Promise<Categoria[]> {
    const { data } = await api.get<Categoria[]>('/categorias')
    return data
  },
}
