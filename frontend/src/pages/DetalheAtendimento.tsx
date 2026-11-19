import { useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { agendaApi } from '../api/agenda'
import { atendimentoApi } from '../api/atendimento'
import { Badge, CabecalhoPagina, Card } from '../components/ui'
import type { RegistrarAtendimentoPayload } from '../api/tipos'
import { fmtData, fmtDataSemana, fmtHora } from '../lib/formato'

const CAMPO = 'w-full bg-white border border-hairline rounded px-3 py-2.5 text-sm'

/**
 * C2/C3 — Detalhe do atendimento + registro (UC07/08).
 * Campos condicionais RN07–RN09: solução (RESOLVIDO), prazo (EM_TRATATIVA),
 * observações (NAO_RESOLVIDO). Ausência não envia statusResolucao (RN12).
 */
export default function DetalheAtendimento() {
  const { id } = useParams<{ id: string }>()
  const agendamentoId = Number(id)
  const nav = useNavigate()
  const qc = useQueryClient()

  // busca o agendamento pela agenda admin (GET /api/agendamentos/{id} é só do aluno, RNF18)
  const { data: ag, isLoading } = useQuery({
    queryKey: ['agenda', 'item', agendamentoId],
    queryFn: async () => {
      const page = await agendaApi.listar({ de: '2000-01-01', ate: '2100-12-31', size: 200 })
      return page.content.find((x) => x.id === agendamentoId) ?? null
    },
    enabled: Number.isFinite(agendamentoId),
  })
  const { data: atendimento } = useQuery({
    queryKey: ['atendimento', agendamentoId],
    queryFn: () => atendimentoApi.detalhe(agendamentoId),
  })
  const { data: historico = [] } = useQuery({
    queryKey: ['atendimento', 'historico', agendamentoId],
    queryFn: () => atendimentoApi.historico(agendamentoId),
  })

  const [realizado, setRealizado] = useState<boolean | null>(null)
  const [statusResolucao, setStatusResolucao] = useState('')
  const [solucao, setSolucao] = useState('')
  const [observacoes, setObservacoes] = useState('')
  const [prazoRetorno, setPrazoRetorno] = useState('')
  const [encaminhamento, setEncaminhamento] = useState('')
  const [erro, setErro] = useState<string | null>(null)

  const registrar = useMutation({
    mutationFn: (p: RegistrarAtendimentoPayload) => atendimentoApi.registrar(p),
    onSuccess: () => {
      qc.invalidateQueries()
      nav('/app/agenda')
    },
    onError: (e: { response?: { data?: { detail?: string; campos?: Record<string, string> } } }) => {
      const d = e.response?.data
      setErro(d?.detail ?? Object.values(d?.campos ?? {})[0] ?? 'Não foi possível registrar o atendimento.')
    },
  })

  if (isLoading || !ag) return <p className="text-sm text-ink-3">Carregando atendimento…</p>

  const ativo = ag.status === 'SOLICITADO' || ag.status === 'CONFIRMADO'
  const jaRegistrado = atendimento !== undefined && atendimento.realizado !== undefined && atendimento.statusResolucao !== null && atendimento.dataRealizacao !== null
  const podeRegistrar =
    realizado !== null &&
    (realizado === false || (statusResolucao !== '' &&
      (statusResolucao !== 'RESOLVIDO' || solucao.trim() !== '') &&
      (statusResolucao !== 'EM_TRATATIVA' || prazoRetorno !== '') &&
      (statusResolucao !== 'NAO_RESOLVIDO' || observacoes.trim() !== '')))

  function enviar() {
    if (realizado === null) return
    const payload: RegistrarAtendimentoPayload = {
      agendamentoId,
      realizado,
      ...(realizado && statusResolucao
        ? {
            statusResolucao,
            ...(statusResolucao === 'RESOLVIDO' && solucao.trim() ? { solucao: solucao.trim() } : {}),
            ...(statusResolucao === 'NAO_RESOLVIDO' && observacoes.trim() ? { observacoes: observacoes.trim() } : {}),
            ...(statusResolucao === 'EM_TRATATIVA' && prazoRetorno ? { prazoRetorno } : {}),
            ...(encaminhamento.trim() ? { encaminhamento: encaminhamento.trim() } : {}),
          }
        : {}),
    }
    registrar.mutate(payload)
  }

  return (
    <div className="max-w-3xl mx-auto space-y-6">
      <CabecalhoPagina
        titulo="Detalhe do Atendimento"
        subtitulo={`Protocolo ${ag.protocolo}`}
        acoes={
          <div className="flex flex-wrap items-center gap-3">
            {ag.modalidade !== 'PRESENCIAL' && ag.status !== 'CANCELADO' && (
              <Link
                to={`/app/chat/${ag.id}`}
                className={
                  ativo
                    ? 'rounded bg-brand hover:bg-brand-dark text-white text-sm font-bold px-4 py-2 transition-colors focus:outline-none focus:ring-2 focus:ring-brand focus:ring-offset-2'
                    : 'rounded border border-brand text-brand hover:bg-brand-soft text-sm font-bold px-4 py-2 transition-colors focus:outline-none focus:ring-2 focus:ring-brand focus:ring-offset-2'
                }
              >
                {ativo ? 'Entrar na conversa' : 'Histórico da conversa'}
              </Link>
            )}
            <button type="button" onClick={() => nav('/app/agenda')} className="text-sm font-bold text-brand hover:underline">
              Voltar à agenda
            </button>
          </div>
        }
      />

      <Card titulo="Agendamento">
        <dl className="grid grid-cols-[auto_1fr] gap-x-6 gap-y-2.5 text-sm">
          <dt className="text-ink-3">Data/Hora</dt>
          <dd className="font-semibold text-ink capitalize">
            {fmtDataSemana(ag.data)}{' '}
            · {fmtHora(ag.horaInicio)}–{fmtHora(ag.horaFim)}
          </dd>
          <dt className="text-ink-3">Modalidade</dt>
          <dd className="font-semibold text-ink">
            {ag.modalidade === 'ONLINE' ? 'Online' : 'Presencial'}
          </dd>
          <dt className="text-ink-3">Assunto</dt>
          <dd className="text-ink-2">{ag.assunto}</dd>
          {ag.descricao && (<><dt className="text-ink-3">Descrição</dt><dd className="text-ink-2">{ag.descricao}</dd></>)}
          <dt className="text-ink-3">Status</dt>
          <dd><Badge valor={ag.status} /></dd>
        </dl>
      </Card>

      {(jaRegistrado || historico.length > 0) && (
        <Card titulo="Atendimento registrado">
          {atendimento && (
            <dl className="grid grid-cols-[auto_1fr] gap-x-6 gap-y-2.5 text-sm">
              <dt className="text-ink-3">Comparecimento</dt>
              <dd className="font-semibold text-ink">{atendimento.realizado ? 'Compareceu' : 'Não compareceu'}</dd>
              <dt className="text-ink-3">Resolução</dt>
              <dd><Badge valor={atendimento.statusResolucao || 'PENDENTE'} /></dd>
              {atendimento.solucaoRegistrada && (<><dt className="text-ink-3">Solução</dt><dd className="text-ink-2">{atendimento.solucaoRegistrada}</dd></>)}
              {atendimento.prazoRetorno && (<><dt className="text-ink-3">Prazo de retorno</dt><dd className="text-ink-2">{fmtData(atendimento.prazoRetorno)}</dd></>)}
              {atendimento.observacoes && (<><dt className="text-ink-3">Observações (internas)</dt><dd className="text-ink-2">{atendimento.observacoes}</dd></>)}
            </dl>
          )}
          {historico.length > 0 && (
            <>
              <h3 className="text-xs font-bold uppercase tracking-wide text-ink-3 mt-5 mb-2">Histórico</h3>
              <ol className="border-l-2 border-hairline ml-2 space-y-3">
                {historico.map((h) => (
                  <li key={h.id} className="ml-3">
                    <p className="text-sm text-ink-2">
                      <span className="font-bold">{h.statusAnterior ?? '—'} → {h.statusNovo}</span>{' '}
                      <span className="text-ink-3 text-xs">
                        {h.dataRegistro ? new Date(h.dataRegistro).toLocaleString('pt-BR') : ''}
                      </span>
                    </p>
                    {h.descricao && <p className="text-xs text-ink-3">{h.descricao}</p>}
                  </li>
                ))}
              </ol>
            </>
          )}
        </Card>
      )}

      {!jaRegistrado && (
        <Card titulo="Registrar atendimento">
          {erro && (
            <p role="alert" className="mb-4 bg-red-50 border border-red-200 text-red-700 text-sm rounded px-4 py-3">{erro}</p>
          )}
          <fieldset>
            <legend className="text-sm font-bold text-ink-2 mb-2">O aluno compareceu?</legend>
            <div className="flex gap-2">
              {([true, false] as const).map((v) => (
                <button
                  key={String(v)}
                  type="button"
                  onClick={() => { setRealizado(v); setStatusResolucao('') }}
                  aria-pressed={realizado === v}
                  className={`px-4 py-2.5 rounded border text-sm font-bold ${
                    realizado === v ? 'bg-brand text-white border-brand' : 'border-hairline hover:border-brand'
                  }`}
                >
                  {v ? 'Compareceu' : 'Não compareceu'}
                </button>
              ))}
            </div>
          </fieldset>

          {realizado === true && (
            <div className="mt-5 space-y-4">
              <div>
                <label className="block text-sm font-bold text-ink-2 mb-1.5">Status da resolução *</label>
                <div className="grid grid-cols-3 gap-2">
                  {(['RESOLVIDO', 'EM_TRATATIVA', 'NAO_RESOLVIDO'] as const).map((s) => (
                    <button
                      key={s}
                      type="button"
                      onClick={() => setStatusResolucao(s)}
                      aria-pressed={statusResolucao === s}
                      className={`px-3 py-2.5 rounded border text-sm font-bold ${
                        statusResolucao === s ? 'bg-brand text-white border-brand' : 'border-hairline hover:border-brand'
                      }`}
                    >
                      {s === 'RESOLVIDO' ? 'Resolvido' : s === 'EM_TRATATIVA' ? 'Em tratativa' : 'Não resolvido'}
                    </button>
                  ))}
                </div>
              </div>
              {statusResolucao === 'RESOLVIDO' && (
                <div>
                  <label htmlFor="solucao" className="block text-sm font-bold text-ink-2 mb-1.5">
                    Solução registrada * <span className="text-ink-3 font-normal">(RN07)</span>
                  </label>
                  <textarea id="solucao" rows={3} value={solucao} onChange={(e) => setSolucao(e.target.value)}
                    placeholder="Como a demanda foi resolvida" className={CAMPO} />
                </div>
              )}
              {statusResolucao === 'EM_TRATATIVA' && (
                <div>
                  <label htmlFor="prazo" className="block text-sm font-bold text-ink-2 mb-1.5">
                    Prazo de retorno * <span className="text-ink-3 font-normal">(RN08)</span>
                  </label>
                  <input id="prazo" type="date" value={prazoRetorno} onChange={(e) => setPrazoRetorno(e.target.value)} className={CAMPO} />
                </div>
              )}
              {statusResolucao === 'NAO_RESOLVIDO' && (
                <div>
                  <label htmlFor="obs" className="block text-sm font-bold text-ink-2 mb-1.5">
                    Observações internas * <span className="text-ink-3 font-normal">(RN09 — não visível ao aluno)</span>
                  </label>
                  <textarea id="obs" rows={3} value={observacoes} onChange={(e) => setObservacoes(e.target.value)}
                    placeholder="Motivo interno da não resolução" className={CAMPO} />
                </div>
              )}
              <div>
                <label htmlFor="encaminhamento" className="block text-sm font-bold text-ink-2 mb-1.5">
                  Encaminhamento <span className="text-ink-3 font-normal">(opcional)</span>
                </label>
                <input id="encaminhamento" value={encaminhamento} onChange={(e) => setEncaminhamento(e.target.value)}
                  placeholder="Ex.: encaminhado à secretaria acadêmica" className={CAMPO} />
              </div>
            </div>
          )}

          {realizado === false && (
            <p className="mt-4 text-sm text-ink-2 bg-panel rounded px-4 py-3">
              Será registrada <strong>ausência do aluno</strong>. O agendamento é encerrado sem status de
              resolução (RN12) e a ausência não entra nos indicadores de resolução.
            </p>
          )}

          <div className="mt-6 flex justify-end gap-2">
            <button type="button" onClick={() => nav('/app/agenda')}
              className="px-4 py-2.5 rounded border border-hairline text-sm font-bold text-ink-2 hover:bg-panel">
              Cancelar
            </button>
            <button type="button" onClick={enviar} disabled={!podeRegistrar || registrar.isPending}
              className="bg-brand hover:bg-brand-dark disabled:opacity-40 text-white font-bold text-sm px-6 py-2.5 rounded transition-colors">
              {registrar.isPending ? 'Registrando…' : 'Registrar Atendimento'}
            </button>
          </div>
        </Card>
      )}
    </div>
  )
}
