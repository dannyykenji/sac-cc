import type { ReactNode } from 'react'

/** Página de conteúdo claro com título/subtítulo (padrão do mockup). */
export function CabecalhoPagina({ titulo, subtitulo, acoes }: {
  titulo: string
  subtitulo?: string
  acoes?: ReactNode
}) {
  return (
    <div className="flex items-end justify-between gap-4 flex-wrap">
      <div>
        <h1 className="text-2xl font-extrabold text-ink tracking-tight">{titulo}</h1>
        {subtitulo && <p className="text-sm text-ink-3 mt-1">{subtitulo}</p>}
      </div>
      {acoes}
    </div>
  )
}

/** Card branco com hairline (padrão do mockup). */
export function Card({ titulo, acoes, children, className = '' }: {
  titulo?: string
  acoes?: ReactNode
  children: ReactNode
  className?: string
}) {
  return (
    <section className={`bg-white border border-hairline rounded-lg ${className}`}>
      {(titulo || acoes) && (
        <header className="flex items-center justify-between gap-3 px-6 pt-5 pb-3">
          {titulo && <h2 className="font-extrabold text-ink">{titulo}</h2>}
          {acoes}
        </header>
      )}
      <div className="px-6 pb-6">{children}</div>
    </section>
  )
}

const CORES_STATUS: Record<string, string> = {
  // agendamento
  SOLICITADO: 'bg-amber-50 text-amber-700 ring-amber-600/20',
  CONFIRMADO: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  CANCELADO: 'bg-red-50 text-red-700 ring-red-600/20',
  REALIZADO: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  CONCLUIDO: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  NAO_COMPARECEU: 'bg-amber-50 text-amber-700 ring-amber-600/20',
  ENCERRADO: 'bg-gray-100 text-gray-600 ring-gray-500/20',
  // resolução
  RESOLVIDO: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  NAO_RESOLVIDO: 'bg-red-50 text-red-700 ring-red-600/20',
  EM_TRATATIVA: 'bg-amber-50 text-amber-700 ring-amber-600/20',
  AUSENCIA: 'bg-amber-50 text-amber-700 ring-amber-600/20',
  // genéricos
  PENDENTE: 'bg-amber-50 text-amber-700 ring-amber-600/20',
  ATIVO: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  INATIVO: 'bg-gray-100 text-gray-600 ring-gray-500/20',
  DISPONIVEL: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  BLOQUEADO: 'bg-gray-100 text-gray-600 ring-gray-500/20',
  RESERVADO: 'bg-amber-50 text-amber-700 ring-amber-600/20',
  EXPIRADO: 'bg-gray-100 text-gray-600 ring-gray-500/20',
  // sala de chat
  ABERTA: 'bg-emerald-50 text-emerald-700 ring-emerald-600/20',
  FECHADA: 'bg-amber-50 text-amber-700 ring-amber-600/20',
  ENCERRADA: 'bg-gray-100 text-gray-600 ring-gray-500/20',
}

const ROTULOS: Record<string, string> = {
  SOLICITADO: 'Solicitado',
  CONFIRMADO: 'Confirmado',
  CANCELADO: 'Cancelado',
  REALIZADO: 'Realizado',
  CONCLUIDO: 'Concluído',
  NAO_COMPARECEU: 'Não compareceu',
  ENCERRADO: 'Encerrado',
  RESOLVIDO: 'Resolvido',
  NAO_RESOLVIDO: 'Não resolvido',
  EM_TRATATIVA: 'Em tratativa',
  AUSENCIA: 'Ausência',
  PENDENTE: 'Pendente',
  ATIVO: 'Ativo',
  INATIVO: 'Inativo',
  DISPONIVEL: 'Disponível',
  BLOQUEADO: 'Bloqueado',
  RESERVADO: 'Reservado',
  EXPIRADO: 'Expirado',
  ONLINE: 'Online',
  PRESENCIAL: 'Presencial',
  HIBRIDO: 'Híbrido',
  ABERTA: 'Aberta',
  FECHADA: 'Fechada',
  ENCERRADA: 'Encerrada',
}

/** Badge de status: cor + TEXTO (nunca só cor — RNF47/50). */
export function Badge({ valor, className = '' }: { valor: string; className?: string }) {
  const cor = CORES_STATUS[valor] ?? 'bg-gray-100 text-gray-600 ring-gray-500/20'
  const rotulo = ROTULOS[valor] ?? valor
  return (
    <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold ring-1 ring-inset ${cor} ${className}`}>
      {rotulo}
    </span>
  )
}
