import api, { iniciarSessaoCsrf } from './client'
import type { AutoCadastroPayload, LoginPayload, Page, UsuarioAdmin, UsuarioAtual } from './tipos'

export const authApi = {
  async login(payload: LoginPayload): Promise<UsuarioAtual> {
    await iniciarSessaoCsrf()
    const { data } = await api.post<UsuarioAtual>('/auth/login', payload)
    return data
  },
  async logout(): Promise<void> {
    await api.post('/auth/logout')
  },
  async me(): Promise<UsuarioAtual | null> {
    try {
      const { data } = await api.get<UsuarioAtual>('/auth/me')
      return data
    } catch {
      return null
    }
  },
  async autoCadastro(payload: AutoCadastroPayload): Promise<void> {
    await iniciarSessaoCsrf()
    await api.post('/cadastro-aluno', payload)
  },
}

export const adminApi = {
  async pendentes(page = 0): Promise<Page<UsuarioAdmin>> {
    const { data } = await api.get<Page<UsuarioAdmin>>('/admin/cadastros-pendentes', {
      params: { page, size: 20 },
    })
    return data
  },
  async aprovar(id: number): Promise<void> {
    await api.post(`/admin/cadastros-pendentes/${id}/aprovar`)
  },
  async rejeitar(id: number, motivo: string): Promise<void> {
    await api.post(`/admin/cadastros-pendentes/${id}/rejeitar`, { motivo })
  },
  async usuarios(params: { perfil?: string; status?: string; busca?: string; page?: number }): Promise<Page<UsuarioAdmin>> {
    const { data } = await api.get<Page<UsuarioAdmin>>('/usuarios', { params: { ...params, size: 20 } })
    return data
  },
  async desativar(id: number): Promise<void> {
    await api.post(`/usuarios/${id}/desativar`)
  },
  async ativar(id: number): Promise<void> {
    await api.post(`/usuarios/${id}/ativar`)
  },
}
