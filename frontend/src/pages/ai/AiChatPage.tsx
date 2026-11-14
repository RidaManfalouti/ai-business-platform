import React, { useState, useEffect, useRef } from 'react';
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { aiApi } from '@/api/aiApi';
import { useAuth } from '@/context/AuthContext';
import { AiConversationSummary, AiMessage } from '@/types/ai';
import { MarkdownRenderer } from '@/components/ai/MarkdownRenderer';
import {
  Bot,
  Send,
  Plus,
  Trash2,
  Sparkles,
  User,
  AlertCircle,
  Loader2,
  MessageSquare,
  TrendingUp,
  AlertTriangle,
  Award,
  Users,
} from 'lucide-react';

export const AiChatPage: React.FC = () => {
  const { activeTenantId, activeTenantSlug } = useAuth();
  const queryClient = useQueryClient();

  const [activeConversationId, setActiveConversationId] = useState<string | null>(null);
  const [messages, setMessages] = useState<AiMessage[]>([]);
  const [inputValue, setInputValue] = useState('');
  const [errorBanner, setErrorBanner] = useState<string | null>(null);

  const messagesEndRef = useRef<HTMLDivElement | null>(null);
  const inputRef = useRef<HTMLInputElement | null>(null);

  // 1. Fetch conversations list
  const {
    data: conversationsPage,
    isLoading: isLoadingConversations,
  } = useQuery({
    queryKey: ['ai-conversations', activeTenantId],
    queryFn: () => aiApi.getConversations(0, 50),
    enabled: !!activeTenantId,
  });

  const conversations: AiConversationSummary[] = conversationsPage?.content || [];

  // 2. Fetch active conversation messages
  const {
    data: activeConversationData,
    isLoading: isLoadingMessages,
  } = useQuery({
    queryKey: ['ai-conversation', activeConversationId],
    queryFn: () => (activeConversationId ? aiApi.getConversation(activeConversationId) : null),
    enabled: !!activeConversationId,
  });

  useEffect(() => {
    if (activeConversationData?.messages) {
      setMessages(activeConversationData.messages);
    } else if (!activeConversationId) {
      setMessages([]);
    }
  }, [activeConversationData, activeConversationId]);

  // Scroll to bottom on messages update
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // 3. Send message mutation
  const sendMessageMutation = useMutation({
    mutationFn: (text: string) =>
      aiApi.sendMessage({
        conversationId: activeConversationId,
        message: text,
      }),
    onMutate: (text: string) => {
      setErrorBanner(null);
      // Optimistic user message append
      const tempUserMessage: AiMessage = {
        id: 'temp-' + Date.now(),
        role: 'user',
        content: text,
        createdAt: new Date().toISOString(),
      };
      setMessages((prev) => [...prev, tempUserMessage]);
    },
    onSuccess: (data) => {
      // If new conversation was created
      if (!activeConversationId) {
        setActiveConversationId(data.conversationId);
      }
      queryClient.invalidateQueries({ queryKey: ['ai-conversations', activeTenantId] });

      const assistantMessage: AiMessage = {
        id: data.messageId,
        role: 'assistant',
        content: data.response,
        createdAt: data.createdAt,
      };

      setMessages((prev) => [...prev, assistantMessage]);
    },
    onError: (err: any) => {
      const msg = err?.message || "Une erreur est survenue lors de l'échange avec l'assistant.";
      setErrorBanner(msg);
      // Remove failed optimistic message
      setMessages((prev) => prev.filter((m) => !m.id.startsWith('temp-')));
    },
  });

  // 4. Delete conversation mutation
  const deleteMutation = useMutation({
    mutationFn: (id: string) => aiApi.deleteConversation(id),
    onSuccess: (_, deletedId) => {
      if (activeConversationId === deletedId) {
        setActiveConversationId(null);
        setMessages([]);
      }
      queryClient.invalidateQueries({ queryKey: ['ai-conversations', activeTenantId] });
    },
    onError: (err: any) => {
      setErrorBanner(err?.message || 'Impossible de supprimer la conversation.');
    },
  });

  const handleSend = (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    const trimmed = inputValue.trim();
    if (!trimmed || sendMessageMutation.isPending) return;

    setInputValue('');
    sendMessageMutation.mutate(trimmed);
  };

  const handleQuickPrompt = (prompt: string) => {
    setInputValue('');
    sendMessageMutation.mutate(prompt);
  };

  const handleNewConversation = () => {
    setActiveConversationId(null);
    setMessages([]);
    setErrorBanner(null);
    inputRef.current?.focus();
  };

  return (
    <div className="flex h-[calc(100vh-5rem)] flex-col lg:flex-row gap-4 p-2 lg:p-4">
      {/* Sidebar: Conversation History */}
      <div className="w-full lg:w-72 flex flex-col bg-slate-900 border border-slate-800 rounded-xl overflow-hidden shrink-0">
        <div className="p-3 border-b border-slate-800 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Bot className="w-5 h-5 text-emerald-400" />
            <h2 className="text-sm font-bold text-white">AI Copilot</h2>
          </div>
          <button
            type="button"
            onClick={handleNewConversation}
            className="inline-flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-medium transition-colors shadow-sm"
          >
            <Plus className="w-3.5 h-3.5" />
            Nouveau
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-2 space-y-1">
          {isLoadingConversations ? (
            <div className="flex justify-center items-center py-8 text-slate-500 text-xs">
              <Loader2 className="w-4 h-4 animate-spin mr-2" />
              Chargement...
            </div>
          ) : conversations.length === 0 ? (
            <div className="text-center py-8 px-4 text-xs text-slate-400">
              <MessageSquare className="w-8 h-8 mx-auto mb-2 opacity-30 text-slate-400" />
              Aucune conversation pour le moment. Posez une question pour commencer !
            </div>
          ) : (
            conversations.map((conv) => {
              const isActive = conv.id === activeConversationId;
              return (
                <div
                  key={conv.id}
                  onClick={() => setActiveConversationId(conv.id)}
                  className={`group flex items-center justify-between px-3 py-2 rounded-lg cursor-pointer text-xs transition-all ${
                    isActive
                      ? 'bg-emerald-950/70 border border-emerald-500/40 text-emerald-200'
                      : 'text-slate-300 hover:bg-slate-800/80 hover:text-white'
                  }`}
                >
                  <span className="truncate pr-2">{conv.title}</span>
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      if (window.confirm('Voulez-vous supprimer cette conversation ?')) {
                        deleteMutation.mutate(conv.id);
                      }
                    }}
                    className="opacity-0 group-hover:opacity-100 p-1 text-slate-500 hover:text-rose-400 rounded transition-opacity"
                    title="Supprimer"
                  >
                    <Trash2 className="w-3.5 h-3.5" />
                  </button>
                </div>
              );
            })
          )}
        </div>
      </div>

      {/* Main Chat Area */}
      <div className="flex-1 flex flex-col bg-slate-900 border border-slate-800 rounded-xl overflow-hidden">
        {/* Chat Header */}
        <div className="px-4 py-3 border-b border-slate-800 flex items-center justify-between bg-slate-950/40">
          <div className="flex items-center gap-2">
            <div className="flex h-7 w-7 items-center justify-center rounded-md bg-emerald-500/20 text-emerald-400">
              <Sparkles className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-xs font-bold text-white">Assistant Décisionnel d'Entreprise</h3>
              <p className="text-[11px] text-slate-400">
                {activeTenantSlug ? `Organisation: ${activeTenantSlug}` : 'Multi-tenant copilot'}
              </p>
            </div>
          </div>
          <span className="px-2 py-0.5 rounded text-[10px] font-semibold bg-emerald-950 text-emerald-400 border border-emerald-800/60">
            En ligne
          </span>
        </div>

        {/* Error Notification */}
        {errorBanner && (
          <div className="m-3 p-3 rounded-lg bg-rose-950/60 border border-rose-800 text-rose-200 text-xs flex items-center justify-between">
            <div className="flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0 text-rose-400" />
              <span>{errorBanner}</span>
            </div>
            <button
              type="button"
              onClick={() => setErrorBanner(null)}
              className="text-rose-400 hover:text-white text-xs font-bold px-2"
            >
              ×
            </button>
          </div>
        )}

        {/* Messages Stream */}
        <div className="flex-1 overflow-y-auto p-4 space-y-4">
          {isLoadingMessages ? (
            <div className="flex justify-center items-center py-12 text-slate-400 text-xs">
              <Loader2 className="w-5 h-5 animate-spin mr-2" />
              Chargement des échanges...
            </div>
          ) : messages.length === 0 ? (
            <div className="h-full flex flex-col items-center justify-center text-center p-6 space-y-4">
              <div className="w-12 h-12 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400 shadow-inner">
                <Sparkles className="w-6 h-6" />
              </div>
              <div className="max-w-md">
                <h4 className="text-sm font-bold text-white mb-1">
                  Que souhaitez-vous savoir sur votre entreprise ?
                </h4>
                <p className="text-xs text-slate-400">
                  Posez des questions en langage naturel pour explorer vos ventes, votre stock et vos
                  performances.
                </p>
              </div>

              {/* Quick Action Prompt Chips */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 w-full max-w-lg mt-2 text-left">
                <button
                  type="button"
                  onClick={() => handleQuickPrompt("Quels sont mes ventes et mon chiffre d'affaires ce mois-ci ?")}
                  className="p-2.5 rounded-lg bg-slate-800/70 hover:bg-slate-800 border border-slate-700/80 text-left transition-colors flex items-center gap-2.5 text-xs text-slate-200"
                >
                  <TrendingUp className="w-4 h-4 text-emerald-400 shrink-0" />
                  <span>Total des ventes du mois</span>
                </button>
                <button
                  type="button"
                  onClick={() => handleQuickPrompt('Quels sont les produits avec un stock faible ?')}
                  className="p-2.5 rounded-lg bg-slate-800/70 hover:bg-slate-800 border border-slate-700/80 text-left transition-colors flex items-center gap-2.5 text-xs text-slate-200"
                >
                  <AlertTriangle className="w-4 h-4 text-amber-400 shrink-0" />
                  <span>Alertes de stock faible</span>
                </button>
                <button
                  type="button"
                  onClick={() => handleQuickPrompt('Quel est mon top 5 des meilleurs produits ?')}
                  className="p-2.5 rounded-lg bg-slate-800/70 hover:bg-slate-800 border border-slate-700/80 text-left transition-colors flex items-center gap-2.5 text-xs text-slate-200"
                >
                  <Award className="w-4 h-4 text-cyan-400 shrink-0" />
                  <span>Top 5 des meilleurs produits</span>
                </button>
                <button
                  type="button"
                  onClick={() => handleQuickPrompt('Qui sont mes clients enregistrés ?')}
                  className="p-2.5 rounded-lg bg-slate-800/70 hover:bg-slate-800 border border-slate-700/80 text-left transition-colors flex items-center gap-2.5 text-xs text-slate-200"
                >
                  <Users className="w-4 h-4 text-indigo-400 shrink-0" />
                  <span>Liste de mes clients</span>
                </button>
              </div>
            </div>
          ) : (
            messages.map((msg) => {
              const isUser = msg.role === 'user';
              return (
                <div
                  key={msg.id}
                  className={`flex items-start gap-3 ${isUser ? 'flex-row-reverse' : 'flex-row'}`}
                >
                  <div
                    className={`w-7 h-7 rounded-full flex items-center justify-center shrink-0 ${
                      isUser ? 'bg-emerald-600 text-white' : 'bg-slate-800 text-emerald-400 border border-slate-700'
                    }`}
                  >
                    {isUser ? <User className="w-4 h-4" /> : <Bot className="w-4 h-4" />}
                  </div>

                  <div
                    className={`max-w-[85%] lg:max-w-[75%] rounded-2xl px-4 py-3 text-xs leading-relaxed ${
                      isUser
                        ? 'bg-emerald-600 text-white rounded-tr-none shadow-md'
                        : 'bg-slate-800/90 text-slate-100 border border-slate-700/60 rounded-tl-none shadow-md'
                    }`}
                  >
                    {isUser ? (
                      <p className="whitespace-pre-wrap">{msg.content}</p>
                    ) : (
                      <MarkdownRenderer content={msg.content} />
                    )}
                  </div>
                </div>
              );
            })
          )}

          {sendMessageMutation.isPending && (
            <div className="flex items-start gap-3">
              <div className="w-7 h-7 rounded-full bg-slate-800 text-emerald-400 border border-slate-700 flex items-center justify-center shrink-0">
                <Bot className="w-4 h-4" />
              </div>
              <div className="rounded-2xl bg-slate-800/90 text-slate-300 border border-slate-700/60 px-4 py-3 rounded-tl-none text-xs flex items-center gap-2">
                <Loader2 className="w-3.5 h-3.5 animate-spin text-emerald-400" />
                <span>Analyse de vos données d'entreprise en cours...</span>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Input Bar */}
        <form onSubmit={handleSend} className="p-3 border-t border-slate-800 bg-slate-950/60 flex gap-2">
          <input
            ref={inputRef}
            type="text"
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            placeholder="Posez une question sur vos ventes, stocks, KPIs..."
            disabled={sendMessageMutation.isPending}
            className="flex-1 bg-slate-900 border border-slate-700 rounded-lg px-3.5 py-2 text-xs text-white placeholder-slate-500 focus:outline-none focus:ring-1 focus:ring-emerald-500"
          />
          <button
            type="submit"
            disabled={!inputValue.trim() || sendMessageMutation.isPending}
            className="inline-flex items-center justify-center px-4 py-2 rounded-lg bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 disabled:cursor-not-allowed text-white text-xs font-semibold transition-colors shadow-sm"
          >
            {sendMessageMutation.isPending ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <Send className="w-4 h-4" />
            )}
          </button>
        </form>
      </div>
    </div>
  );
};

export default AiChatPage;
