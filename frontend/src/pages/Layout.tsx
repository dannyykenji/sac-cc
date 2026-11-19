import { useEffect, useRef, useState } from 'react'
import { NavLink, Outlet, useNavigate } from 'react-router-dom'
import { useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../auth/AuthContext'
import { notificacaoApi } from '../api/notificacao'
import { adminApi } from '../api/saccc'
import { Logo } from '../components/Logo'
import type { Notificacao } from '../api/tipos'

interface ItemMenu {
  para: string
  rotulo: string
  perfis: Array<'ALUNO' | 'COORDENADOR' | 'ADMIN'>
  badge?: 'pendentes'
}

const MENU: ItemMenu[] = [
  { para: '/app', rotulo: 'Início', perfis: ['ALUNO', 'COORDENADOR', 'ADMIN'] },
  // aluno
  { para: '/app/agendar', rotulo: 'Agendar Atendimento', perfis: ['ALUNO'] },
  { para: '/app/meus-agendamentos', rotulo: 'Meus Agendamentos', perfis: ['ALUNO'] },
  // coordenação
  { para: '/app/agenda', rotulo: 'Agenda de Atendimentos', perfis: ['COORDENADOR', 'ADMIN'] },
  { para: '/app/disponibilidade', rotulo: 'Gerenciar Disponibilidade', perfis: ['COORDENADOR', 'ADMIN'] },
  { para: '/app/estatisticas', rotulo: 'Estatísticas', perfis: ['COORDENADOR', 'ADMIN'] },
  // admin
  { para: '/app/usuarios', rotulo: 'Usuários', perfis: ['ADMIN'] },
  { para: '/app/cadastros-pendentes', rotulo: 'Cadastros Pendentes', perfis: ['ADMIN'], badge: 'pendentes' },
  { para: '/app/auditoria', rotulo: 'Auditoria', perfis: ['ADMIN'] },
]

function Inicial({ nome }: { nome: string }) {
  const partes = nome.trim().split(/\s+/)
  const ini = ((partes[0]?.[0] ?? '') + (partes.length > 1 ? partes[partes.length - 1][0] : '')).toUpperCase()
  return <span aria-hidden>{ini || '?'}</span>
}

/** Painel de notificações (sininho) — canal SISTEMA (RF56/60). */
function SinoNotificacoes() {
  const [aberto, setAberto] = useState(false)
  const ref = useRef<HTMLDivElement>(null)
  const { data: notificacoes = [] } = useQuery({
    queryKey: ['notificacoes'],
    queryFn: notificacaoApi.minhas,
    refetchInterval: 60_000,
  })

  useEffect(() => {
    if (!aberto) return
    const fora = (e: MouseEvent) => {
      if (ref.current && !ref.current.contains(e.target as Node)) setAberto(false)
    }
    document.addEventListener('mousedown', fora)
    return () => document.removeEventListener('mousedown', fora)
  }, [aberto])

  return (
    <div className="relative" ref={ref}>
      <button
        type="button"
        aria-label={`Notificações (${notificacoes.length})`}
        aria-expanded={aberto}
        onClick={() => setAberto((v) => !v)}
        className="relative w-10 h-10 rounded-full hover:bg-panel flex items-center justify-center text-ink-2"
      >
        <span className="text-lg" aria-hidden>🔔</span>
        {notificacoes.length > 0 && (
          <span className="absolute top-1 right-1 min-w-4 h-4 px-1 rounded-full bg-brand text-white text-[10px] font-bold flex items-center justify-center">
            {notificacoes.length > 9 ? '9+' : notificacoes.length}
          </span>
        )}
      </button>
      {aberto && (
        <div className="absolute right-0 mt-2 w-80 bg-white border border-hairline rounded-lg shadow-lg z-50">
          <p className="px-4 py-3 font-bold text-ink border-b border-hairline text-sm">Notificações</p>
          {notificacoes.length === 0 ? (
            <p className="px-4 py-6 text-sm text-ink-3 text-center">Nenhuma notificação.</p>
          ) : (
            <ul className="max-h-80 overflow-auto divide-y divide-hairline">
              {notificacoes.map((n: Notificacao) => (
                <li key={n.id} className="px-4 py-3">
                  <p className="text-sm font-bold text-ink">{n.assunto}</p>
                  <p className="text-xs text-ink-2 mt-0.5">{n.mensagem}</p>
                  <p className="text-[11px] text-ink-3 mt-1">
                    {n.dataCriacao ? new Date(n.dataCriacao).toLocaleString('pt-BR') : ''}
                  </p>
                </li>
              ))}
            </ul>
          )}
        </div>
      )}
    </div>
  )
}

/**
 * Shell do app (mockup 2026-11-05): sidebar vinho #680D20 260px com marca d'água
 * "A", header claro com sininho + avatar iniciais, conteúdo em canvas claro.
 */
export default function Layout() {
  const { usuario, sair } = useAuth()
  const nav = useNavigate()
  const qc = useQueryClient()
  if (!usuario) return null
  const itens = MENU.filter((i) => i.perfis.includes(usuario.perfil))

  // badge do menu admin: total de cadastros pendentes
  const { data: pendentes } = useQuery({
    queryKey: ['admin', 'pendentes', 'badge'],
    queryFn: () => adminApi.pendentes(0),
    enabled: usuario.perfil === 'ADMIN',
    refetchInterval: 60_000,
  })
  const contagemPendentes = pendentes?.totalElements ?? 0

  async function aoSair() {
    await sair()
    qc.clear()
    nav('/login')
  }

  return (
    <div className="min-h-screen bg-canvas text-ink flex">
      <aside className="w-[260px] shrink-0 bg-brand text-white flex-col relative overflow-hidden hidden md:flex">
        <div className="px-6 pt-6 pb-5 border-b border-white/10">
          <Logo />
        </div>
        <nav className="flex-1 px-3 py-4 space-y-0.5 overflow-y-auto" aria-label="Menu principal">
          {itens.map((i) => (
            <NavLink
              key={i.para}
              to={i.para}
              end={i.para === '/app'}
              className={({ isActive }) =>
                `flex items-center gap-3 px-3 py-2.5 rounded text-sm font-semibold transition-colors ${
                  isActive
                    ? 'bg-white/10 text-white ring-1 ring-inset ring-white/15'
                    : 'text-white/80 hover:bg-white/5 hover:text-white'
                }`
              }
            >
              <span className="flex-1">{i.rotulo}</span>
              {i.badge === 'pendentes' && contagemPendentes > 0 && (
                <span className="bg-white text-brand text-[10px] font-extrabold px-1.5 py-0.5 rounded-full">
                  {contagemPendentes}
                </span>
              )}
            </NavLink>
          ))}
        </nav>
      </aside>

      <div className="flex-1 flex flex-col min-w-0">
        {/* mobile: barra compacta */}
        <div className="md:hidden bg-brand text-white px-4 py-3 flex items-center justify-between">
          <Logo compacto />
          <button type="button" onClick={aoSair} className="border border-white/40 px-3 py-1 text-xs font-bold rounded">
            Sair
          </button>
        </div>
        <div className="md:hidden bg-brand pb-2 px-2 flex gap-1 overflow-x-auto">
          {itens.map((i) => (
            <NavLink
              key={i.para}
              to={i.para}
              end={i.para === '/app'}
              className={({ isActive }) =>
                `whitespace-nowrap px-3 py-1.5 text-xs rounded font-semibold ${
                  isActive ? 'bg-white text-brand' : 'text-white/90 hover:bg-white/10'
                }`
              }
            >
              {i.rotulo}
            </NavLink>
          ))}
        </div>

        <header className="bg-white border-b border-hairline px-8 h-16 hidden md:flex items-center justify-between">
          <p className="text-sm text-ink-3">
            Portal do Aluno ·{' '}
            <span className="text-ink-2 font-semibold">Coordenação do Curso de Ciência da Computação</span>
          </p>
          <div className="flex items-center gap-2">
            <SinoNotificacoes />
            <div className="flex items-center gap-3 pl-3 border-l border-hairline">
              <span className="w-10 h-10 rounded-full bg-brand-soft text-brand font-extrabold flex items-center justify-center text-sm">
                <Inicial nome={usuario.nome} />
              </span>
              <span className="leading-tight">
                <span className="block text-sm font-bold text-ink">{usuario.nome}</span>
                <span className="block text-xs text-ink-3">
                  {usuario.perfil === 'ALUNO' ? 'Aluno' : usuario.perfil === 'COORDENADOR' ? 'Coordenação' : 'Administrador'}
                </span>
              </span>
              <button
                type="button"
                onClick={aoSair}
                className="text-xs font-bold text-ink-3 hover:text-brand px-2 py-1.5 rounded"
              >
                Sair
              </button>
            </div>
          </div>
        </header>
        <main className="flex-1 overflow-y-auto px-4 md:px-8 py-8">
          <Outlet />
        </main>
      </div>
    </div>
  )
}
