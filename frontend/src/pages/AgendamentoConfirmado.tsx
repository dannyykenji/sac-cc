import { Link, useLocation } from 'react-router-dom'
import { useState } from 'react'
import type { Agendamento } from '../api/tipos'
import { fmtDataSemana, fmtHora } from '../lib/formato'

/** A4 — Confirmação com protocolo (RNF06). */
export default function AgendamentoConfirmado() {
  const location = useLocation()
  const ag = (location.state as { agendamento?: Agendamento } | null)?.agendamento
  const [copiado, setCopiado] = useState(false)

  if (!ag) {
    return (
      <section className="max-w-xl">
        <p className="rounded-none border border-dashed border-hairline bg-white p-6 text-center text-ink-3">
          Nenhum agendamento recente.{' '}
          <Link to="/app/meus-agendamentos" className="text-brand hover:underline">Ver meus agendamentos</Link>
        </p>
      </section>
    )
  }

  async function copiar() {
    await navigator.clipboard.writeText(ag!.protocolo)
    setCopiado(true)
    setTimeout(() => setCopiado(false), 2000)
  }

  return (
    <section aria-labelledby="titulo-conf" className="max-w-xl">
      <div className="rounded border border-emerald-200 bg-white p-8 text-center">
        <div aria-hidden className="mx-auto h-14 w-14 rounded-full bg-emerald-100 flex items-center justify-center text-3xl">✓</div>
        <h1 id="titulo-conf" className="mt-4 text-xl font-bold text-ink">Atendimento agendado!</h1>

        <div className="mt-6 rounded-xl bg-canvas border border-hairline p-4">
          <p className="text-xs uppercase tracking-wide text-ink-3">Protocolo</p>
          <div className="mt-1 flex items-center justify-center gap-2">
            <p className="text-2xl font-mono font-bold text-ink">{ag.protocolo}</p>
            <button
              onClick={copiar}
              className="rounded border border-hairline px-2 py-1 text-xs text-ink-2 hover:bg-panel focus:outline-none focus:ring-2 focus:ring-brand"
              aria-label={`Copiar protocolo ${ag.protocolo}`}
            >
              {copiado ? 'Copiado!' : 'Copiar'}
            </button>
          </div>
        </div>

        <dl className="mt-6 space-y-2 text-left inline-block">
          <div className="flex gap-2">
            <dt className="text-ink-3">Data:</dt>
            <dd className="font-medium text-ink">
              {fmtDataSemana(ag.data)}
            </dd>
          </div>
          <div className="flex gap-2">
            <dt className="text-ink-3">Horário:</dt>
            <dd className="font-medium text-ink">{fmtHora(ag.horaInicio)} – {fmtHora(ag.horaFim)}</dd>
          </div>
          <div className="flex gap-2">
            <dt className="text-ink-3">Modalidade:</dt>
            <dd className="font-medium text-ink">{ag.modalidade === 'ONLINE' ? 'Online' : 'Presencial'}</dd>
          </div>
          <div className="flex gap-2">
            <dt className="text-ink-3">Assunto:</dt>
            <dd className="font-medium text-ink">{ag.assunto}</dd>
          </div>
        </dl>

        <p className="mt-6 text-xs text-ink-3">
          Guarde o protocolo para consulta. Você pode cancelar em "Meus agendamentos" até 24h antes.
        </p>
        <Link
          to="/app/meus-agendamentos"
          className="mt-4 inline-block rounded bg-brand hover:bg-brand-dark text-white font-medium px-6 py-2 focus:outline-none focus:ring-2 focus:ring-brand focus:ring-offset-2"
        >
          Ver em Meus agendamentos
        </Link>
      </div>
    </section>
  )
}
