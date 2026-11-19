import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { adminApi } from '../api/saccc'
import type { ApiError } from '../api/tipos'

function erro(err: unknown): string {
  return (err as { response?: { data?: ApiError } })?.response?.data?.detail ?? 'Erro inesperado.'
}

/** M2 — fila de cadastros pendentes (ADR-006). */
export default function CadastrosPendentes() {
  const qc = useQueryClient()
  const [erroAcao, setErroAcao] = useState<string | null>(null)
  const [motivo, setMotivo] = useState<Record<number, string>>({})
  const [confirmando, setConfirmando] = useState<number | null>(null)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['pendentes'],
    queryFn: () => adminApi.pendentes(0),
  })

  async function aprovar(id: number) {
    setErroAcao(null)
    try {
      await adminApi.aprovar(id)
      qc.invalidateQueries({ queryKey: ['pendentes'] })
    } catch (err) {
      setErroAcao(erro(err))
    }
  }

  async function rejeitar(id: number) {
    setErroAcao(null)
    try {
      await adminApi.rejeitar(id, motivo[id] || '')
      setConfirmando(null)
      qc.invalidateQueries({ queryKey: ['pendentes'] })
    } catch (err) {
      setErroAcao(erro(err))
    }
  }

  if (isLoading) return <p className="text-ink-3">Carregando…</p>
  if (isError) return <p role="alert" className="text-red-700">Não foi possível carregar os cadastros.</p>

  return (
    <section aria-labelledby="titulo-pendentes">
      <h1 id="titulo-pendentes" className="text-xl font-bold text-ink">Cadastros pendentes</h1>
      <p className="mt-1 text-sm text-ink-3">
        Valide a matrícula antes de aprovar. Rejeitar exige motivo e preserva o registro.
      </p>

      {erroAcao && <p role="alert" className="mt-4 rounded bg-red-50 border border-red-200 text-red-800 text-sm px-3 py-2">{erroAcao}</p>}

      {data && data.content.length === 0 && (
        <p className="mt-6 rounded-none border border-dashed border-hairline bg-white p-6 text-center text-ink-3">
          Nenhum cadastro aguardando aprovação.
        </p>
      )}

      <ul className="mt-6 space-y-3">
        {data?.content.map((a) => (
          <li key={a.id} className="rounded-none border border-hairline bg-white p-4">
            <div className="flex flex-wrap items-start justify-between gap-3">
              <div>
                <p className="font-medium text-ink">{a.nome}</p>
                <p className="text-sm text-ink-3">
                  {a.email} · matrícula <strong>{a.matricula}</strong>
                  {a.periodo ? ` · período ${a.periodo}` : ''}
                  {a.turma ? ` · ${a.turma}` : ''}
                </p>
                <p className="text-xs text-ink-3">recebido em {new Date(a.dataCadastro).toLocaleString('pt-BR')}</p>
              </div>
              <div className="flex gap-2">
                <button
                  onClick={() => aprovar(a.id)}
                  className="rounded bg-emerald-700 hover:bg-emerald-800 text-white text-sm px-4 py-1.5 focus:outline-none focus:ring-2 focus:ring-emerald-600 focus:ring-offset-2"
                >
                  Aprovar
                </button>
                <button
                  onClick={() => setConfirmando(confirmando === a.id ? null : a.id)}
                  className="rounded border border-red-300 text-red-700 hover:bg-red-50 text-sm px-4 py-1.5 focus:outline-none focus:ring-2 focus:ring-red-600 focus:ring-offset-2"
                >
                  Rejeitar
                </button>
              </div>
            </div>

            {confirmando === a.id && (
              <div className="mt-3 rounded bg-canvas border border-hairline p-3">
                <label htmlFor={`motivo-${a.id}`} className="block text-sm font-medium text-ink-2">
                  Motivo da rejeição (obrigatório)
                </label>
                <input
                  id={`motivo-${a.id}`}
                  value={motivo[a.id] ?? ''}
                  onChange={(e) => setMotivo((m) => ({ ...m, [a.id]: e.target.value }))}
                  className="mt-1 w-full rounded border border-hairline px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-red-600"
                  placeholder="Ex.: matrícula não encontrada"
                />
                <div className="mt-2 flex gap-2">
                  <button
                    onClick={() => rejeitar(a.id)}
                    className="rounded bg-red-700 hover:bg-red-800 text-white text-sm px-4 py-1.5 focus:outline-none focus:ring-2 focus:ring-red-600 focus:ring-offset-2"
                  >
                    Confirmar rejeição
                  </button>
                  <button
                    onClick={() => setConfirmando(null)}
                    className="rounded border border-hairline text-ink-2 hover:bg-panel text-sm px-4 py-1.5"
                  >
                    Cancelar
                  </button>
                </div>
              </div>
            )}
          </li>
        ))}
      </ul>
    </section>
  )
}
