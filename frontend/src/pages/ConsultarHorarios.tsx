import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useState } from 'react'
import { disponibilidadeApi } from '../api/disponibilidade'
import { fmtDataSemana, fmtHora } from '../lib/formato'

function dataISO(d: Date): string {
  return d.toISOString().slice(0, 10)
}

const MODALIDADE: Record<string, { texto: string }> = {
  ONLINE: { texto: 'Online' },
  PRESENCIAL: { texto: 'Presencial' },
  HIBRIDO: { texto: 'Híbrido' },
}

/** A2 — Consultar horários disponíveis (UC02). Apenas DISPONIVEL e futuro. */
export default function ConsultarHorarios() {
  const hoje = new Date()
  const [periodo, setPeriodo] = useState({ de: dataISO(hoje), ate: dataISO(new Date(hoje.getTime() + 28 * 86400000)) })

  const { data, isLoading, isError } = useQuery({
    queryKey: ['disp-publicas', periodo],
    queryFn: () => disponibilidadeApi.publicas(periodo.de, periodo.ate),
  })

  const grupos = new Map<string, typeof data>()
  if (data) {
    for (const s of data) {
      const lista = grupos.get(s.data) ?? []
      lista.push(s)
      grupos.set(s.data, lista!)
    }
  }

  const campo = 'mt-1 rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-darker'

  return (
    <section aria-labelledby="titulo-consultar">
      <h1 id="titulo-consultar" className="text-xl font-bold text-slate-900">Consultar horários</h1>

      <div className="mt-4 flex flex-wrap items-end gap-3">
        <div>
          <label htmlFor="c-de" className="block text-xs font-medium text-slate-600">De</label>
          <input id="c-de" type="date" value={periodo.de}
            onChange={(e) => setPeriodo((p) => ({ ...p, de: e.target.value }))} className={campo} />
        </div>
        <div>
          <label htmlFor="c-ate" className="block text-xs font-medium text-slate-600">Até</label>
          <input id="c-ate" type="date" value={periodo.ate}
            onChange={(e) => setPeriodo((p) => ({ ...p, ate: e.target.value }))} className={campo} />
        </div>
      </div>

      {isLoading && <p className="mt-6 text-slate-500">Carregando…</p>}
      {isError && <p role="alert" className="mt-6 text-red-700">Não foi possível carregar os horários.</p>}
      {data && data.length === 0 && (
        <p className="mt-6 rounded-none border border-dashed border-slate-300 bg-white p-6 text-center text-slate-500">
          Nenhum horário disponível neste período. Tente ampliar as datas.
        </p>
      )}

      <div className="mt-6 space-y-6">
        {[...grupos.entries()].map(([dataIso, slots]) => (
          <div key={dataIso}>
            <h3 className="text-sm font-semibold text-slate-700">
              {fmtDataSemana(dataIso)}
            </h3>
            {/* alternativa textual à grade (RNF49) — lista simples de botões */}
            <ul className="mt-2 grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
              {slots?.map((slot) => (
                <li key={slot.id}>
                  <Link
                    to={`/agendar/${slot.id}`}
                    className="block rounded-none border border-emerald-200 bg-white hover:border-emerald-400 hover:shadow p-3 focus:outline-none focus:ring-2 focus:ring-emerald-600"
                  >
                    <p className="font-medium text-slate-900 text-sm">
                        {fmtHora(slot.horaInicio)} – {fmtHora(slot.horaFim)}
                    </p>
                    <p className="text-xs text-slate-500">
                      {MODALIDADE[slot.modalidade].texto}
                      {slot.local ? ` · ${slot.local}` : ''} · {slot.duracaoMinutos} min
                    </p>
                    <p className="mt-1 text-xs font-medium text-emerald-700">Agendar</p>
                  </Link>
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>
    </section>
  )
}
