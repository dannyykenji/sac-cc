import api from './client'
import type { Agendamento, Page } from './tipos'

/** Filtros da agenda administrativa (RF14) e do painel (RF54). */
export interface FiltrosAgenda {
  de?: string
  ate?: string
  status?: string
  modalidade?: string
  categoriaId?: number
  busca?: string
  page?: number
  size?: number
}

export const agendaApi = {
  /** GET /api/admin/agenda — todos os agendamentos, todos os status (COORDENADOR/ADMIN). */
  async listar(f: FiltrosAgenda = {}): Promise<Page<Agendamento>> {
    const { data } = await api.get<Page<Agendamento>>('/admin/agenda', {
      params: { ...f, size: f.size ?? 20 },
    })
    return data
  },
}
