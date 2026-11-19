import api from './client'
import type { DistribuicaoItem, EvolucaoItem, ResumoEstatisticas } from './tipos'

export interface FiltrosEstatisticas {
  de?: string
  ate?: string
  modalidade?: string
  categoriaId?: number
  responsavelId?: number
}

export const estatisticaApi = {
  async resumo(f: FiltrosEstatisticas = {}): Promise<ResumoEstatisticas> {
    const { data } = await api.get<ResumoEstatisticas>('/estatisticas/resumo', { params: f })
    return data
  },
  async evolucao(f: FiltrosEstatisticas = {}): Promise<EvolucaoItem[]> {
    const { data } = await api.get<EvolucaoItem[]>('/estatisticas/evolucao', { params: f })
    return data
  },
  async distribuicao(f: FiltrosEstatisticas = {}): Promise<DistribuicaoItem[]> {
    const { data } = await api.get<DistribuicaoItem[]>('/estatisticas/distribuicao', { params: f })
    return data
  },
}
