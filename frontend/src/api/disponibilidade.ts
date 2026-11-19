import api from './client'
import type { Disponibilidade } from './tipos'

export interface CriarDisponibilidadePayload {
  data: string // yyyy-MM-dd
  horaInicio: string // HH:mm
  horaFim: string
  duracaoMinutos: number
  modalidade: 'ONLINE' | 'PRESENCIAL' | 'HIBRIDO'
  local?: string
  linkReuniao?: string
  observacao?: string
}

export const disponibilidadeApi = {
  async publicas(de?: string, ate?: string): Promise<Disponibilidade[]> {
    const { data } = await api.get<Disponibilidade[]>('/disponibilidades/publicas', {
      params: { de, ate },
    })
    return data
  },
  async admin(de?: string, ate?: string): Promise<Disponibilidade[]> {
    const { data } = await api.get<Disponibilidade[]>('/disponibilidades', {
      params: { de, ate },
    })
    return data
  },
  async criarLote(payload: CriarDisponibilidadePayload): Promise<Disponibilidade[]> {
    const { data } = await api.post<Disponibilidade[]>('/disponibilidades', payload)
    return data
  },
  async bloquear(id: number): Promise<void> {
    await api.post(`/disponibilidades/${id}/bloquear`)
  },
  async desbloquear(id: number): Promise<void> {
    await api.post(`/disponibilidades/${id}/desbloquear`)
  },
}
