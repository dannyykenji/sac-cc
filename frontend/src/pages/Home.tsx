import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { agendamentoApi } from '../api/agendamento'
import { estatisticaApi } from '../api/estatistica'
import { agendaApi } from '../api/agenda'
import { adminApi } from '../api/saccc'
import { Badge, CabecalhoPagina, Card } from '../components/ui'
import type { Agendamento } from '../api/tipos'
import { fmtDataCurta, fmtHora } from '../lib/formato'

function dataHora(ag: Agendamento): string {
  return `${fmtDataCurta(ag.data)} · ${fmtHora(ag.horaInicio)}`
}

/** A1 — Home do aluno: saudação + próximos + CTA. */
function HomeAluno() {
  const { usuario } = useAuth()
  const { data: proximos = [] } = useQuery({
    queryKey: ['meus-agendamentos', 'prox'],
    queryFn: agendamentoApi.proximos,
  })
  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <CabecalhoPagina
        titulo={`Olá, ${usuario?.nome.split(' ')[0]}!`}
        subtitulo="Como a coordenação pode ajudar você hoje?"
      />
      <div className="grid md:grid-cols-[1fr_260px] gap-6 items-start">
        <Card titulo="Próximos atendimentos">
          {proximos.length === 0 ? (
            <div className="text-center py-8">
              <p className="text-sm text-ink-2 font-semibold">Você ainda não tem atendimentos agendados</p>
              <p className="text-sm text-ink-3 mt-1">Agende uma conversa com a coordenação do curso.</p>
              <Link
                to="/app/agendar"
                className="inline-block mt-4 bg-brand hover:bg-brand-dark text-white font-bold text-sm px-5 py-2.5 rounded transition-colors"
              >
                + Agendar Atendimento
              </Link>
            </div>
          ) : (
            <ul className="divide-y divide-hairline">
              {proximos.map((ag) => {
                const ativo = ag.status === 'SOLICITADO' || ag.status === 'CONFIRMADO'
                return (
                  <li key={ag.id} className="py-3 flex flex-col sm:flex-row sm:items-center justify-between gap-3 first:pt-0 last:pb-0">
                    <div className="min-w-0">
                      <p className="text-sm font-bold text-ink">
                        {dataHora(ag)}
                        <span className="ml-2 text-xs font-normal text-ink-3">
                          {ag.modalidade === 'ONLINE' ? 'Online' : ag.modalidade === 'PRESENCIAL' ? 'Presencial' : 'Híbrido'}
                        </span>
                      </p>
                      <p className="text-sm text-ink-3 truncate">{ag.assunto}</p>
                    </div>
                    <div className="flex flex-wrap items-center gap-2 shrink-0">
                      <Badge valor={ag.status} />
                      {ativo && ag.modalidade !== 'PRESENCIAL' && (
                        <Link
                          to={`/app/chat/${ag.id}`}
                          className="rounded bg-brand hover:bg-brand-dark text-white text-xs font-bold px-3 py-1.5 whitespace-nowrap focus:outline-none focus:ring-2 focus:ring-brand focus:ring-offset-2"
                        >
                          Entrar na conversa
                        </Link>
                      )}
                    </div>
                  </li>
                )
              })}
            </ul>
          )}
        </Card>
        <Card titulo="Atalhos">
          <ul className="space-y-2 text-sm">
            <li>
              <Link to="/app/agendar" className="text-brand font-bold hover:underline">Agendar atendimento</Link>
            </li>
            <li>
              <Link to="/app/meus-agendamentos" className="text-brand font-bold hover:underline">Meus agendamentos</Link>
            </li>
          </ul>
        </Card>
      </div>
    </div>
  )
}

/** C-home — painel da coordenação: KPIs + próximos da agenda. */
function HomeCoordenacao() {
  const { usuario } = useAuth()
  const { data: resumo } = useQuery({ queryKey: ['estatisticas', 'resumo'], queryFn: () => estatisticaApi.resumo() })
  const { data: agenda } = useQuery({
    queryKey: ['agenda', 'home'],
    queryFn: () => agendaApi.listar({ size: 6 }),
  })
  const kpis = [
    { rotulo: 'Realizados', valor: resumo?.totalRealizados ?? '—' },
    { rotulo: '% Resolução', valor: resumo ? `${resumo.percentualResolucao.toFixed(0).replace('.', ',')}%` : '—' },
    { rotulo: 'Em tratativa', valor: resumo?.totalEmTratativa ?? '—' },
    { rotulo: 'Cancelados', valor: resumo?.totalCancelamentos ?? '—' },
  ]
  return (
    <div className="max-w-5xl mx-auto space-y-6">
      <CabecalhoPagina
        titulo={`Olá, ${usuario?.nome.split(' ')[0]}!`}
        subtitulo="Resumo dos atendimentos da coordenação"
      />
      <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
        {kpis.map((k) => (
          <div key={k.rotulo} className="bg-white border border-hairline rounded-lg px-5 py-4">
            <p className="text-xs font-bold uppercase tracking-wide text-ink-3">{k.rotulo}</p>
            <p className="text-3xl font-extrabold text-brand mt-1">{k.valor}</p>
          </div>
        ))}
      </div>
      <Card titulo="Próximos atendimentos" acoes={
        <Link to="/app/agenda" className="text-sm font-bold text-brand hover:underline">Ver agenda completa</Link>
      }>
        {agenda && agenda.content.length === 0 ? (
          <p className="text-sm text-ink-3 py-4 text-center">Nenhum atendimento na agenda.</p>
        ) : (
          <ul className="divide-y divide-hairline">
            {agenda?.content.map((ag) => {
              const ativo = ag.status === 'SOLICITADO' || ag.status === 'CONFIRMADO'
              return (
                <li key={ag.id} className="py-3 flex flex-col sm:flex-row sm:items-center justify-between gap-3 first:pt-0 last:pb-0">
                  <div className="min-w-0">
                    <p className="text-sm font-bold text-ink capitalize">
                      {dataHora(ag)}
                      <span className="ml-2 text-xs font-normal text-ink-3 normal-case">
                        {ag.modalidade === 'ONLINE' ? 'Online' : ag.modalidade === 'PRESENCIAL' ? 'Presencial' : 'Híbrido'}
                      </span>
                    </p>
                    <p className="text-sm text-ink-3 truncate">{ag.assunto}</p>
                  </div>
                  <div className="flex flex-wrap items-center gap-3 shrink-0">
                    <Badge valor={ag.status} />
                    {ativo && ag.modalidade !== 'PRESENCIAL' && (
                      <Link
                        to={`/app/chat/${ag.id}`}
                        className="rounded bg-brand hover:bg-brand-dark text-white text-xs font-bold px-3 py-1.5 whitespace-nowrap focus:outline-none focus:ring-2 focus:ring-brand focus:ring-offset-2"
                      >
                        Entrar na conversa
                      </Link>
                    )}
                    <Link
                      to={`/app/atendimento/${ag.id}`}
                      className="text-xs font-bold text-brand hover:underline whitespace-nowrap"
                    >
                      {ativo ? 'Registrar' : 'Detalhes'}
                    </Link>
                  </div>
                </li>
              )
            })}
          </ul>
        )}
      </Card>
    </div>
  )
}

/** M-home — Admin: pendências + atalhos. */
function HomeAdmin() {
  const { usuario } = useAuth()
  const qcPend = useQuery({
    queryKey: ['admin', 'pendentes', 'home'],
    queryFn: () => adminApi.pendentes(0),
  })
  const pend = qcPend.data?.totalElements ?? 0
  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <CabecalhoPagina titulo={`Olá, ${usuario?.nome.split(' ')[0]}!`} subtitulo="Painel administrativo" />
      <div className="grid md:grid-cols-2 gap-4">
        <Link to="/app/cadastros-pendentes" className="bg-white border border-hairline rounded-lg p-6 hover:border-brand transition-colors block">
          <p className="text-xs font-bold uppercase tracking-wide text-ink-3">Cadastros pendentes</p>
          <p className="text-3xl font-extrabold text-brand mt-1">{qcPend.isLoading ? '…' : pend}</p>
          <p className="text-sm text-ink-3 mt-1">Aprovar ou rejeitar</p>
        </Link>
        <Link to="/app/usuarios" className="bg-white border border-hairline rounded-lg p-6 hover:border-brand transition-colors block">
          <p className="text-xs font-bold uppercase tracking-wide text-ink-3">Usuários</p>
          <p className="text-sm text-ink-3 mt-1">Gerenciar perfis e status</p>
        </Link>
      </div>
    </div>
  )
}

export default function Home() {
  const { usuario } = useAuth()
  if (usuario?.perfil === 'ALUNO') return <HomeAluno />
  if (usuario?.perfil === 'COORDENADOR') return <HomeCoordenacao />
  return <HomeAdmin />
}
