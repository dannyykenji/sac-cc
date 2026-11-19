import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { disponibilidadeApi } from '../api/disponibilidade'
import type { ApiError, Disponibilidade } from '../api/tipos'
import { fmtDataSemana, fmtHora } from '../lib/formato'

function erro(err: unknown): string {
  return (err as { response?: { data?: ApiError } })?.response?.data?.detail ?? 'Erro inesperado.'
}

function dataISO(d: Date): string {
  return d.toISOString().slice(0, 10)
}

const STATUS_BADGE: Record<string, string> = {
  DISPONIVEL: 'bg-emerald-100 text-emerald-800 border-emerald-200',
  BLOQUEADO: 'bg-panel text-ink-2 border-hairline',
  RESERVADO: 'bg-brand-soft text-brand border-brand/30',
  EXPIRADO: 'bg-amber-100 text-amber-800 border-amber-200',
}

const MODALIDADE_TEXTO: Record<string, string> = {
  ONLINE: 'Online',
  PRESENCIAL: 'Presencial',
  HIBRIDO: 'Híbrido',
}

/** Agrupa slots por data e resume o status do dia. */
function agruparPorData(slots: Disponibilidade[]) {
  const grupos = new Map<string, Disponibilidade[]>()
  for (const s of slots) {
    const lista = grupos.get(s.data) ?? []
    lista.push(s)
    grupos.set(s.data, lista)
  }
  return [...grupos.entries()].sort(([a], [b]) => a.localeCompare(b))
}

/** C4 — Gerenciar disponibilidade (UC06). */
export default function GerenciarDisponibilidade() {
  const qc = useQueryClient()
  const hoje = new Date()
  const [periodo, setPeriodo] = useState({ de: dataISO(hoje), ate: dataISO(new Date(hoje.getTime() + 21 * 86400000)) })
  const [erroAcao, setErroAcao] = useState<string | null>(null)
  const [feedback, setFeedback] = useState<string | null>(null)

  const { data, isLoading, isError } = useQuery({
    queryKey: ['disp-admin', periodo],
    queryFn: () => disponibilidadeApi.admin(periodo.de, periodo.ate),
  })

  const [form, setForm] = useState({
    data: dataISO(new Date(hoje.getTime() + 86400000)),
    horaInicio: '14:00',
    horaFim: '16:00',
    duracaoMinutos: 30,
    modalidade: 'ONLINE' as 'ONLINE' | 'PRESENCIAL' | 'HIBRIDO',
    local: '',
    linkReuniao: '',
  })

  const criar = useMutation({
    mutationFn: () =>
      disponibilidadeApi.criarLote({
        ...form,
        local: form.local || undefined,
        linkReuniao: form.linkReuniao || undefined,
      }),
    onSuccess: (slots) => {
      setFeedback(`${slots.length} horário(s) criado(s) em ${form.data}.`)
      setErroAcao(null)
      qc.invalidateQueries({ queryKey: ['disp-admin'] })
    },
    onError: (err) => {
      setErroAcao(erro(err))
      setFeedback(null)
    },
  })

  const alternarBloqueio = useMutation({
    mutationFn: (slot: Disponibilidade) =>
      slot.status === 'BLOQUEADO'
        ? disponibilidadeApi.desbloquear(slot.id)
        : disponibilidadeApi.bloquear(slot.id),
    onSuccess: () => {
      setErroAcao(null)
      qc.invalidateQueries({ queryKey: ['disp-admin'] })
    },
    onError: (err) => setErroAcao(erro(err)),
  })

  const campo = 'mt-1 w-full rounded border border-hairline px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-brand'

  return (
    <section aria-labelledby="titulo-disp">
      <h1 id="titulo-disp" className="text-xl font-bold text-ink">Gerenciar disponibilidade</h1>
      <p className="mt-1 text-sm text-ink-3">
        O intervalo informado é dividido em horários individuais (ex.: 14:00–16:00 com 30 min → 4 horários).
      </p>

      {/* criação em lote */}
      <form
        className="mt-6 rounded-none border border-hairline bg-white p-4"
        onSubmit={(e) => {
          e.preventDefault()
          criar.mutate()
        }}
        aria-labelledby="titulo-novo-lote"
      >
        <h2 id="titulo-novo-lote" className="font-medium text-ink-2">Novo lote de horários</h2>
        <div className="mt-3 grid grid-cols-2 gap-3 md:grid-cols-4">
          <div>
            <label htmlFor="l-data" className="block text-xs font-medium text-ink-2">Data *</label>
            <input id="l-data" type="date" required value={form.data}
              onChange={(e) => setForm((f) => ({ ...f, data: e.target.value }))} className={campo} />
          </div>
          <div>
            <label htmlFor="l-inicio" className="block text-xs font-medium text-ink-2">Início *</label>
            <input id="l-inicio" type="time" required value={form.horaInicio}
              onChange={(e) => setForm((f) => ({ ...f, horaInicio: e.target.value }))} className={campo} />
          </div>
          <div>
            <label htmlFor="l-fim" className="block text-xs font-medium text-ink-2">Fim *</label>
            <input id="l-fim" type="time" required value={form.horaFim}
              onChange={(e) => setForm((f) => ({ ...f, horaFim: e.target.value }))} className={campo} />
          </div>
          <div>
            <label htmlFor="l-duracao" className="block text-xs font-medium text-ink-2">Duração (min) *</label>
            <input id="l-duracao" type="number" min={10} max={240} step={5} required value={form.duracaoMinutos}
              onChange={(e) => setForm((f) => ({ ...f, duracaoMinutos: Number(e.target.value) }))} className={campo} />
          </div>
          <div>
            <label htmlFor="l-modalidade" className="block text-xs font-medium text-ink-2">Modalidade *</label>
            <select id="l-modalidade" value={form.modalidade}
              onChange={(e) => setForm((f) => ({ ...f, modalidade: e.target.value as typeof form.modalidade }))}
              className={campo + ' bg-white'}>
              <option value="ONLINE">Online</option>
              <option value="PRESENCIAL">Presencial</option>
              <option value="HIBRIDO">Híbrido</option>
            </select>
          </div>
          <div className={form.modalidade !== 'ONLINE' ? '' : 'opacity-50'}>
            <label htmlFor="l-local" className="block text-xs font-medium text-ink-2">
              Local <span className="text-ink-3 font-normal">(opcional)</span>
            </label>
            <input id="l-local" value={form.local} placeholder={form.modalidade !== 'ONLINE' ? 'Vazio usa o local da coordenação' : ''}
              onChange={(e) => setForm((f) => ({ ...f, local: e.target.value }))} className={campo} />
          </div>
          <div className="flex items-end">
            <p className="text-xs text-ink-3">
              Atendimento online usa a conversa do sistema (sala automática) — sem link externo.
            </p>
          </div>
          <div className="flex items-end">
            <button type="submit" disabled={criar.isPending}
              className="w-full rounded bg-brand hover:bg-brand-dark disabled:opacity-60 text-white text-sm font-medium py-2 focus:outline-none focus:ring-2 focus:ring-brand focus:ring-offset-2">
              {criar.isPending ? 'Criando…' : 'Criar horários'}
            </button>
          </div>
        </div>

        {erroAcao && <p role="alert" className="mt-3 rounded bg-red-50 border border-red-200 text-red-800 text-sm px-3 py-2">{erroAcao}</p>}
        {feedback && <p role="status" className="mt-3 rounded bg-emerald-50 border border-emerald-200 text-emerald-800 text-sm px-3 py-2">{feedback}</p>}
      </form>

      {/* filtros de período */}
      <div className="mt-6 flex flex-wrap items-end gap-3">
        <div>
          <label htmlFor="f-de" className="block text-xs font-medium text-ink-2">De</label>
          <input id="f-de" type="date" value={periodo.de}
            onChange={(e) => setPeriodo((p) => ({ ...p, de: e.target.value }))} className={campo + ' mt-1'} />
        </div>
        <div>
          <label htmlFor="f-ate" className="block text-xs font-medium text-ink-2">Até</label>
          <input id="f-ate" type="date" value={periodo.ate}
            onChange={(e) => setPeriodo((p) => ({ ...p, ate: e.target.value }))} className={campo + ' mt-1'} />
        </div>
      </div>

      {/* agenda agrupada por dia */}
      {isLoading && <p className="mt-6 text-ink-3">Carregando…</p>}
      {isError && <p role="alert" className="mt-6 text-red-700">Não foi possível carregar a agenda.</p>}
      {data && data.length === 0 && (
        <p className="mt-6 rounded-none border border-dashed border-hairline bg-white p-6 text-center text-ink-3">
          Nenhum horário neste período.
        </p>
      )}

      <div className="mt-6 space-y-6">
        {data &&
          agruparPorData(data).map(([dataIso, slots]) => (
            <div key={dataIso}>
              <h3 className="text-sm font-semibold text-ink-2">
                {fmtDataSemana(dataIso)}
              </h3>
              <ul className="mt-2 grid gap-2 sm:grid-cols-2 lg:grid-cols-3">
                {slots.map((slot) => (
                  <li key={slot.id} className="rounded-none border border-hairline bg-white p-3 flex items-center justify-between gap-2">
                    <div>
                      <p className="font-medium text-ink text-sm">
                        {fmtHora(slot.horaInicio)} – {fmtHora(slot.horaFim)}
                        <span className="ml-2 text-xs font-normal text-ink-3">{MODALIDADE_TEXTO[slot.modalidade]}</span>
                      </p>
                      <p className="text-xs text-ink-3">
                        {slot.local && <>{slot.local} · </>}
                        {slot.duracaoMinutos} min
                      </p>
                    </div>
                    <div className="flex items-center gap-2">
                      <span className={`rounded-full border px-2 py-0.5 text-xs ${STATUS_BADGE[slot.status]}`}>
                        {slot.status === 'DISPONIVEL' ? 'Disponível' : slot.status === 'BLOQUEADO' ? 'Bloqueado' : slot.status === 'RESERVADO' ? 'Reservado' : 'Expirado'}
                      </span>
                      {slot.status !== 'RESERVADO' && (
                        <button
                          onClick={() => alternarBloqueio.mutate(slot)}
                          className="rounded border border-hairline text-xs px-2 py-1 text-ink-2 hover:bg-panel focus:outline-none focus:ring-2 focus:ring-brand"
                        >
                          {slot.status === 'BLOQUEADO' ? 'Desbloquear' : 'Bloquear'}
                        </button>
                      )}
                    </div>
                  </li>
                ))}
              </ul>
            </div>
          ))}
      </div>
    </section>
  )
}
