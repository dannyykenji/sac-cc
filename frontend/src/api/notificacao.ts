import api from './client'
import type { Notificacao } from './tipos'

export const notificacaoApi = {
  /** GET /api/notificacoes — as próprias notificações (RF56/RF60). */
  async minhas(): Promise<Notificacao[]> {
    const { data } = await api.get<Notificacao[]>('/notificacoes')
    return data
  },
}
