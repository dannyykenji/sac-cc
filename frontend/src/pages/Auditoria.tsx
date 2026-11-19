import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { auditoriaApi } from '../api/auditoria'
import { CabecalhoPagina, Card } from '../components/ui'

/** M4 — Auditoria (ADMIN, RF64/RNF26): trilha com filtros e paginação. */
export default function Auditoria() {
  const [entidade, setEntidade] = useState('')
  const [operacao, setOperacao] = useState('')
  const [page, setPage] = useState(0)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['auditoria', entidade, operacao, page],
    queryFn: () =>
      auditoriaApi.listar({
        entidade: entidade || undefined,
        operacao: operacao || undefined,
        page,
      }),
  })

  return (
    <div className="max-w-6xl mx-auto space-y-6">
      <CabecalhoPagina
        titulo="Auditoria"
        subtitulo="Trilha de operações do sistema (leitura, sem dados sensíveis — RNF25)"
      />

      <Card>
        <form className="flex flex-wrap items-end gap-3" onSubmit={(e) => e.preventDefault()}>
          <div>
            <label htmlFor="a-ent" className="block text-xs font-bold text-ink-3 mb-1">Entidade</label>
            <input id="a-ent" value={entidade} onChange={(e) => { setEntidade(e.target.value); setPage(0) }}
              placeholder="ex.: Agendamento" className="bg-white border border-hairline rounded px-2.5 py-2 text-sm" />
          </div>
          <div>
            <label htmlFor="a-op" className="block text-xs font-bold text-ink-3 mb-1">Operação</label>
            <input id="a-op" value={operacao} onChange={(e) => { setOperacao(e.target.value); setPage(0) }}
              placeholder="ex.: CRIAR" className="bg-white border border-hairline rounded px-2.5 py-2 text-sm" />
          </div>
        </form>
      </Card>

      {isLoading && <p className="text-sm text-ink-3">Carregando trilha…</p>}
      {isError && <p role="alert" className="text-sm text-red-700">Não foi possível carregar a auditoria.</p>}

      {data && data.content.length === 0 && (
        <div className="bg-white border border-hairline rounded-lg py-10 text-center">
          <p className="text-sm font-semibold text-ink-2">Nenhum registro com esses filtros.</p>
        </div>
      )}

      {data && data.content.length > 0 && (
        <Card className="!px-0">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <caption className="sr-only">Trilha de auditoria</caption>
              <thead>
                <tr className="bg-panel text-left text-xs font-bold uppercase tracking-wide text-ink-3">
                  <th scope="col" className="px-5 py-2.5">Data/Hora</th>
                  <th scope="col" className="px-5 py-2.5">Operação</th>
                  <th scope="col" className="px-5 py-2.5">Entidade</th>
                  <th scope="col" className="px-5 py-2.5">ID</th>
                  <th scope="col" className="px-5 py-2.5">Usuário</th>
                  <th scope="col" className="px-5 py-2.5">Detalhes</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-hairline">
                {data.content.map((a) => (
                  <tr key={a.id} className="hover:bg-brand-soft/40">
                    <td className="px-5 py-2.5 whitespace-nowrap text-ink-2">
                      {a.dataHora ? new Date(a.dataHora).toLocaleString('pt-BR') : '—'}
                    </td>
                    <td className="px-5 py-2.5"><span className="font-bold text-brand">{a.operacao}</span></td>
                    <td className="px-5 py-2.5 text-ink-2">{a.entidade}</td>
                    <td className="px-5 py-2.5 text-ink-3 font-mono text-xs">{a.identificadorEntidade ?? '—'}</td>
                    <td className="px-5 py-2.5 text-ink-3">#{a.usuarioId}</td>
                    <td className="px-5 py-2.5 text-ink-3 max-w-64 truncate" title={a.detalhes ?? ''}>{a.detalhes ?? '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <div className="flex items-center justify-between px-5 py-3 border-t border-hairline mt-2">
            <p className="text-xs text-ink-3">
              {data.totalElements} registro(s) — página {data.number + 1} de {data.totalPages || 1}
            </p>
            <div className="flex gap-2">
              <button type="button" disabled={data.number === 0} onClick={() => setPage((p) => p - 1)}
                className="text-xs font-bold px-3 py-1.5 rounded border border-hairline disabled:opacity-40 hover:bg-panel">
                ‹ Anterior</button>
              <button type="button" disabled={data.number + 1 >= data.totalPages} onClick={() => setPage((p) => p + 1)}
                className="text-xs font-bold px-3 py-1.5 rounded border border-hairline disabled:opacity-40 hover:bg-panel">
                Próxima ›</button>
            </div>
          </div>
        </Card>
      )}
    </div>
  )
}
