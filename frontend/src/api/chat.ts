import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import api from './client'
import type { EstadoSala, MensagemChat } from './tipos'

/**
 * Chat do atendimento online (ADR-008).
 * Envio via REST (POST persiste + difunde); recepção por assinatura STOMP em
 * /topic/agendamento/{id} — o backend valida cada SUBSCRIBE no servidor (RNF17/18).
 */
export const chatApi = {
  async historico(agendamentoId: number): Promise<MensagemChat[]> {
    const { data } = await api.get<MensagemChat[]>(`/agendamentos/${agendamentoId}/chat`)
    return data
  },
  async estado(agendamentoId: number): Promise<{ estado: EstadoSala; modoTeste: boolean }> {
    const { data } = await api.get<{ estado: EstadoSala; modoTeste: boolean }>(
      `/agendamentos/${agendamentoId}/chat/estado`,
    )
    return data
  },
  async enviar(agendamentoId: number, conteudo: string): Promise<MensagemChat> {
    const { data } = await api.post<MensagemChat>(`/agendamentos/${agendamentoId}/chat`, { conteudo })
    return data
  },
}

/**
 * Conecta ao broker STOMP (SockJS em /ws, sessão HTTP existente) e assina a sala.
 * Retorna função de limpeza. `onMensagem` recebe mensagens novas em tempo real.
 */
export function assinarSala(
  agendamentoId: number,
  onMensagem: (m: MensagemChat) => void,
  onEstado?: (conectado: boolean) => void,
): () => void {
  const client = new Client({
    webSocketFactory: () => new SockJS('/ws') as WebSocket,
    reconnectDelay: 4000,
    onConnect: () => {
      client.subscribe(`/topic/agendamento/${agendamentoId}`, (frame) => {
        try {
          onMensagem(JSON.parse(frame.body) as MensagemChat)
        } catch {
          /* payload inesperado: ignora */
        }
      })
      onEstado?.(true)
    },
    onWebSocketClose: () => onEstado?.(false),
  })
  client.activate()
  return () => client.deactivate()
}
