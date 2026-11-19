export interface UsuarioAtual {
  id: number
  nome: string
  email: string
  perfil: 'ALUNO' | 'COORDENADOR' | 'ADMIN'
  permissoes: string[]
}

export interface LoginPayload {
  email: string
  senha: string
}

export interface AutoCadastroPayload {
  nome: string
  email: string
  matricula: string
  senha: string
  periodo?: number
  turma?: string
  telefone?: string
}

export interface UsuarioAdmin {
  id: number
  nome: string
  email: string
  perfil: string
  status: 'PENDENTE' | 'ATIVO' | 'INATIVO'
  dataCadastro: string
  matricula: string | null
  curso: string | null
  periodo: number | null
  turma: string | null
  telefone: string | null
  statusMatricula: string | null
  registroFuncional: string | null
  cargo: string | null
}

export interface Page<T> {
  content: T[]
  totalElements: number
  totalPages: number
  number: number
}

/** Erro de API no formato ProblemDetail (RFC 7807) do backend. */
export interface ApiError {
  title?: string
  detail?: string
  campos?: Record<string, string>
}

export interface Disponibilidade {
  id: number
  data: string
  horaInicio: string
  horaFim: string
  duracaoMinutos: number
  modalidade: 'ONLINE' | 'PRESENCIAL' | 'HIBRIDO'
  local: string | null
  linkReuniao: string | null
  status: 'DISPONIVEL' | 'BLOQUEADO' | 'RESERVADO' | 'EXPIRADO'
  observacao: string | null
}

export interface Agendamento {
  id: number
  protocolo: string
  data: string
  horaInicio: string
  horaFim: string
  modalidade: string
  categoriaId: number | null
  assunto: string
  descricao: string | null
  status: string
  dataCancelamento: string | null
  motivoCancelamento: string | null
}

export interface Categoria {
  id: number
  nome: string
}

/** Visão da coordenação do atendimento (inclui interno — RF43 só proíbe p/ aluno). */
export interface Atendimento {
  agendamentoId: number
  responsavelId: number
  realizado: boolean
  statusResolucao: string
  dataRealizacao: string | null
  observacoes: string | null
  solucaoRegistrada: string | null
  encaminhamento: string | null
  prazoRetorno: string | null
  dataEncerramento: string | null
  confirmadoAluno: boolean
}

export interface HistoricoAtendimento {
  id: number
  statusAnterior: string | null
  statusNovo: string
  descricao: string
  usuarioId: number
  dataRegistro: string
}

export interface RegistrarAtendimentoPayload {
  agendamentoId: number
  realizado: boolean
  statusResolucao?: string
  observacoes?: string
  solucao?: string
  encaminhamento?: string
  prazoRetorno?: string
}

export interface ResumoEstatisticas {
  totalRealizados: number
  totalResolvidos: number
  totalNaoResolvidos: number
  totalEmTratativa: number
  percentualResolucao: number
  totalCancelamentos: number
  totalAusencias: number
  periodoDe: string | null
  periodoAte: string | null
}

export interface EvolucaoItem {
  ano: number
  mes: number
  realizados: number
  resolvidos: number
}

export interface DistribuicaoItem {
  chave: string
  total: number
}

export interface Notificacao {
  id: number
  tipo: string
  assunto: string
  mensagem: string
  statusEnvio: string
  dataCriacao: string | null
}

export interface AuditoriaItem {
  id: number
  usuarioId: number
  operacao: string
  entidade: string
  identificadorEntidade: number
  dataHora: string | null
  enderecoIp: string
  detalhes: string
}

// ---- Chat do atendimento (ADR-008) ----
export type EstadoSala = 'ABERTA' | 'FECHADA' | 'ENCERRADA'

export interface MensagemChat {
  id: number
  autorId: number
  autorNome: string
  perfilAutor: string
  conteudo: string
  dataEnvio: string
}
