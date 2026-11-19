import type { ReactNode } from 'react'

/** Etapa do wizard de agendamento (mockup: 3 etapas conectadas por linha). */
export interface Etapa {
  titulo: string
  subtitulo?: string
}

interface Props {
  etapas: Etapa[]
  atual: number // 0-based
}

/** Stepper horizontal do mockup: número em círculo, título, sub e trilho conectando. */
export function Stepper({ etapas, atual }: Props) {
  return (
    <ol className="flex items-start gap-0" aria-label="Etapas do agendamento">
      {etapas.map((e, i) => {
        const feito = i < atual
        const ativa = i === atual
        return (
          <li key={e.titulo} className="flex-1 last:flex-none">
            <div className="flex items-start">
              <div className="flex flex-col items-center gap-1">
                <span
                  className={`num-circle border ${
                    ativa
                      ? 'bg-brand text-white border-brand'
                      : feito
                        ? 'bg-brand-soft text-brand border-brand'
                        : 'bg-white text-ink-3 border-hairline'
                  }`}
                  aria-current={ativa ? 'step' : undefined}
                >
                  {feito ? '✓' : i + 1}
                </span>
                <span
                  className={`text-center text-xs leading-tight max-w-28 ${
                    ativa ? 'text-ink font-bold' : feito ? 'text-ink-2' : 'text-ink-3'
                  }`}
                >
                  {e.titulo}
                  {e.subtitulo && <span className="block font-normal text-ink-3">{e.subtitulo}</span>}
                </span>
              </div>
              {i < etapas.length - 1 && (
                <div
                  className={`h-px flex-1 mt-3.5 mx-1 min-w-6 ${i < atual ? 'bg-brand' : 'bg-hairline'}`}
                  aria-hidden
                />
              )}
            </div>
          </li>
        )
      })}
    </ol>
  )
}

/** Barra inferior de ação do wizard (mockup: resumo à esquerda, botão à direita). */
export function BarraAcao({ resumo, rotuloResumo, acao, disabled, rotuloAcao }: {
  resumo?: ReactNode
  rotuloResumo: string
  acao: () => void
  disabled?: boolean
  rotuloAcao: string
}) {
  return (
    <div className="bg-panel border border-hairline rounded-lg px-6 py-4 flex items-center justify-between gap-4">
      <p className="text-sm text-ink-2">
        <span className="text-ink-3">{rotuloResumo}</span> {resumo}
      </p>
      <button
        type="button"
        onClick={acao}
        disabled={disabled}
        className="bg-brand hover:bg-brand-dark disabled:opacity-40 disabled:cursor-not-allowed text-white font-bold text-sm px-6 py-3 rounded focus-visible:outline-brand transition-colors whitespace-nowrap"
      >
        {rotuloAcao}
      </button>
    </div>
  )
}
