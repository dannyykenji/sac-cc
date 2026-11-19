import api from './client'
import type { AuditoriaItem, Page } from './tipos'

export interface FiltrosAuditoria {
  entidade?: string
  operacao?: string
  page?: number
  size?: number
}

export const auditoriaApi = {
  /** GET /api/admin/auditoria — trilha auditável (ADMIN, RF64/RNF26). */
  async listar(f: FiltrosAuditoria = {}): Promise<Page<AuditoriaItem>> {
    const { data } = await api.get<Page<AuditoriaItem>>('/admin/auditoria', {
      params: { ...f, size: f.size ?? 20 },
    })
    return data
  },
}
