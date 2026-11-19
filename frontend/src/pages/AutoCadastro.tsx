import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { authApi } from '../api/saccc'
import type { ApiError } from '../api/tipos'

/** T02 — Auto-cadastro do aluno (ADR-006). Conta nasce PENDENTE. */
export default function AutoCadastro() {
  const navigate = useNavigate()
  const [form, setForm] = useState({ nome: '', email: '', matricula: '', senha: '', periodo: '', turma: '', telefone: '' })
  const [erros, setErros] = useState<Record<string, string>>({})
  const [erroGeral, setErroGeral] = useState<string | null>(null)
  const [enviando, setEnviando] = useState(false)

  function set(campo: string, valor: string) {
    setForm((f) => ({ ...f, [campo]: valor }))
  }

  async function aoEnviar(ev: React.FormEvent) {
    ev.preventDefault()
    setErros({})
    setErroGeral(null)
    setEnviando(true)
    try {
      await authApi.autoCadastro({
        nome: form.nome,
        email: form.email,
        matricula: form.matricula,
        senha: form.senha,
        periodo: form.periodo ? Number(form.periodo) : undefined,
        turma: form.turma || undefined,
        telefone: form.telefone || undefined,
      })
      navigate('/cadastro/recebido')
    } catch (err) {
      const e = err as { response?: { data?: ApiError } }
      const data = e.response?.data
      if (data?.campos) setErros(data.campos)
      setErroGeral(data?.detail ?? 'Não foi possível enviar o cadastro. Verifique os campos.')
    } finally {
      setEnviando(false)
    }
  }

  const campo = 'mt-1 w-full rounded border border-hairline px-3 py-2 focus:outline-none focus:ring-2 focus:ring-brand'

  return (
    <main className="min-h-screen bg-canvas flex items-center justify-center p-6">
      <section className="w-full max-w-md bg-white rounded-none shadow-none p-8" aria-labelledby="titulo-cadastro">
        <h1 id="titulo-cadastro" className="text-xl font-bold text-ink">Criar cadastro de aluno</h1>
        <p className="mt-1 text-sm text-ink-3">
          Seu cadastro será analisado pela coordenação antes de liberar o acesso.
        </p>

        <form onSubmit={aoEnviar} className="mt-6 space-y-4" noValidate>
          <div>
            <label htmlFor="nome" className="block text-sm font-medium text-ink-2">Nome completo *</label>
            <input id="nome" required value={form.nome} onChange={(e) => set('nome', e.target.value)} className={campo} />
            {erros.nome && <p role="alert" className="mt-1 text-xs text-red-700">{erros.nome}</p>}
          </div>
          <div>
            <label htmlFor="email-cad" className="block text-sm font-medium text-ink-2">E-mail *</label>
            <input id="email-cad" type="email" required value={form.email} onChange={(e) => set('email', e.target.value)} className={campo} />
            {erros.email && <p role="alert" className="mt-1 text-xs text-red-700">{erros.email}</p>}
          </div>
          <div>
            <label htmlFor="matricula" className="block text-sm font-medium text-ink-2">Matrícula *</label>
            <input id="matricula" inputMode="numeric" required value={form.matricula} onChange={(e) => set('matricula', e.target.value)} className={campo} />
            {erros.matricula && <p role="alert" className="mt-1 text-xs text-red-700">{erros.matricula}</p>}
          </div>
          <div className="grid grid-cols-2 gap-4">
            <div>
              <label htmlFor="periodo" className="block text-sm font-medium text-ink-2">Período</label>
              <input id="periodo" inputMode="numeric" value={form.periodo} onChange={(e) => set('periodo', e.target.value)} className={campo} />
            </div>
            <div>
              <label htmlFor="turma" className="block text-sm font-medium text-ink-2">Turma</label>
              <input id="turma" value={form.turma} onChange={(e) => set('turma', e.target.value)} className={campo} />
            </div>
          </div>
          <div>
            <label htmlFor="telefone" className="block text-sm font-medium text-ink-2">Telefone</label>
            <input id="telefone" value={form.telefone} onChange={(e) => set('telefone', e.target.value)} className={campo} />
          </div>
          <div>
            <label htmlFor="senha-cad" className="block text-sm font-medium text-ink-2">Senha *</label>
            <input id="senha-cad" type="password" required value={form.senha} onChange={(e) => set('senha', e.target.value)} className={campo} aria-describedby="ajuda-senha" />
            <p id="ajuda-senha" className="mt-1 text-xs text-ink-3">Mínimo de 8 caracteres, com letras e números.</p>
            {erros.senha && <p role="alert" className="mt-1 text-xs text-red-700">{erros.senha}</p>}
          </div>

          {erroGeral && (
            <p role="alert" className="rounded bg-red-50 border border-red-200 text-red-800 text-sm px-3 py-2">{erroGeral}</p>
          )}

          <button
            type="submit" disabled={enviando}
            className="w-full rounded bg-brand hover:bg-brand-dark disabled:opacity-60 text-white font-medium py-2 focus:outline-none focus:ring-2 focus:ring-brand focus:ring-offset-2"
          >
            {enviando ? 'Enviando…' : 'Enviar cadastro'}
          </button>
        </form>

        <p className="mt-6 text-sm text-ink-3">
          Já tem conta?{' '}
          <Link to="/login" className="text-brand hover:underline focus:outline-none focus:ring-2 focus:ring-brand rounded">Entrar</Link>
        </p>
      </section>
    </main>
  )
}
