import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { agendamentoApi, categoriaApi } from '../api/agendamento'
import { disponibilidadeApi } from '../api/disponibilidade'
import type { ApiError } from '../api/tipos'
import { fmtDataSemana, fmtHora } from '../lib/formato'

const MODALIDADE_TEXTO: Record<string, string> = { ONLINE: 'Online', PRESENCIAL: 'Presencial' }

/** A3 — Formulário de agendamento (UC03). */
export default function Agendar() {
  const { slotId } = useParams<{ slotId: string }>()
  const navigate = useNavigate()
  const qc = useQueryClient()
  const [assunto, setAssunto] = useState('')
  const [descricao, setDescricao] = useState('')
  const [categoriaId, setCategoriaId] = useState<number | ''>('')
  const [modalidade, setModalidade] = useState<'ONLINE' | 'PRESENCIAL' | ''>('')
  const [erros, setErros] = useState<Record<string, string>>({})
  const [erroGeral, setErroGeral] = useState<string | null>(null)

  const { data: slot } = useQuery({
    queryKey: ['slot', slotId],
    queryFn: () => disponibilidadeApi.publicas().then((lista) => lista.find((s) => s.id === Number(slotId))),
    enabled: !!slotId,
  })
  const { data: categorias } = useQuery({ queryKey: ['categorias'], queryFn: categoriaApi.listar })

  const agendar = useMutation({
    mutationFn: () =>
      agendamentoApi.agendar({
        disponibilidadeId: Number(slotId),
        modalidade: modalidade as 'ONLINE' | 'PRESENCIAL',
        categoriaId: categoriaId === '' ? undefined : Number(categoriaId),
        assunto,
        descricao: descricao || undefined,
      }),
    onSuccess: (ag) => {
      qc.invalidateQueries({ queryKey: ['meus-agendamentos'] })
      navigate('/agendamento-confirmado', { state: { agendamento: ag } })
    },
    onError: (err) => {
      const e = err as { response?: { data?: ApiError; status?: number } }
      if (e.response?.status === 409) {
        setErroGeral(e.response.data?.detail ?? 'Este horário acabou de ser ocupado. Escolha outro.')
      } else {
        setErroGeral(e.response?.data?.detail ?? 'Não foi possível agendar. Verifique os campos.')
        if (e.response?.data?.campos) setErros(e.response.data.campos)
      }
    },
  })

  if (!slot) {
    return (
      <section aria-labelledby="titulo-agendar" className="max-w-xl">
        <h1 id="titulo-agendar" className="text-xl font-bold text-slate-900">Agendar atendimento</h1>
        <p className="mt-4 rounded-none border border-dashed border-slate-300 bg-white p-6 text-center text-slate-500">
          Horário não encontrado ou não disponível.{' '}
          <Link to="/app/agendar" className="text-primary-darker hover:underline">Voltar à consulta</Link>
        </p>
      </section>
    )
  }

  const opcoesModalidade: Array<'ONLINE' | 'PRESENCIAL'> =
    slot.modalidade === 'HIBRIDO' ? ['ONLINE', 'PRESENCIAL'] : [slot.modalidade]

  const campo = 'mt-1 w-full rounded-lg border border-slate-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-darker'

  return (
    <section aria-labelledby="titulo-agendar" className="max-w-xl">
      <h1 id="titulo-agendar" className="text-xl font-bold text-slate-900">Agendar atendimento</h1>

      <div className="mt-4 rounded-none border border-emerald-200 bg-emerald-50 p-4">
        <p className="font-medium text-slate-900">
          {fmtDataSemana(slot.data)}
          {' · '}
          {fmtHora(slot.horaInicio)} – {fmtHora(slot.horaFim)}
        </p>
        <p className="text-sm text-slate-600">
          {MODALIDADE_TEXTO[slot.modalidade] ?? slot.modalidade}
          {slot.local ? ` · ${slot.local}` : ''} · {slot.duracaoMinutos} min
        </p>
      </div>

      <form
        className="mt-6 space-y-4"
        onSubmit={(e) => {
          e.preventDefault()
          setErros({})
          setErroGeral(null)
          if (!modalidade) {
            setErros({ modalidade: 'Escolha a modalidade.' })
            return
          }
          agendar.mutate()
        }}
        noValidate
      >
        <div>
          <label htmlFor="ag-modalidade" className="block text-sm font-medium text-slate-700">Modalidade *</label>
          <select
            id="ag-modalidade"
            value={modalidade}
            onChange={(e) => setModalidade(e.target.value as 'ONLINE' | 'PRESENCIAL')}
            className={campo + ' bg-white'}
          >
            <option value="">Selecione…</option>
            {opcoesModalidade.map((m) => (
              <option key={m} value={m}>{MODALIDADE_TEXTO[m]}</option>
            ))}
          </select>
          {erros.modalidade && <p role="alert" className="mt-1 text-xs text-red-700">{erros.modalidade}</p>}
        </div>

        <div>
          <label htmlFor="ag-categoria" className="block text-sm font-medium text-slate-700">Categoria</label>
          <select
            id="ag-categoria"
            value={categoriaId}
            onChange={(e) => setCategoriaId(e.target.value === '' ? '' : Number(e.target.value))}
            className={campo + ' bg-white'}
          >
            <option value="">Selecione…</option>
            {categorias?.map((c) => (
              <option key={c.id} value={c.id}>{c.nome}</option>
            ))}
          </select>
        </div>

        <div>
          <label htmlFor="ag-assunto" className="block text-sm font-medium text-slate-700">Assunto *</label>
          <input
            id="ag-assunto" required maxLength={255} value={assunto}
            onChange={(e) => setAssunto(e.target.value)} className={campo}
            placeholder="Ex.: dúvida sobre estágio supervisionado"
          />
          {erros.assunto && <p role="alert" className="mt-1 text-xs text-red-700">{erros.assunto}</p>}
        </div>

        <div>
          <label htmlFor="ag-descricao" className="block text-sm font-medium text-slate-700">
            Descrição (opcional)
          </label>
          <textarea
            id="ag-descricao" rows={4} maxLength={2000} value={descricao}
            onChange={(e) => setDescricao(e.target.value)} className={campo}
            placeholder="Detalhe sua demanda para aproveitar melhor o atendimento"
          />
        </div>

        {erroGeral && (
          <p role="alert" className="rounded-lg bg-red-50 border border-red-200 text-red-800 text-sm px-3 py-2">
            {erroGeral}{' '}
            {erroGeral.includes('ocupado') && (
              <Link to="/app/agendar" className="underline font-medium">Ver outros horários</Link>
            )}
          </p>
        )}

        <button
          type="submit" disabled={agendar.isPending}
          className="w-full rounded-lg bg-primary hover:bg-primary-dark disabled:opacity-60 text-white font-medium py-2 focus:outline-none focus:ring-2 focus:ring-primary-darker focus:ring-offset-2"
        >
          {agendar.isPending ? 'Confirmando…' : 'Confirmar agendamento'}
        </button>
      </form>
    </section>
  )
}
