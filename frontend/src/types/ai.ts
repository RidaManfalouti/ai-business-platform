export interface AiMessage {
  id: string;
  role: 'user' | 'assistant' | 'system' | 'tool';
  content: string;
  createdAt: string;
}

export interface AiConversationSummary {
  id: string;
  title: string;
  createdAt: string;
  updatedAt: string;
}

export interface AiConversationDetail {
  id: string;
  title: string;
  createdAt: string;
  updatedAt: string;
  messages: AiMessage[];
}

export interface SendMessageRequest {
  conversationId?: string | null;
  message: string;
}

export interface SendMessageResponse {
  conversationId: string;
  messageId: string;
  role: string;
  response: string;
  createdAt: string;
}
