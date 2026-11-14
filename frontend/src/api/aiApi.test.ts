import { describe, it, expect, vi, beforeEach } from 'vitest';
import { aiApi } from './aiApi';
import { apiClient } from './client';

vi.mock('./client', () => ({
  apiClient: {
    get: vi.fn(),
    post: vi.fn(),
    delete: vi.fn(),
  },
}));

describe('aiApi', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('getConversations appelle GET /api/ai/conversations avec pagination', async () => {
    const mockPage = {
      content: [{ id: 'conv-1', title: 'Test Conv', createdAt: '', updatedAt: '' }],
      totalElements: 1,
      totalPages: 1,
      size: 20,
      number: 0,
      numberOfElements: 1,
      first: true,
      last: true,
      empty: false,
    };
    (apiClient.get as any).mockResolvedValueOnce({ data: mockPage });

    const result = await aiApi.getConversations(0, 20);

    expect(apiClient.get).toHaveBeenCalledWith('/api/ai/conversations', {
      params: { page: 0, size: 20, sort: 'updatedAt,desc' },
    });
    expect(result.content).toHaveLength(1);
    expect(result.content[0].title).toBe('Test Conv');
  });

  it('getConversation appelle GET /api/ai/conversations/:id', async () => {
    const mockDetail = {
      id: 'conv-1',
      title: 'Détail',
      createdAt: '',
      updatedAt: '',
      messages: [{ id: 'msg-1', role: 'user', content: 'Hello', createdAt: '' }],
    };
    (apiClient.get as any).mockResolvedValueOnce({ data: mockDetail });

    const result = await aiApi.getConversation('conv-1');

    expect(apiClient.get).toHaveBeenCalledWith('/api/ai/conversations/conv-1');
    expect(result.messages).toHaveLength(1);
  });

  it('deleteConversation appelle DELETE /api/ai/conversations/:id', async () => {
    (apiClient.delete as any).mockResolvedValueOnce({});

    await aiApi.deleteConversation('conv-1');

    expect(apiClient.delete).toHaveBeenCalledWith('/api/ai/conversations/conv-1');
  });

  it('sendMessage appelle POST /api/ai/chat', async () => {
    const mockResponse = {
      conversationId: 'conv-1',
      messageId: 'msg-2',
      role: 'assistant',
      response: 'Bonjour !',
      createdAt: '',
    };
    (apiClient.post as any).mockResolvedValueOnce({ data: mockResponse });

    const result = await aiApi.sendMessage({ conversationId: 'conv-1', message: 'Hello' });

    expect(apiClient.post).toHaveBeenCalledWith('/api/ai/chat', {
      conversationId: 'conv-1',
      message: 'Hello',
    });
    expect(result.response).toBe('Bonjour !');
  });
});
