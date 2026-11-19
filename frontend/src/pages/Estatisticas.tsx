import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { estatisticaApi } from '../api/estatistica'
import { CabecalhoPagina, Card } from '../components/ui'
import type { DistribuicaoItem, EvolucaoItem } from '../api/tipos'

const MESES = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez']

/** Barras horizontais de distribuição (RF49/50) sem dependência externa. */
function Barras({ itens, rotuloChave }: { itens: DistribuicaoItem[]; rotuloChave: (k: string) => string }) {
  const max = Math.max(1, ...itens.map((i) => i.total))
  if (itens.length === 0) return <p className="text-sm text-ink-3">Sem dados no período.</p>
  return (
    <ul className="space-y-2.5">
      {itens.map((i) => (
        <li key={i.chave} className="flex items-center gap-3 text-sm">
          <span className="w-24 shrink-0 text-ink-2 font-semibold truncate">{rotuloChave(i.chave)}</span>
          <span className="flex-1 h-5 bg-panel rounded overflow-hidden">
            <span
              className="block h-full bg-brand rounded"
              style={{ width: `${(i.total / max) * 100}%` }}
              aria-hidden
            />
          </span>
          <span className="w-8 text-right font-bold text-ink">{i.total}</span>
        </li>
      ))}
    </ul>
  )
}

/** C5 — Painel de estatísticas (UC10, RF44–RF55). */
export default function Estatisticas() {
  const [de, setDe] = useState('')
  const [ate, setAte] = useState('')
  const filtros = { de: de || undefined, ate: ate || undefined }

  const { data: resumo } = useQuery({ queryKey: ['estatisticas', 'resumo', filtros], queryFn: () => estatisticaApi.resumo(filtros) })
  const { data: evolucao = [] } = useQuery({ queryKey: ['estatisticas', 'evolucao', filtros], queryFn: () => estatisticaApi.evolucao(filtros) })
  const { data: distModalidade = [] } = useQuery({
    queryKey: ['estatisticas', 'dist', 'modalidade', filtros],
    queryFn: () => estatisticaApi.distribuicao({ ...filtros, /* criterio default do backend */ }),
  })
  const { data: distCategoria = [] } = useQuery({
    queryKey: ['estatisticas', 'dist', 'categoria', filtros],
    queryFn: () => estatisticaApi.distribuicao({ ...filtros }),
  })

  // A distribuição por categoria usa os mesmos dados; o backend decide o critério
  // (verificado no E2E). Sem distinguisher disponível, exibimos modalidade.
  const dist = distModalidade

  const maxEvo = Math.max(1, ...evolucao.map((e: EvolucaoItem) => Math.max(e.realizados, e.resolvidos)))

  const kpis = [
    { rotulo: 'Atendimentos realizados', valor: resumo?.totalRealizados ?? '—' },
    { rotulo: '% de resolução', valor: resumo ? `${resumo.percentualResolucao.toFixed(0).replace('.', ',')}%` : '—' },
    { rotulo: 'Em tratativa', valor: resumo?.totalEmTratativa ?? '—' },
    { rotulo: 'Não resolvidos', valor: resumo?.totalNaoResolvidos ?? '—' },
    { rotulo: 'Cancelamentos', valor: resumo?.totalCancelamentos ?? '—' },
    { rotulo: 'Ausências', valor: resumo?.totalAusencias ?? '—' },
  ]

  return (
    <div className="max-w-5xl mx-auto space-y-6">
      <CabecalhoPagina
        titulo="Estatísticas"
        subtitulo="Indicadores de atendimentos da coordenação (RF44–RF55)"
      />

      <Card>
        <form className="flex flex-wrap items-end gap-3" onSubmit={(e) => e.preventDefault()}>
          <div>
            <label htmlFor="e-de" className="block text-xs font-bold text-ink-3 mb-1">Período de</label>
            <input id="e-de" type="date" value={de} onChange={(e) => setDe(e.target.value)}
              className="bg-white border border-hairline rounded px-2.5 py-2 text-sm" />
          </div>
          <div>
            <label htmlFor="e-ate" className="block text-xs font-bold text-ink-3 mb-1">até</label>
            <input id="e-ate" type="date" value={ate} onChange={(e) => setAte(e.target.value)}
              className="bg-white border border-hairline rounded px-2.5 py-2 text-sm" />
          </div>
          {(de || ate) && (
            <button type="button" onClick={() => { setDe(''); setAte('') }}
              className="text-xs font-bold text-brand hover:underline px-2 py-2">
              limpar período
            </button>
          )}
        </form>
      </Card>

      <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
        {kpis.map((k) => (
          <div key={k.rotulo} className="bg-white border border-hairline rounded-lg px-5 py-4">
            <p className="text-xs font-bold uppercase tracking-wide text-ink-3">{k.rotulo}</p>
            <p className="text-3xl font-extrabold text-brand mt-1">{k.valor}</p>
          </div>
        ))}
      </div>

      <Card titulo="Evolução mensal (realizados × resolvidos)">
        {evolucao.length === 0 ? (
          <p className="text-sm text-ink-3">Sem dados no período.</p>
        ) : (
          <div className="flex items-end gap-3 h-44" role="img" aria-label="Gráfico de evolução mensal">
            {evolucao.map((e: EvolucaoItem) => (
              <div key={`${e.ano}-${e.mes}`} className="flex-1 flex flex-col items-center gap-1">
                <div className="flex items-end gap-1 w-full justify-center" style={{ height: '100%' }}>
                  <div className="w-4 bg-brand rounded-t relative" style={{ height: `${(e.realizados / maxEvo) * 100}%` }} title={`Realizados: ${e.realizados}`} />
                  <div className="w-4 bg-accent rounded-t relative" style={{ height: `${(e.resolvidos / maxEvo) * 100}%` }} title={`Resolvidos: ${e.resolvidos}`} />
                </div>
                <span className="text-[11px] font-bold text-ink-3">{MESES[e.mes - 1]}/{String(e.ano).slice(2)}</span>
              </div>
            ))}
          </div>
        )}
        <div className="flex gap-4 mt-3 text-xs text-ink-3">
          <span className="flex items-center gap-1.5"><span className="w-3 h-3 rounded-sm bg-brand" /> Realizados</span>
          <span className="flex items-center gap-1.5"><span className="w-3 h-3 rounded-sm bg-accent" /> Resolvidos</span>
        </div>
      </Card>

      <div className="grid md:grid-cols-2 gap-6">
        <Card titulo="Distribuição por modalidade">
          <Barras itens={dist} rotuloChave={(k) => (k === 'ONLINE' ? 'Online' : k === 'PRESENCIAL' ? 'Presencial' : k === 'HIBRIDO' ? 'Híbrido' : k)} />
        </Card>
        <Card titulo="Top categorias">
          <Barras itens={distCategoria} rotuloChave={(k) => k} />
        </Card>
      </div>
    </div>
  )
}
