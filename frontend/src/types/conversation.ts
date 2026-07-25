export interface ConversationResponse {
  id: string;
  title: string;
  documentNames: string[];
  createdAt: string;
}

export interface ConversationDetailResponse {
  id: string;
  title: string;
  documentNames: string[];
  messages: MessageResponse[];
  createdAt: string;
}

export interface MessageResponse {
  id: string;
  role: 'USER' | 'ASSISTANT';
  content: string;
  citations: string;
  createdAt: string;
}

export interface CreateConversationRequest {
  documentIds: string[];
}

export interface SendMessageRequest {
  message: string;
}
