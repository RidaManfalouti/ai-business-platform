import { apiClient } from './client';
import {
  AiConversationDetail,
  AiConversationSummary,
  SendMessageRequest,
  SendMessageResponse,
} from '@/types/ai';
import { SpringPage } from '@/types/api';

export const aiApi = {
  getConversations: async (page = 0, size = 20): Promise<SpringPage<AiConversationSummary>> => {
    const response = await apiClient.get<SpringPage<AiConversationSummary>>('/api/ai/conversations', {
      params: { page, size, sort: 'updatedAt,desc' },
    });
    return response.data;
  },

  getConversation: async (id: string): Promise<AiConversationDetail> => {
    const response = await apiClient.get<AiConversationDetail>(`/api/ai/conversations/${id}`);
    return response.data;
  },

  deleteConversation: async (id: string): Promise<void> => {
    await apiClient.delete(`/api/ai/conversations/${id}`);
  },

  sendMessage: async (request: SendMessageRequest): Promise<SendMessageResponse> => {
    const response = await apiClient.post<SendMessageResponse>('/api/ai/chat', request);
    return response.data;
  },
};
