import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { adminApi } from '../api/saccc'
import type { ApiError } from '../api/tipos'

function erro(err: unknown): string {
  return (err as { response?: { data?: ApiError } })?.response?.data?.detail ?? 'Erro inesperado.'
}

const STATUS_TEXTO: Record<string, string> = {
  PENDENTE: 'Pendente',
  ATIVO: 'Ativo',
  INATIVO: 'Inativo',
}

const STATUS_CLASSE: Record<string, string> = {
  PENDENTE: 'bg-amber-100 text-amber-800 border-amber-200',
  ATIVO: 'bg-emerald-100 text-emerald-800 border-emerald-200',
  INATIVO: 'bg-panel text-ink-2 border-hairline',
}

/** M1 — gestão de usuários (UC11). Sem exclusão física (RNF60). */
export default function Usuarios() {
  const qc = useQueryClient()
  const [filtros, setFiltros] = useState({ perfil: '', status: '', busca: '' })
  const [erroAcao, setErroAcao] = useState<string | null>(null)
  const [confirmar, setConfirmar] = useState<number | null>(null)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['usuarios', filtros],
    queryFn: () =>
      adminApi.usuarios({
        perfil: filtros.perfil || undefined,
        status: filtros.status || undefined,
        busca: filtros.busca || undefined,
        page: 0,
      }),
  })

  async function alternarStatus(id: number, acao: 'ativar' | 'desativar') {
    setErroAcao(null)
    try {
      if (acao === 'ativar') await adminApi.ativar(id)
      else await adminApi.desativar(id)
      setConfirmar(null)
      qc.invalidateQueries({ queryKey: ['usuarios'] })
    } catch (err) {
      setErroAcao(erro(err))
    }
  }

  if (isLoading) return <p className="text-ink-3">Carregando…</p>
  if (isError) return <p role="alert" className="text-red-700">Não foi possível carregar os usuários.</p>

  return (
    <section aria-labelledby="titulo-usuarios">
      <h1 id="titulo-usuarios" className="text-xl font-bold text-ink">Usuários</h1>

      <form className="mt-4 flex flex-wrap gap-2" onSubmit={(e) => e.preventDefault()}>
        <label htmlFor="f-perfil" className="sr-only">Perfil</label>
        <select
          id="f-perfil" value={filtros.perfil}
          onChange={(e) => setFiltros((f) => ({ ...f, perfil: e.target.value }))}
          className="rounded border border-hairline px-3 py-2 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-brand"
        >
          <option value="">Todos os perfis</option>
          <option value="ALUNO">Aluno</option>
          <option value="COORDENADOR">Coordenador</option>
          <option value="ADMIN">Admin</option>
        </select>
        <label htmlFor="f-status" className="sr-only">Status</label>
        <select
          id="f-status" value={filtros.status}
          onChange={(e) => setFiltros((f) => ({ ...f, status: e.target.value }))}
          className="rounded border border-hairline px-3 py-2 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-brand"
        >
          <option value="">Todos os status</option>
          <option value="ATIVO">Ativo</option>
          <option value="PENDENTE">Pendente</option>
          <option value="INATIVO">Inativo</option>
        </select>
        <label htmlFor="f-busca" className="sr-only">Buscar</label>
        <input
          id="f-busca" type="search" placeholder="Buscar por nome ou e-mail"
          value={filtros.busca} onChange={(e) => setFiltros((f) => ({ ...f, busca: e.target.value }))}
          className="flex-1 min-w-48 rounded border border-hairline px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand"
        />
      </form>

      {erroAcao && <p role="alert" className="mt-4 rounded bg-red-50 border border-red-200 text-red-800 text-sm px-3 py-2">{erroAcao}</p>}

      <div className="mt-4 overflow-x-auto rounded-none border border-hairline bg-white">
        <table className="w-full text-sm">
          <caption className="sr-only">Lista de usuários cadastrados</caption>
          <thead className="bg-panel text-left text-ink-2">
            <tr>
              <th scope="col" className="px-4 py-2">Nome</th>
              <th scope="col" className="px-4 py-2">E-mail</th>
              <th scope="col" className="px-4 py-2">Perfil</th>
              <th scope="col" className="px-4 py-2">Status</th>
              <th scope="col" className="px-4 py-2">Ações</th>
            </tr>
          </thead>
          <tbody>
            {data?.content.map((u) => (
              <tr key={u.id} className="border-t border-hairline">
                <td className="px-4 py-2 font-medium text-ink">{u.nome}</td>
                <td className="px-4 py-2 text-ink-2">{u.email}</td>
                <td className="px-4 py-2 text-ink-2">{u.perfil}</td>
                <td className="px-4 py-2">
                  <span className={`inline-block rounded-full border px-2 py-0.5 text-xs ${STATUS_CLASSE[u.status]}`}>
                    {STATUS_TEXTO[u.status]}
                  </span>
                </td>
                <td className="px-4 py-2">
                  {confirmar === u.id ? (
                    <div className="flex items-center gap-2">
                      <span className="text-xs text-ink-3">Confirmar?</span>
                      <button onClick={() => alternarStatus(u.id, 'desativar')} className="rounded bg-red-700 hover:bg-red-800 text-white text-xs px-2 py-1 focus:outline-none focus:ring-2 focus:ring-red-600">
                        Sim, desativar
                      </button>
                      <button onClick={() => setConfirmar(null)} className="rounded border border-hairline text-xs px-2 py-1 text-ink-2 hover:bg-panel">
                        Cancelar
                      </button>
                    </div>
                  ) : u.status === 'ATIVO' ? (
                    <button onClick={() => setConfirmar(u.id)} className="rounded border border-hairline text-xs px-2 py-1 text-ink-2 hover:bg-panel focus:outline-none focus:ring-2 focus:ring-brand">
                      Desativar
                    </button>
                  ) : (
                    <button onClick={() => alternarStatus(u.id, 'ativar')} className="rounded border border-emerald-300 text-emerald-700 text-xs px-2 py-1 hover:bg-emerald-50 focus:outline-none focus:ring-2 focus:ring-emerald-600">
                      Ativar
                    </button>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      {data && data.totalElements > data.content.length && (
        <p className="mt-2 text-xs text-ink-3">{data.totalElements} usuários — paginação completa na próxima iteração</p>
      )}
    </section>
  )
}
