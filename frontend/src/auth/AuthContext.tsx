import { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react'
import type { ReactNode } from 'react'
import { authApi } from '../api/saccc'
import type { UsuarioAtual } from '../api/tipos'

interface AuthState {
  usuario: UsuarioAtual | null
  carregando: boolean
  entrar: (email: string, senha: string) => Promise<UsuarioAtual>
  sair: () => Promise<void>
  recarregar: () => Promise<void>
}

const AuthContext = createContext<AuthState | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [usuario, setUsuario] = useState<UsuarioAtual | null>(null)
  const [carregando, setCarregando] = useState(true)

  useEffect(() => {
    authApi
      .me()
      .then(setUsuario)
      .finally(() => setCarregando(false))
  }, [])

  const entrar = useCallback(async (email: string, senha: string) => {
    const u = await authApi.login({ email, senha })
    setUsuario(u)
    return u
  }, [])

  const sair = useCallback(async () => {
    await authApi.logout()
    setUsuario(null)
  }, [])

  const recarregar = useCallback(async () => {
    setUsuario(await authApi.me())
  }, [])

  const value = useMemo(
    () => ({ usuario, carregando, entrar, sair, recarregar }),
    [usuario, carregando, entrar, sair, recarregar],
  )
  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

export function useAuth(): AuthState {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error('useAuth deve ser usado dentro de AuthProvider')
  return ctx
}
