import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Link, useNavigate, useParams } from 'react-router-dom'
import { assinarSala, chatApi } from '../api/chat'
import { useAuth } from '../auth/AuthContext'
import { Badge, CabecalhoPagina, Card } from '../components/ui'
import type { Agendamento, ApiError, EstadoSala, MensagemChat } from '../api/tipos'
import { agendamentoApi } from '../api/agendamento'
import { agendaApi } from '../api/agenda'
import { fmtDataLongo, fmtHora } from '../lib/formato'

const ROTULO_ESTADO: Record<EstadoSala, string> = {
  ABERTA: 'Sala aberta',
  FECHADA: 'Sala fechada',
  ENCERRADA: 'Encerrada — somente leitura',
}

function horaCurta(iso: string): string {
  const d = new Date(iso)
  return `${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}`
}

/**
 * Chat do atendimento online (ADR-008): sala 1:1 por agendamento.
 * Aluno entra pela área dele (meus agendamentos); coordenação pelo detalhe.
 */
export default function SalaChat() {
  const { id } = useParams<{ id: string }>()
  const agendamentoId = Number(id)
  const { usuario } = useAuth()
  const nav = useNavigate()
  const qc = useQueryClient()
  const ehCoordenacao = usuario?.perfil !== 'ALUNO'

  const [texto, setTexto] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const [wsOk, setWsOk] = useState(false)
  const fimRef = useRef<HTMLDivElement>(null)

  // dados do agendamento: aluno usa endpoint próprio; coordenação busca na agenda admin
  const { data: ag } = useQuery({
    queryKey: ['chat', 'agendamento', agendamentoId],
    queryFn: async (): Promise<Agendamento | null> => {
      if (!ehCoordenacao) {
        try {
          return await agendamentoApi.detalhe(agendamentoId)
        } catch {
          return null
        }
      }
      const page = await agendaApi.listar({ de: '2000-01-01', ate: '2100-12-31', size: 200 })
      return page.content.find((x) => x.id === agendamentoId) ?? null
    },
    enabled: Number.isFinite(agendamentoId),
  })

  const { data: estadoResp } = useQuery({
    queryKey: ['chat', 'estado', agendamentoId],
    queryFn: () => chatApi.estado(agendamentoId),
    refetchInterval: 30_000,
    enabled: Number.isFinite(agendamentoId),
  })
  const estado = estadoResp?.estado
  const modoTeste = estadoResp?.modoTeste ?? false

  const { data: mensagens = [], isLoading } = useQuery({
    queryKey: ['chat', 'mensagens', agendamentoId],
    queryFn: () => chatApi.historico(agendamentoId),
    enabled: Number.isFinite(agendamentoId),
  })

  // tempo real: assina o tópico da sala (backend valida o SUBSCRIBE)
  useEffect(() => {
    if (!Number.isFinite(agendamentoId)) return
    return assinarSala(agendamentoId, (m) => {
      qc.setQueryData<MensagemChat[]>(['chat', 'mensagens', agendamentoId], (atuais) =>
        atuais?.some((x) => x.id === m.id) ? atuais : [...(atuais ?? []), m],
      )
    }, setWsOk)
  }, [agendamentoId, qc])

  // rola para a última mensagem
  useEffect(() => {
    fimRef.current?.scrollIntoView({ block: 'end' })
  }, [mensagens.length])

  const enviar = useMutation({
    mutationFn: () => chatApi.enviar(agendamentoId, texto),
    onSuccess: () => {
      setTexto('')
      setErro(null)
      // a mensagem chega pelo tópico; invalidate garante em caso de WS caído
      qc.invalidateQueries({ queryKey: ['chat', 'mensagens', agendamentoId] })
    },
    onError: (e) => {
      const detail = (e as { response?: { data?: ApiError } })?.response?.data?.detail
      setErro(detail ?? 'Não foi possível enviar.')
    },
  })

  if (isLoading) return <p className="text-sm text-ink-3">Carregando conversa…</p>
  if (!ag) {
    return (
      <div className="max-w-xl">
        <CabecalhoPagina titulo="Conversa" />
        <p className="mt-4 text-sm text-ink-3">
          Atendimento não encontrado.{' '}
          <button type="button" onClick={() => nav('/app')} className="text-brand font-bold hover:underline">
            Voltar ao início
          </button>
        </p>
      </div>
    )
  }

  const aberta = estado === 'ABERTA'

  return (
    <div className="max-w-3xl mx-auto flex flex-col" style={{ minHeight: 'calc(100vh - 10rem)' }}>
      <CabecalhoPagina
        titulo={ehCoordenacao ? 'Atendimento Online' : 'Conversa com a Coordenação'}
        subtitulo={`${fmtDataLongo(ag.data)} · ${fmtHora(ag.horaInicio)}–${fmtHora(ag.horaFim)} · protocolo ${ag.protocolo}`}
        acoes={
          <div className="flex flex-wrap items-center gap-3">
            <Badge valor={estado ?? 'FECHADA'} className={estado === 'ABERTA' ? '' : 'opacity-90'} />
            {ehCoordenacao ? (
              <>
                <Link
                  to={`/app/atendimento/${ag.id}`}
                  className="text-sm font-bold text-brand hover:underline"
                >
                  Registrar atendimento
                </Link>
                <Link
                  to="/app/agenda"
                  className="text-sm font-bold text-ink-2 hover:text-brand hover:underline"
                >
                  Voltar à agenda
                </Link>
              </>
            ) : (
              <Link
                to="/app/meus-agendamentos"
                className="text-sm font-bold text-brand hover:underline"
              >
                Meus agendamentos
              </Link>
            )}
          </div>
        }
      />

      {/* modo teste (deploy de avaliação): aviso explícito de que a janela está desativada */}
      {modoTeste && estado !== 'ENCERRADA' && (
        <p
          role="note"
          className="mt-2 text-xs bg-amber-50 border border-amber-200 text-amber-800 rounded px-3 py-2"
        >
          Ambiente de teste: o chat está aberto desde a criação da sala, sem janela de horário.
          Em uso real, a sala abriria 10 minutos antes do horário marcado e fecharia 15 minutos
          após o término, caso o atendimento não fosse registrado.
        </p>
      )}

      {/* status da sala — texto além da cor (RNF47/50) */}
      <p className="mt-2 text-xs text-ink-3" role="status">
        {ROTULO_ESTADO[estado ?? 'FECHADA']}
        {estado === 'FECHADA' && ' — a sala abre 10 minutos antes do horário marcado.'}
        {estado === 'ENCERRADA' && ' — histórico preservado como registro do atendimento.'}
        {!wsOk && aberta && ' · reconectando…'}
      </p>

      <Card className="mt-4 flex-1 flex flex-col !pb-0 overflow-hidden">
        {/* mensagens */}
        <div className="flex-1 overflow-y-auto px-6 py-5 space-y-3" role="log" aria-label="mensagens" style={{ maxHeight: '55vh' }}>
          {mensagens.length === 0 && (
            <p className="text-sm text-ink-3 text-center py-8">
              {aberta
                ? 'Nenhuma mensagem ainda. Envie a primeira para iniciar a conversa.'
                : 'Nenhuma mensagem nesta sala.'}
            </p>
          )}
          {mensagens.map((m) => {
            const minha = m.autorId === usuario?.id
            return (
              <div key={m.id} className={`flex ${minha ? 'justify-end' : 'justify-start'}`}>
                <div
                  className={`max-w-[80%] rounded-lg px-4 py-2.5 ${
                    minha ? 'bg-brand text-white' : 'bg-panel text-ink'
                  }`}
                >
                  {!minha && (
                    <p className="text-[11px] font-bold text-ink-3 mb-0.5">
                      {m.perfilAutor === 'ALUNO' ? m.autorNome : `Coordenação · ${m.autorNome}`}
                    </p>
                  )}
                  <p className="text-sm whitespace-pre-wrap break-words">{m.conteudo}</p>
                  <p className={`text-[10px] mt-1 ${minha ? 'text-white/70' : 'text-ink-3'}`}>
                    {horaCurta(m.dataEnvio)}
                  </p>
                </div>
              </div>
            )
          })}
          <div ref={fimRef} />
        </div>

        {/* composer */}
        <div className="border-t border-hairline px-6 py-4">
          {erro && (
            <p role="alert" className="mb-3 bg-red-50 border border-red-200 text-red-700 text-sm rounded px-3 py-2">
              {erro}
            </p>
          )}
          {aberta ? (
            <form
              className="flex gap-2"
              onSubmit={(e) => {
                e.preventDefault()
                if (texto.trim()) enviar.mutate()
              }}
            >
              <label htmlFor="chat-msg" className="sr-only">Mensagem</label>
              <textarea
                id="chat-msg"
                rows={1}
                cols={1}
                value={texto}
                maxLength={2000}
                onChange={(e) => setTexto(e.target.value)}
                onKeyDown={(e) => {
                  if (e.key === 'Enter' && !e.shiftKey) {
                    e.preventDefault()
                    if (texto.trim()) enviar.mutate()
                  }
                }}
                placeholder="Escreva sua mensagem…"
                className="flex-1 min-w-0 resize-none bg-white border border-hairline rounded px-3 py-2.5 text-sm focus:outline-none focus:ring-2 focus:ring-brand"
              />
              <button
                type="submit"
                disabled={!texto.trim() || enviar.isPending}
                aria-label="Enviar mensagem (Enter)"
                title="Enviar (Enter)"
                className="shrink-0 bg-brand hover:bg-brand-dark disabled:opacity-40 text-white font-bold text-sm px-4 py-2.5 rounded transition-colors focus:outline-none focus:ring-2 focus:ring-brand focus:ring-offset-2"
              >
                {enviar.isPending ? '…' : '➤'}
              </button>
            </form>
          ) : (
            <p className="text-sm text-ink-3 py-2 text-center">
              {estado === 'ENCERRADA'
                ? 'Esta conversa foi encerrada com o registro do atendimento e permanece como histórico.'
                : 'A sala não está aberta para mensagens neste momento.'}
            </p>
          )}
        </div>
      </Card>
    </div>
  )
}
