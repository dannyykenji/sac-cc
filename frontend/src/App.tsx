import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import type { JSX } from 'react'
import { AuthProvider, useAuth } from './auth/AuthContext'
import Layout from './pages/Layout'
import Login from './pages/Login'
import AutoCadastro from './pages/AutoCadastro'
import CadastroRecebido from './pages/CadastroRecebido'
import Home from './pages/Home'
import CadastrosPendentes from './pages/CadastrosPendentes'
import Usuarios from './pages/Usuarios'
import GerenciarDisponibilidade from './pages/GerenciarDisponibilidade'
import AgendarWizard from './pages/AgendarWizard'
import AgendamentoConfirmado from './pages/AgendamentoConfirmado'
import MeusAgendamentos from './pages/MeusAgendamentos'
import AgendaCoordenacao from './pages/AgendaCoordenacao'
import DetalheAtendimento from './pages/DetalheAtendimento'
import SalaChat from './pages/SalaChat'
import Estatisticas from './pages/Estatisticas'
import Auditoria from './pages/Auditoria'

const queryClient = new QueryClient()

function Protegido({ children, perfis }: { children: JSX.Element; perfis?: string[] }) {
  const { usuario, carregando } = useAuth()
  if (carregando) {
    return (
      <main className="min-h-screen bg-canvas flex items-center justify-center p-6">
        <p className="text-ink-3">Carregando…</p>
      </main>
    )
  }
  if (!usuario) return <Navigate to="/login" replace />
  if (perfis && !perfis.includes(usuario.perfil)) return <Navigate to="/app" replace />
  return children
}

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <BrowserRouter>
          <Routes>
            {/* público */}
            <Route path="/login" element={<Login />} />
            <Route path="/cadastro" element={<AutoCadastro />} />
            <Route path="/cadastro/recebido" element={<CadastroRecebido />} />

            {/* autenticado — shell do mockup */}
            <Route path="/app" element={<Protegido><Layout /></Protegido>}>
              <Route index element={<Home />} />
              {/* aluno */}
              <Route path="agendar" element={<Protegido perfis={['ALUNO']}><AgendarWizard /></Protegido>} />
              <Route path="agendamento-confirmado" element={<Protegido perfis={['ALUNO']}><AgendamentoConfirmado /></Protegido>} />
              <Route path="meus-agendamentos" element={<Protegido perfis={['ALUNO']}><MeusAgendamentos /></Protegido>} />
              <Route path="chat/:id" element={<Protegido><SalaChat /></Protegido>} />
              {/* coordenação/admin */}
              <Route path="agenda" element={<Protegido perfis={['COORDENADOR', 'ADMIN']}><AgendaCoordenacao /></Protegido>} />
              <Route path="atendimento/:id" element={<Protegido perfis={['COORDENADOR', 'ADMIN']}><DetalheAtendimento /></Protegido>} />
              <Route path="disponibilidade" element={<Protegido perfis={['COORDENADOR', 'ADMIN']}><GerenciarDisponibilidade /></Protegido>} />
              <Route path="estatisticas" element={<Protegido perfis={['COORDENADOR', 'ADMIN']}><Estatisticas /></Protegido>} />
              {/* admin */}
              <Route path="usuarios" element={<Protegido perfis={['ADMIN']}><Usuarios /></Protegido>} />
              <Route path="cadastros-pendentes" element={<Protegido perfis={['ADMIN']}><CadastrosPendentes /></Protegido>} />
              <Route path="auditoria" element={<Protegido perfis={['ADMIN']}><Auditoria /></Protegido>} />
            </Route>

            {/* legado — redireciona para o shell novo */}
            <Route path="/" element={<Navigate to="/app" replace />} />
            <Route path="/agendar-horario" element={<Navigate to="/app/agendar" replace />} />
            <Route path="/meus-agendamentos" element={<Navigate to="/app/meus-agendamentos" replace />} />
            <Route path="/agendamento-confirmado" element={<Navigate to="/app/agendamento-confirmado" replace />} />
            <Route path="/disponibilidade" element={<Navigate to="/app/disponibilidade" replace />} />
            <Route path="/admin/pendentes" element={<Navigate to="/app/cadastros-pendentes" replace />} />
            <Route path="/admin/usuarios" element={<Navigate to="/app/usuarios" replace />} />
            <Route path="*" element={<Navigate to="/app" replace />} />
          </Routes>
        </BrowserRouter>
      </AuthProvider>
    </QueryClientProvider>
  )
}
