import logoBranco from '../assets/logo-afya-branco.png'
import logoRosa from '../assets/logo-afya.png'

/**
 * Marca institucional (logo oficial Afya — Centro Universitário Ji-Paraná · RO).
 * Padrão: versão BRANCA (sidebar e painel de login são fundos escuros — ADR-007).
 * Use variante="rosa" sobre superfícies claras.
 */
export function Logo({
  compacto = false,
  variante = 'branco',
}: {
  compacto?: boolean
  variante?: 'branco' | 'rosa'
}) {
  const src = variante === 'branco' ? logoBranco : logoRosa
  return (
    <div className="flex items-center gap-3 select-none">
      <img
        src={src}
        alt="Afya — Centro Universitário Ji-Paraná · RO"
        className={compacto ? 'h-7 w-auto' : 'h-8 w-auto'}
      />
    </div>
  )
}
