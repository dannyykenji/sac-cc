import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { agendaApi } from '../api/agenda'
import { Badge, CabecalhoPagina, Card } from '../components/ui'
import type { Agendamento } from '../api/tipos'
import { fmtData, fmtHora } from '../lib/formato'

/** C1 — Agenda de atendimentos da coordenação, com filtros (RF14). */
export default function AgendaCoordenacao() {
  const nav = useNavigate()
  const [filtros, setFiltros] = useState({ de: '', ate: '', status: '', modalidade: '', busca: '' })
  const [page, setPage] = useState(0)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['agenda', filtros, page],
    queryFn: () =>
      agendaApi.listar({
        de: filtros.de || undefined,
        ate: filtros.ate || undefined,
        status: filtros.status || undefined,
        modalidade: filtros.modalidade || undefined,
        busca: filtros.busca || undefined,
        page,
      }),
  })

  return (
    <div className="max-w-6xl mx-auto space-y-6">
      <CabecalhoPagina
        titulo="Agenda de Atendimentos"
        subtitulo="Todos os agendamentos — clique em um atendimento para registrar o resultado"
      />

      <Card>
        <form className="grid grid-cols-2 md:grid-cols-5 gap-3 items-end" onSubmit={(e) => e.preventDefault()}>
          <div>
            <label htmlFor="f-de" className="block text-xs font-bold text-ink-3 mb-1">De</label>
            <input id="f-de" type="date" value={filtros.de}
              onChange={(e) => { setFiltros((f) => ({ ...f, de: e.target.value })); setPage(0) }}
              className="w-full bg-white border border-hairline rounded px-2.5 py-2 text-sm" />
          </div>
          <div>
            <label htmlFor="f-ate" className="block text-xs font-bold text-ink-3 mb-1">Até</label>
            <input id="f-ate" type="date" value={filtros.ate}
              onChange={(e) => { setFiltros((f) => ({ ...f, ate: e.target.value })); setPage(0) }}
              className="w-full bg-white border border-hairline rounded px-2.5 py-2 text-sm" />
          </div>
          <div>
            <label htmlFor="f-status" className="block text-xs font-bold text-ink-3 mb-1">Status</label>
            <select id="f-status" value={filtros.status}
              onChange={(e) => { setFiltros((f) => ({ ...f, status: e.target.value })); setPage(0) }}
              className="w-full bg-white border border-hairline rounded px-2.5 py-2 text-sm">
              <option value="">Todos</option>
              <option value="SOLICITADO">Solicitado</option>
              <option value="CONFIRMADO">Confirmado</option>
              <option value="REALIZADO">Realizado</option>
              <option value="CANCELADO">Cancelado</option>
              <option value="NAO_COMPARECEU">Não compareceu</option>
              <option value="ENCERRADO">Encerrado</option>
            </select>
          </div>
          <div>
            <label htmlFor="f-modalidade" className="block text-xs font-bold text-ink-3 mb-1">Modalidade</label>
            <select id="f-modalidade" value={filtros.modalidade}
              onChange={(e) => { setFiltros((f) => ({ ...f, modalidade: e.target.value })); setPage(0) }}
              className="w-full bg-white border border-hairline rounded px-2.5 py-2 text-sm">
              <option value="">Todas</option>
              <option value="ONLINE">Online</option>
              <option value="PRESENCIAL">Presencial</option>
            </select>
          </div>
          <div>
            <label htmlFor="f-busca" className="block text-xs font-bold text-ink-3 mb-1">Buscar</label>
            <input id="f-busca" type="search" placeholder="aluno, protocolo…"
              value={filtros.busca}
              onChange={(e) => { setFiltros((f) => ({ ...f, busca: e.target.value })); setPage(0) }}
              className="w-full bg-white border border-hairline rounded px-2.5 py-2 text-sm" />
          </div>
        </form>
      </Card>

      {isLoading && <p className="text-sm text-ink-3">Carregando agenda…</p>}
      {isError && <p role="alert" className="text-sm text-red-700">Não foi possível carregar a agenda.</p>}

      {data && data.content.length === 0 && (
        <div className="bg-white border border-hairline rounded-lg py-10 text-center">
          <p className="text-sm font-semibold text-ink-2">Nenhum atendimento encontrado</p>
          <p className="text-sm text-ink-3 mt-1">Ajuste os filtros ou aguarde novos agendamentos.</p>
        </div>
      )}

      {data && data.content.length > 0 && (
        <Card className="!px-0">
          <div className="overflow-x-auto">
            <table className="w-full text-sm">
              <caption className="sr-only">Agenda de atendimentos</caption>
              <thead>
                <tr className="bg-panel text-left text-xs font-bold uppercase tracking-wide text-ink-3">
                  <th scope="col" className="px-5 py-2.5">Data/Hora</th>
                  <th scope="col" className="px-5 py-2.5">Protocolo</th>
                  <th scope="col" className="px-5 py-2.5">Assunto</th>
                  <th scope="col" className="px-5 py-2.5">Modalidade</th>
                  <th scope="col" className="px-5 py-2.5">Status</th>
                  <th scope="col" className="px-5 py-2.5 text-right">Ação</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-hairline">
                {data.content.map((ag: Agendamento) => {
                  const ativo = ag.status === 'SOLICITADO' || ag.status === 'CONFIRMADO'
                  return (
                    <tr key={ag.id} className="hover:bg-brand-soft/40">
                      <td className="px-5 py-3 whitespace-nowrap">
                        <span className="font-bold text-ink">{fmtData(ag.data)}</span>{' '}
                        <span className="text-ink-3">{fmtHora(ag.horaInicio)}</span>
                      </td>
                      <td className="px-5 py-3 font-mono text-xs text-ink-3">{ag.protocolo}</td>
                      <td className="px-5 py-3 text-ink-2 max-w-64 truncate">{ag.assunto}</td>
                      <td className="px-5 py-3 text-ink-3">
                        {ag.modalidade === 'ONLINE' ? 'Online' : ag.modalidade === 'PRESENCIAL' ? 'Presencial' : 'Híbrido'}
                      </td>
                      <td className="px-5 py-3"><Badge valor={ag.status} /></td>
                      <td className="px-5 py-3 text-right whitespace-nowrap">
                        <div className="inline-flex items-center justify-end gap-3">
                          {ativo && ag.modalidade !== 'PRESENCIAL' && (
                            <Link
                              to={`/app/chat/${ag.id}`}
                              className="rounded bg-brand hover:bg-brand-dark text-white text-xs font-bold px-3 py-1.5 focus:outline-none focus:ring-2 focus:ring-brand focus:ring-offset-2"
                            >
                              Entrar na conversa
                            </Link>
                          )}
                          <button
                            type="button"
                            onClick={() => nav(`/app/atendimento/${ag.id}`)}
                            className="text-brand font-bold text-xs hover:underline"
                          >
                            {ativo ? 'Registrar' : 'Detalhes'}
                          </button>
                        </div>
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
          <div className="flex items-center justify-between px-5 py-3 border-t border-hairline mt-2">
            <p className="text-xs text-ink-3">
              {data.totalElements} atendimento(s) — página {data.number + 1} de {data.totalPages || 1}
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