import axios from 'axios'

/**
 * Cliente HTTP da SPA (ADR-002/004): mesma origem via proxy Vite em dev;
 * CSRF lido do cookie XSRF-TOKEN e reenviado no header (padrão SPA Spring Security).
 */
const api = axios.create({ baseURL: '/api', withCredentials: true })

function lerCookie(nome: string): string | null {
  const entrada = document.cookie
    .split('; ')
    .find((c) => c.startsWith(nome + '='))
  return entrada ? decodeURIComponent(entrada.slice(nome.length + 1)) : null
}

api.interceptors.request.use((config) => {
  const token = lerCookie('XSRF-TOKEN')
  if (token) config.headers['X-XSRF-TOKEN'] = token
  return config
})

/** Garante que o cookie XSRF exista antes do primeiro POST (ex.: login). */
export async function iniciarSessaoCsrf(): Promise<void> {
  await axios.get('/api/auth/csrf', { withCredentials: true })
}

export default api
