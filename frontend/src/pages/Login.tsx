import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { Logo } from '../components/Logo'
import type { ApiError } from '../api/tipos'

function mensagemDeErro(err: unknown): string {
  const e = err as { response?: { data?: ApiError; status?: number } }
  const data = e.response?.data
  if (data?.detail) return data.detail
  if (e.response?.status === 429) return 'Muitas tentativas. Aguarde alguns minutos antes de tentar novamente.'
  return 'Não foi possível entrar. Tente novamente.'
}

/** T01 — Login (UC01) — painel esquerdo vinho (mockup) + formulário. */
export default function Login() {
  const { entrar } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('')
  const [senha, setSenha] = useState('')
  const [erro, setErro] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  async function aoEnviar(ev: React.FormEvent) {
    ev.preventDefault()
    setErro(null)
    setEnviando(true)
    try {
      await entrar(email, senha)
      navigate('/app')
    } catch (err) {
      setErro(mensagemDeErro(err))
    } finally {
      setEnviando(false)
    }
  }

  const campo =
    'mt-1 w-full bg-white border border-hairline rounded px-3 py-2.5 text-sm text-ink placeholder:text-ink-3/60'

  return (
    <main className="min-h-screen bg-canvas flex">
      {/* painel de marca (vinho do mockup) */}
      <section className="hidden lg:flex w-[45%] bg-brand text-white flex-col justify-between p-12 relative overflow-hidden">
        <Logo />
        <div className="relative z-10">
          <h1 className="text-3xl font-extrabold leading-snug max-w-md">
            Agende uma conversa com a coordenação do seu curso.
          </h1>
          <p className="mt-3 text-brand-muted max-w-md text-sm leading-relaxed">
            Horários online e presenciais, protocolo para acompanhamento e histórico completo dos
            seus atendimentos.
          </p>
          <ul className="mt-8 space-y-3 text-sm text-white/85">
            <li className="flex items-center gap-3"><span className="num-circle bg-white/10 ring-1 ring-white/20">1</span> Escolha o tipo de atendimento</li>
            <li className="flex items-center gap-3"><span className="num-circle bg-white/10 ring-1 ring-white/20">2</span> Selecione data e horário</li>
            <li className="flex items-center gap-3"><span className="num-circle bg-white/10 ring-1 ring-white/20">3</span> Confirme seus dados</li>
          </ul>
        </div>
        <p className="text-xs text-white/50 relative z-10">
          Coordenação do Curso de Ciência da Computação · Portal do Aluno
        </p>
      </section>

      {/* formulário */}
      <section className="flex-1 flex items-center justify-center p-6">
        <div className="w-full max-w-sm" aria-labelledby="titulo-login">
          <div className="lg:hidden mb-8"><Logo /></div>
          <h2 id="titulo-login" className="text-2xl font-extrabold text-ink">Entrar</h2>
          <p className="mt-1 text-sm text-ink-3">Use sua conta institucional.</p>

          <form onSubmit={aoEnviar} className="mt-8 space-y-4" noValidate>
            <div>
              <label htmlFor="email" className="block text-sm font-bold text-ink-2">E-mail</label>
              <input
                id="email" type="email" autoComplete="email" required
                value={email} onChange={(e) => setEmail(e.target.value)}
                placeholder="nome@faculdade.edu.br"
                className={campo}
              />
            </div>
            <div>
              <label htmlFor="senha" className="block text-sm font-bold text-ink-2">Senha</label>
              <input
                id="senha" type="password" autoComplete="current-password" required
                value={senha} onChange={(e) => setSenha(e.target.value)}
                className={campo}
              />
            </div>

            {erro && (
              <p role="alert" className="bg-red-50 border border-red-200 text-red-700 text-sm rounded px-3 py-2">
                {erro}
              </p>
            )}

            <button
              type="submit" disabled={enviando}
              className="w-full bg-brand hover:bg-brand-dark disabled:opacity-60 text-white font-bold py-2.5 rounded transition-colors"
            >
              {enviando ? 'Entrando…' : 'Entrar'}
            </button>
          </form>

          <p className="mt-6 text-sm text-ink-3">
            É aluno sem conta?{' '}
            <Link to="/cadastro" className="text-brand font-bold hover:underline">Criar cadastro</Link>
          </p>
          <p className="mt-2 text-xs text-ink-3/80">
            Esqueceu a senha? Procure a coordenação ou a administração do sistema.
          </p>
        </div>
      </section>
    </main>
  )
}
