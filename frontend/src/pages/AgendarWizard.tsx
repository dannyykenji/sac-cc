import { useMemo, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { agendamentoApi, categoriaApi, type AgendarPayload } from '../api/agendamento'
import { disponibilidadeApi } from '../api/disponibilidade'
import type { Agendamento, Categoria, Disponibilidade } from '../api/tipos'
import { Stepper, BarraAcao } from '../components/Stepper'
import { CabecalhoPagina, Card } from '../components/ui'

const ETAPAS = [
  { titulo: 'Escolha o atendimento', subtitulo: 'tipo de atendimento' },
  { titulo: 'Selecione a data e horário', subtitulo: 'disponibilidade real da coordenação' },
  { titulo: 'Confirme seus dados', subtitulo: 'revise antes de confirmar' },
]

/** Datas em português sem depender de locale do sistema. */
const SEMANA_CURTO = ['dom', 'seg', 'ter', 'qua', 'qui', 'sex', 'sáb']
const MESES = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez']

function iso(d: Date): string {
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

/** Grade mensal estilo mockup: semana começando no domingo, dias passados desabilitados. */
function gradeMensal(ano: number, mes: number): (Date | null)[] {
  const primeiro = new Date(ano, mes, 1)
  const dias: (Date | null)[] = []
  for (let i = 0; i < primeiro.getDay(); i++) dias.push(null)
  const ultimo = new Date(ano, mes + 1, 0).getDate()
  for (let d = 1; d <= ultimo; d++) dias.push(new Date(ano, mes, d))
  return dias
}

export default function AgendarWizard() {
  const nav = useNavigate()
  const qc = useQueryClient()
  const [etapa, setEtapa] = useState(0)
  const [categoria, setCategoria] = useState<number | null>(null)
  const [mesRef, setMesRef] = useState(() => new Date())
  const [dia, setDia] = useState<string | null>(null)
  const [slot, setSlot] = useState<Disponibilidade | null>(null)
  const [modalidade, setModalidade] = useState<'ONLINE' | 'PRESENCIAL' | null>(null)
  const [assunto, setAssunto] = useState('')
  const [descricao, setDescricao] = useState('')
  const [erro, setErro] = useState<string | null>(null)

  const { data: categorias = [] } = useQuery({
    queryKey: ['categorias'],
    queryFn: categoriaApi.listar,
  })

  // janela de consulta: mês visível completo (folga p/ slots de madrugada)
  const [de, ate] = useMemo(() => {
    const ini = new Date(mesRef.getFullYear(), mesRef.getMonth(), 1)
    const fim = new Date(mesRef.getFullYear(), mesRef.getMonth() + 1, 0)
    return [iso(ini), iso(fim)]
  }, [mesRef])

  const { data: slots = [], isLoading: carregandoSlots } = useQuery({
    queryKey: ['disponibilidades', 'publicas', de, ate],
    queryFn: () => disponibilidadeApi.publicas(de, ate),
    enabled: etapa >= 1,
  })

  const disponiveis = useMemo(
    () => slots.filter((s) => s.status === 'DISPONIVEL' && new Date(`${s.data}T${s.horaInicio}:00`) > new Date()),
    [slots],
  )
  const diasComVaga = useMemo(() => new Set(disponiveis.map((s) => s.data)), [disponiveis])
  const slotsDoDia = useMemo(() => disponiveis.filter((s) => s.data === dia), [disponiveis, dia])

  const agendar = useMutation({
    mutationFn: (p: AgendarPayload) => agendamentoApi.agendar(p),
    onSuccess: (a: Agendamento) => {
      qc.invalidateQueries()
      nav('/app/agendamento-confirmado', { state: { agendamento: a } })
    },
    onError: (e: { response?: { data?: { detail?: string } } }) => {
      setErro(e.response?.data?.detail ?? 'Não foi possível concluir o agendamento. Tente novamente.')
      if (String(e.response?.data?.detail ?? '').match(/ocupad|conflit/i)) {
        setSlot(null)
        setEtapa(1)
      }
    },
  })

  const hoje = new Date()
  const grade = gradeMensal(mesRef.getFullYear(), mesRef.getMonth())
  const diaSel = dia ? new Date(`${dia}T12:00:00`) : null
  const rotuloDiaSel = diaSel
    ? `${SEMANA_CURTO[diaSel.getDay()]}, ${diaSel.getDate()} de ${MESES[diaSel.getMonth()]}`
    : ''

  function podeAvancar(): boolean {
    if (etapa === 0) return categoria !== null
    if (etapa === 1) return slot !== null && modalidade !== null
    return assunto.trim().length >= 5
  }

  function avancar() {
    setErro(null)
    if (etapa === 1 && slot) {
      setModalidade(slot.modalidade === 'HIBRIDO' ? modalidade : (slot.modalidade as 'ONLINE' | 'PRESENCIAL'))
    }
    setEtapa((e) => Math.min(e + 1, 2))
  }

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <CabecalhoPagina
        titulo="Agendamento de Atendimento"
        subtitulo="Fale com a coordenação do curso — online ou presencial"
      />

      <div className="bg-white border border-hairline rounded-lg px-8 py-6">
        <Stepper etapas={ETAPAS} atual={etapa} />
      </div>

      {erro && (
        <p role="alert" className="bg-red-50 border border-red-200 text-red-700 text-sm rounded px-4 py-3">
          {erro}
        </p>
      )}

      {etapa === 0 && (
        <Card titulo="Tipo de atendimento">
          <p className="text-sm text-ink-3 mb-4">
            Selecione o tipo de atendimento que você precisa. Isso ajuda a coordenação a se preparar.
          </p>
          <label className="block text-sm font-bold text-ink-2 mb-1.5" htmlFor="categoria">
            Tipo de atendimento
          </label>
          <select
            id="categoria"
            value={categoria ?? ''}
            onChange={(e) => setCategoria(e.target.value ? Number(e.target.value) : null)}
            className="w-full max-w-md bg-white border border-hairline rounded px-3 py-2.5 text-sm text-ink"
          >
            <option value="">Selecione…</option>
            {categorias.map((c: Categoria) => (
              <option key={c.id} value={c.id}>{c.nome}</option>
            ))}
          </select>
          <p className="mt-4 text-sm text-ink-3 bg-panel rounded px-4 py-3">
            Os atendimentos duram em média 30 minutos. Após confirmar, você receberá um número de
            protocolo para acompanhar seu atendimento.
          </p>
        </Card>
      )}

      {etapa === 1 && (
        <div className="grid md:grid-cols-[1.2fr_1fr] gap-6 items-start">
          <Card titulo="Data">
            <div className="flex items-center justify-between mb-3">
              <button
                type="button"
                aria-label="Mês anterior"
                onClick={() => setMesRef(new Date(mesRef.getFullYear(), mesRef.getMonth() - 1, 1))}
                className="w-9 h-9 rounded hover:bg-panel text-ink-2 font-bold"
              >
                ‹
              </button>
              <p className="font-bold text-ink capitalize">
                {MESES[mesRef.getMonth()].length ? mesRef.toLocaleDateString('pt-BR', { month: 'long' }) : ''}{' '}
                {mesRef.getFullYear()}
              </p>
              <button
                type="button"
                aria-label="Próximo mês"
                onClick={() => setMesRef(new Date(mesRef.getFullYear(), mesRef.getMonth() + 1, 1))}
                className="w-9 h-9 rounded hover:bg-panel text-ink-2 font-bold"
              >
                ›
              </button>
            </div>
            <div className="grid grid-cols-7 gap-1 text-center text-[11px] font-bold text-ink-3 mb-1">
              {SEMANA_CURTO.map((s) => <span key={s}>{s}</span>)}
            </div>
            <div className="grid grid-cols-7 gap-1">
              {grade.map((d, i) => {
                if (!d) return <span key={`v${i}`} />
                const s = iso(d)
                const passado = d < new Date(hoje.getFullYear(), hoje.getMonth(), hoje.getDate())
                const temVaga = diasComVaga.has(s)
                const sel = dia === s
                return (
                  <button
                    key={s}
                    type="button"
                    disabled={passado || !temVaga}
                    onClick={() => { setDia(s); setSlot(null) }}
                    aria-label={`${d.getDate()} de ${MESES[d.getMonth()]}`}
                    aria-pressed={sel}
                    className={`h-10 rounded text-sm font-semibold transition-colors ${
                      sel
                        ? 'bg-brand text-white'
                        : passado
                          ? 'text-ink-3/50 cursor-not-allowed'
                          : temVaga
                            ? 'bg-brand-soft text-brand hover:bg-brand hover:text-white'
                            : 'text-ink-3 hover:bg-panel'
                    }`}
                  >
                    {d.getDate()}
                  </button>
                )
              })}
            </div>
            <p className="mt-3 text-xs text-ink-3 flex items-center gap-2">
              <span className="inline-block w-3 h-3 rounded-sm bg-brand-soft border border-brand/30" /> dias com
              horários disponíveis
            </p>
          </Card>

          <Card titulo="Horário">
            {!dia && <p className="text-sm text-ink-3">Selecione uma data para ver os horários.</p>}
            {dia && carregandoSlots && <p className="text-sm text-ink-3">Carregando horários…</p>}
            {dia && !carregandoSlots && slotsDoDia.length === 0 && (
              <p className="text-sm text-ink-3">
                Nenhum horário disponível nesta data. Tente outro dia destacado.
              </p>
            )}
            <div className="grid grid-cols-2 gap-2">
              {slotsDoDia.map((s) => {
                const sel = slot?.id === s.id
                return (
                  <button
                    key={s.id}
                    type="button"
                    onClick={() => { setSlot(s); setModalidade(s.modalidade === 'HIBRIDO' ? null : s.modalidade) }}
                    aria-pressed={sel}
                    className={`border rounded px-3 py-2.5 text-sm font-bold transition-colors ${
                      sel ? 'bg-brand text-white border-brand' : 'border-hairline text-ink hover:border-brand hover:text-brand'
                    }`}
                  >
                    {s.horaInicio}–{s.horaFim}
                  </button>
                )
              })}
            </div>
            {slot && slot.modalidade === 'HIBRIDO' && (
              <div className="mt-4">
                <p className="text-sm font-bold text-ink-2 mb-1.5">Modalidade</p>
                <div className="grid grid-cols-2 gap-2">
                  {(['ONLINE', 'PRESENCIAL'] as const).map((m) => (
                    <button
                      key={m}
                      type="button"
                      onClick={() => setModalidade(m)}
                      aria-pressed={modalidade === m}
                      className={`border rounded px-3 py-2 text-sm font-bold ${
                        modalidade === m ? 'bg-brand text-white border-brand' : 'border-hairline hover:border-brand'
                      }`}
                    >
                      {m === 'ONLINE' ? 'Online' : 'Presencial'}
                    </button>
                  ))}
                </div>
              </div>
            )}
            {/* lista textual alternativa (RNF49) */}
            {slotsDoDia.length > 0 && (
              <ul className="mt-4 text-xs text-ink-3 space-y-1 sr-only">
                {slotsDoDia.map((s) => (
                  <li key={`t-${s.id}`}>{s.data} {s.horaInicio}–{s.horaFim} {s.modalidade}</li>
                ))}
              </ul>
            )}
          </Card>
        </div>
      )}

      {etapa === 2 && slot && (
        <Card titulo="Resumo e confirmação">
          <dl className="grid grid-cols-[auto_1fr] gap-x-6 gap-y-3 text-sm">
            <dt className="text-ink-3">Tipo</dt>
            <dd className="font-semibold text-ink">
              {categorias.find((c) => c.id === categoria)?.nome ?? '—'}
            </dd>
            <dt className="text-ink-3">Data</dt>
            <dd className="font-semibold text-ink capitalize">{rotuloDiaSel}</dd>
            <dt className="text-ink-3">Horário</dt>
            <dd className="font-semibold text-ink">
              {slot.horaInicio} às {slot.horaFim} ({slot.duracaoMinutos} min)
            </dd>
            <dt className="text-ink-3">Modalidade</dt>
            <dd className="font-semibold text-ink">
              {modalidade === 'ONLINE' ? 'Online' : 'Presencial'}
              {slot.local && ` — ${slot.local}`}
            </dd>
          </dl>
          <div className="mt-6 space-y-4">
            <div>
              <label htmlFor="assunto" className="block text-sm font-bold text-ink-2 mb-1.5">
                Assunto <span className="text-brand">*</span>
              </label>
              <input
                id="assunto"
                value={assunto}
                onChange={(e) => setAssunto(e.target.value)}
                maxLength={255}
                placeholder="Ex.: Dúvida sobre matriz curricular"
                className="w-full bg-white border border-hairline rounded px-3 py-2.5 text-sm"
              />
            </div>
            <div>
              <label htmlFor="descricao" className="block text-sm font-bold text-ink-2 mb-1.5">
                Descrição <span className="text-ink-3 font-normal">(opcional)</span>
              </label>
              <textarea
                id="descricao"
                value={descricao}
                onChange={(e) => setDescricao(e.target.value)}
                maxLength={2000}
                rows={4}
                placeholder="Conte um pouco mais sobre o que você precisa (isso agiliza o atendimento)"
                className="w-full bg-white border border-hairline rounded px-3 py-2.5 text-sm"
              />
            </div>
            <p className="text-xs text-ink-3">
              Ao confirmar, você receberá um protocolo para acompanhar. Cancelamentos podem ser feitos
              até 24h antes (parametrizável pela coordenação).
            </p>
          </div>
        </Card>
      )}

      <BarraAcao
        rotuloResumo={
          etapa === 0
            ? 'Etapa 1 de 3 —'
            : etapa === 1
              ? 'Etapa 2 de 3 —'
              : 'Etapa 3 de 3 —'
        }
        resumo={
          etapa === 0
            ? categoria
              ? `tipo: ${categorias.find((c) => c.id === categoria)?.nome}`
              : 'selecione o tipo'
            : etapa === 1
              ? slot
                ? `${rotuloDiaSel} às ${slot.horaInicio}`
                : 'selecione data e horário'
              : 'pronto para confirmar'
        }
        rotuloAcao={etapa < 2 ? 'Continuar' : 'Confirmar Agendamento'}
        disabled={!podeAvancar() || agendar.isPending}
        acao={() => {
          if (etapa < 2) avancar()
          else if (slot && modalidade) {
            agendar.mutate({
              disponibilidadeId: slot.id,
              modalidade,
              categoriaId: categoria ?? undefined,
              assunto: assunto.trim(),
              descricao: descricao.trim() || undefined,
            })
          }
        }}
      />
    </div>
  )
}
