import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useState } from 'react'
import { agendamentoApi } from '../api/agendamento'
import { atendimentoApi } from '../api/atendimento'
import type { Agendamento, ApiError } from '../api/tipos'
import { fmtDataCurta, fmtHora } from '../lib/formato'

const STATUS: Record<string, { texto: string; classe: string }> = {
  SOLICITADO: { texto: 'Solicitado', classe: 'bg-brand-soft text-brand border-brand/30' },
  CONFIRMADO: { texto: 'Confirmado', classe: 'bg-emerald-100 text-emerald-800 border-emerald-200' },
  CANCELADO: { texto: 'Cancelado', classe: 'bg-panel text-ink-2 border-hairline' },
  REALIZADO: { texto: 'Realizado', classe: 'bg-emerald-100 text-emerald-800 border-emerald-200' },
  NAO_COMPARECEU: { texto: 'Não compareceu', classe: 'bg-amber-100 text-amber-800 border-amber-200' },
  ENCERRADO: { texto: 'Encerrado', classe: 'bg-panel text-ink-2 border-hairline' },
}

function dataCurta(iso: string): string {
  return fmtDataCurta(iso)
}

function Card({ ag, onCancelavel }: { ag: Agendamento; onCancelavel: (ag: Agendamento) => void }) {
  const st = STATUS[ag.status] ?? { texto: ag.status, classe: 'bg-panel text-ink-2 border-hairline' }
  const ativo = ag.status === 'SOLICITADO' || ag.status === 'CONFIRMADO'
  const [confirmado, setConfirmado] = useState(false)
  const qc = useQueryClient()
  const confirmar = useMutation({
    mutationFn: () => atendimentoApi.confirmarAluno(ag.id),
    onSuccess: () => {
      setConfirmado(true)
      qc.invalidateQueries({ queryKey: ['meus-agendamentos'] })
    },
  })
  return (
    <li className="rounded-none border border-hairline bg-white p-4">
      <div className="flex flex-wrap items-start justify-between gap-2">
        <div>
          <p className="font-medium text-ink">
            {dataCurta(ag.data)} · {fmtHora(ag.horaInicio)}–{fmtHora(ag.horaFim)}
            <span className="ml-2 text-xs font-normal text-ink-3">
              {ag.modalidade === 'ONLINE' ? 'Online' : 'Presencial'}
            </span>
          </p>
          <p className="text-sm text-ink-2">{ag.assunto}</p>
          <p className="mt-1 font-mono text-xs text-ink-3">{ag.protocolo}</p>
          {ag.motivoCancelamento && (
            <p className="mt-1 text-xs text-ink-3">Motivo do cancelamento: {ag.motivoCancelamento}</p>
          )}
        </div>
        <div className="flex flex-col items-end gap-2">
          <span className={`rounded-full border px-2 py-0.5 text-xs ${st.classe}`}>{st.texto}</span>
          {ativo && ag.modalidade !== 'PRESENCIAL' && (
            <Link
              to={`/app/chat/${ag.id}`}
              className="rounded bg-brand hover:bg-brand-dark text-white text-xs font-bold px-3 py-1.5 focus:outline-none focus:ring-2 focus:ring-brand focus:ring-offset-2"
            >
              Entrar na conversa
            </Link>
          )}
          {ag.status === 'REALIZADO' && !confirmado && (
            <button
              type="button"
              onClick={() => confirmar.mutate()}
              disabled={confirmar.isPending}
              className="rounded border border-emerald-600 text-emerald-700 hover:bg-emerald-50 text-xs font-bold px-3 py-1.5 focus:outline-none focus:ring-2 focus:ring-emerald-600 focus:ring-offset-2"
            >
              {confirmar.isPending ? 'Confirmando…' : 'Confirmar comparecimento'}
            </button>
          )}
          {confirmado && (
            <span className="text-xs font-bold text-emerald-700">Comparecimento confirmado</span>
          )}
          {ativo && (
            <button
              onClick={() => onCancelavel(ag)}
              className="rounded border border-red-300 text-red-700 hover:bg-red-50 text-xs px-3 py-1.5 focus:outline-none focus:ring-2 focus:ring-red-600 focus:ring-offset-2"
            >
              Cancelar
            </button>
          )}
        </div>
      </div>
    </li>
  )
}

/** A5 — Meus agendamentos (UC04): abas Próximos / Histórico; A7 diálogo de cancelamento. */
export default function MeusAgendamentos() {
  const qc = useQueryClient()
  const [aba, setAba] = useState<'proximos' | 'historico'>('proximos')
  const [cancelando, setCancelando] = useState<Agendamento | null>(null)
  const [motivo, setMotivo] = useState('')
  const [erro, setErro] = useState<string | null>(null)

  const proximos = useQuery({ queryKey: ['meus-agendamentos', 'prox'], queryFn: agendamentoApi.proximos })
  const historico = useQuery({ queryKey: ['meus-agendamentos', 'hist'], queryFn: agendamentoApi.historico })

  const cancelar = useMutation({
    mutationFn: (ag: Agendamento) => agendamentoApi.cancelar(ag.id, motivo),
    onSuccess: () => {
      setCancelando(null)
      setMotivo('')
      setErro(null)
      qc.invalidateQueries({ queryKey: ['meus-agendamentos'] })
    },
    onError: (err) => {
      setErro((err as { response?: { data?: ApiError } })?.response?.data?.detail ?? 'Não foi possível cancelar.')
    },
  })

  const lista = aba === 'proximos' ? proximos.data : historico.data
  const carregando = aba === 'proximos' ? proximos.isLoading : historico.isLoading

  return (
    <section aria-labelledby="titulo-meus">
      <h1 id="titulo-meus" className="text-xl font-bold text-ink">Meus agendamentos</h1>

      <div role="tablist" aria-label="tipo de listagem" className="mt-4 flex gap-2">
        <button
          role="tab" aria-selected={aba === 'proximos'}
          onClick={() => setAba('proximos')}
          className={`rounded px-4 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand ${
            aba === 'proximos' ? 'bg-slate-900 text-white' : 'bg-white border border-hairline text-ink-2 hover:bg-panel'
          }`}
        >
          Próximos
        </button>
        <button
          role="tab" aria-selected={aba === 'historico'}
          onClick={() => setAba('historico')}
          className={`rounded px-4 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand ${
            aba === 'historico' ? 'bg-slate-900 text-white' : 'bg-white border border-hairline text-ink-2 hover:bg-panel'
          }`}
        >
          Histórico
        </button>
      </div>

      {carregando && <p className="mt-6 text-ink-3">Carregando…</p>}

      {lista && lista.length === 0 && (
        <p className="mt-6 rounded-none border border-dashed border-hairline bg-white p-6 text-center text-ink-3">
          {aba === 'proximos'
            ? 'Você não tem atendimentos futuros. '
            : 'Nenhum atendimento anterior registrado. '}
        </p>
      )}

      <ul className="mt-6 space-y-3">
        {lista?.map((ag) => (
          <Card key={ag.id} ag={ag} onCancelavel={(a) => { setCancelando(a); setErro(null); setMotivo('') }} />
        ))}
      </ul>

      {/* A7 — diálogo de cancelamento (confirmação dupla, RNF05) */}
      {cancelando && (
        <div role="dialog" aria-modal="true" aria-labelledby="titulo-cancelar" className="fixed inset-0 z-10 flex items-center justify-center bg-slate-900/50 p-4">
          <div className="w-full max-w-md rounded bg-white p-6">
            <h2 id="titulo-cancelar" className="text-lg font-bold text-ink">Cancelar atendimento?</h2>
            <p className="mt-2 text-sm text-ink-2">
              {dataCurta(cancelando.data)} · {fmtHora(cancelando.horaInicio)} — protocolo {cancelando.protocolo}
            </p>
            <div className="mt-4">
              <label htmlFor="motivo-cancel" className="block text-sm font-medium text-ink-2">
                Motivo do cancelamento (obrigatório)
              </label>
              <input
                id="motivo-cancel" value={motivo}
                onChange={(e) => setMotivo(e.target.value)}
                className="mt-1 w-full rounded border border-hairline px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-red-600"
                placeholder="Ex.: conflito de horário"
              />
            </div>
            {erro && <p role="alert" className="mt-3 rounded bg-red-50 border border-red-200 text-red-800 text-sm px-3 py-2">{erro}</p>}
            <div className="mt-5 flex justify-end gap-2">
              <button
                onClick={() => setCancelando(null)}
                className="rounded border border-hairline px-4 py-2 text-sm text-ink-2 hover:bg-panel"
              >
                Manter agendamento
              </button>
              <button
                onClick={() => cancelar.mutate(cancelando)}
                disabled={!motivo.trim() || cancelar.isPending}
                className="rounded bg-red-700 hover:bg-red-800 disabled:opacity-60 text-white text-sm font-medium px-4 py-2 focus:outline-none focus:ring-2 focus:ring-red-600 focus:ring-offset-2"
              >
                {cancelar.isPending ? 'Cancelando…' : 'Confirmar cancelamento'}
              </button>
            </div>
          </div>
        </div>
      )}
    </section>
  )
}
