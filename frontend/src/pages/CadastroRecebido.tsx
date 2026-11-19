import { Link } from 'react-router-dom'

/** T03 — confirmação neutra do auto-cadastro (ADR-006). */
export default function CadastroRecebido() {
  return (
    <main className="min-h-screen bg-canvas flex items-center justify-center p-6">
      <section className="w-full max-w-md bg-white rounded-none shadow-none p-8 text-center" aria-labelledby="titulo-recebido">
        <div aria-hidden className="mx-auto h-12 w-12 rounded-full bg-emerald-100 flex items-center justify-center text-2xl">✓</div>
        <h1 id="titulo-recebido" className="mt-4 text-xl font-bold text-ink">Cadastro recebido</h1>
        <p className="mt-2 text-ink-2">
          Assim que a coordenação aprovar seu cadastro você poderá entrar com seu e-mail e senha.
        </p>
        <Link
          to="/login"
          className="mt-6 inline-block rounded bg-brand hover:bg-brand-dark text-white font-medium px-6 py-2 focus:outline-none focus:ring-2 focus:ring-brand focus:ring-offset-2"
        >
          Voltar para o login
        </Link>
      </section>
    </main>
  )
}
