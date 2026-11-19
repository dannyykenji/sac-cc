/**
 * Formatação determinística em padrão brasileiro — NUNCA depender do locale
 * do navegador (browser de teste en-US renderiza "11/20" em vez de "20/11").
 * Exibição: sempre por estes helpers; inputs <type=date/time> seguem o
 * locale do SO do usuário (padrão da web, RNF62 fuso SP).
 */

const MESES_LONGO = [
  'janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho',
  'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro',
]
const MESES_CURTO = [
  'jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez',
]
const SEMANA_LONGO = ['domingo', 'segunda-feira', 'terça-feira', 'quarta-feira', 'quinta-feira', 'sexta-feira', 'sábado']
const SEMANA_CURTO = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb']

const p2 = (n: number) => String(n).padStart(2, '0')

/** ISO "aaaa-mm-dd" → "20/11/2026" (sem depender de locale). */
export function fmtData(iso: string): string {
  const [a, m, d] = iso.split('-').map(Number)
  if (!a || !m || !d) return iso
  return `${p2(d)}/${p2(m)}/${a}`
}

/** ISO "aaaa-mm-dd" → "20 de novembro de 2026". */
export function fmtDataLongo(iso: string): string {
  const [a, m, d] = iso.split('-').map(Number)
  if (!a || !m || !d) return iso
  return `${d} de ${MESES_LONGO[m - 1]} de ${a}`
}

/** ISO "aaaa-mm-dd" → "quinta-feira, 20 de novembro de 2026". */
export function fmtDataSemana(iso: string): string {
  const [a, m, d] = iso.split('-').map(Number)
  if (!a || !m || !d) return iso
  const dt = new Date(a, m - 1, d, 12, 0, 0)
  return `${SEMANA_LONGO[dt.getDay()]}, ${d} de ${MESES_LONGO[m - 1]} de ${a}`
}

/** ISO "aaaa-mm-dd" → "Sex, 20 nov" (listas compactas). */
export function fmtDataCurta(iso: string): string {
  const [a, m, d] = iso.split('-').map(Number)
  if (!a || !m || !d) return iso
  const dt = new Date(a, m - 1, d, 12, 0, 0)
  return `${SEMANA_CURTO[dt.getDay()]}, ${d} ${MESES_CURTO[m - 1]}`
}

/** "HH:mm[:ss]" do backend → "HH:mm" 24h garantido (já vem 24h; sanitiza). */
export function fmtHora(h: string | null | undefined): string {
  if (!h) return '—'
  const m = /^(\d{1,2}):(\d{2})/.exec(h)
  return m ? `${p2(Number(m[1]))}:${m[2]}` : h
}

export { MESES_LONGO, MESES_CURTO, SEMANA_CURTO }
