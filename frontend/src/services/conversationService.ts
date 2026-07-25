import api from './api';
import type {
  ConversationResponse,
  ConversationDetailResponse,
  CreateConversationRequest,
  MessageResponse,
  SendMessageRequest,
} from '../types/conversation';

export async function listConversations(): Promise<ConversationResponse[]> {
  const res = await api.get<ConversationResponse[]>('/conversations');
  return res.data;
}

export async function getConversation(id: string): Promise<ConversationDetailResponse> {
  const res = await api.get<ConversationDetailResponse>(`/conversations/${id}`);
  return res.data;
}

export async function createConversation(data: CreateConversationRequest): Promise<ConversationResponse> {
  const res = await api.post<ConversationResponse>('/conversations', data);
  return res.data;
}

export async function sendMessage(conversationId: string, data: SendMessageRequest): Promise<MessageResponse> {
  const res = await api.post<MessageResponse>(`/conversations/${conversationId}/messages`, data);
  return res.data;
}

export async function deleteConversation(id: string): Promise<void> {
  await api.delete(`/conversations/${id}`);
}
