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
    <div className="flex h-[calc(100vh-5.5rem)] flex-col lg:flex-row gap-4 p-1 sm:p-2">
      {/* Sidebar: Conversation History */}
      <div className="w-full lg:w-72 flex flex-col bg-[#18181B] border border-[#27272A] rounded-2xl overflow-hidden shrink-0 shadow-xl">
        <div className="p-3.5 border-b border-[#27272A] flex items-center justify-between bg-[#202024]/30">
          <div className="flex items-center gap-2">
            <div className="w-6 h-6 rounded-lg bg-[#A855F7]/20 flex items-center justify-center text-[#A855F7] border border-[#A855F7]/30">
              <Bot className="w-3.5 h-3.5" />
            </div>
            <h2 className="text-xs font-bold text-[#FAFAFA] tracking-wide">AI Copilot</h2>
          </div>
          <button
            type="button"
            onClick={handleNewConversation}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-[#FACC15] hover:bg-[#EAB308] text-[#09090B] text-xs font-bold transition-all shadow-[0_0_12px_rgba(250,204,21,0.25)] active:scale-95"
          >
            <Plus className="w-3.5 h-3.5" />
            Nouveau
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-2 space-y-1">
          {isLoadingConversations ? (
            <div className="flex justify-center items-center py-8 text-[#A1A1AA] text-xs">
              <Loader2 className="w-4 h-4 animate-spin mr-2 text-[#A855F7]" />
              Chargement...
            </div>
          ) : conversations.length === 0 ? (
            <div className="text-center py-8 px-4 text-xs text-[#71717A]">
              <MessageSquare className="w-8 h-8 mx-auto mb-2 opacity-30 text-[#71717A]" />
              Aucune conversation pour le moment. Posez une question pour commencer !
            </div>
          ) : (
            conversations.map((conv) => {
              const isActive = conv.id === activeConversationId;
              return (
                <div
                  key={conv.id}
                  onClick={() => setActiveConversationId(conv.id)}
                  className={`group flex items-center justify-between px-3 py-2 rounded-xl cursor-pointer text-xs transition-all ${
                    isActive
                      ? 'bg-[#A855F7]/15 border border-[#A855F7]/40 text-[#D8B4FE] font-medium shadow-[0_0_12px_rgba(168,85,247,0.12)]'
                      : 'text-[#A1A1AA] hover:bg-[#202024] hover:text-[#FAFAFA]'
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
                    className="opacity-0 group-hover:opacity-100 p-1 text-[#71717A] hover:text-[#EF4444] rounded transition-opacity"
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
      <div className="flex-1 flex flex-col bg-[#18181B] border border-[#27272A] rounded-2xl overflow-hidden shadow-2xl">
        {/* Chat Header */}
        <div className="px-5 py-3 border-b border-[#27272A] flex items-center justify-between bg-[#202024]/40">
          <div className="flex items-center gap-2.5">
            <div className="flex h-8 w-8 items-center justify-center rounded-xl bg-[#A855F7]/15 text-[#A855F7] border border-[#A855F7]/30 shadow-[0_0_12px_rgba(168,85,247,0.2)]">
              <Sparkles className="w-4 h-4" />
            </div>
            <div>
              <h3 className="text-xs font-bold text-[#FAFAFA]">Assistant Décisionnel d'Entreprise</h3>
              <p className="text-[11px] text-[#A1A1AA]">
                {activeTenantSlug ? `Organisation: ${activeTenantSlug}` : 'Multi-tenant copilot'} &bull; Gemini LLM
              </p>
            </div>
          </div>
          <span className="px-2.5 py-1 rounded-full text-[10px] font-semibold bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 flex items-center gap-1.5">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse" />
            En ligne
          </span>
        </div>

        {/* Error Notification */}
        {errorBanner && (
          <div className="m-3 p-3 rounded-xl bg-[#EF4444]/10 border border-[#EF4444]/25 text-[#EF4444] text-xs flex items-center justify-between">
            <div className="flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0 text-[#EF4444]" />
              <span>{errorBanner}</span>
            </div>
            <button
              type="button"
              onClick={() => setErrorBanner(null)}
              className="text-[#EF4444] hover:text-white text-xs font-bold px-2"
            >
              ×
            </button>
          </div>
        )}

        {/* Messages Stream */}
        <div className="flex-1 overflow-y-auto p-4 sm:p-6 space-y-4 bg-[#09090B]">
          {isLoadingMessages ? (
            <div className="flex justify-center items-center py-12 text-[#A1A1AA] text-xs">
              <Loader2 className="w-5 h-5 animate-spin mr-2 text-[#A855F7]" />
              Chargement des échanges...
            </div>
          ) : messages.length === 0 ? (
            <div className="h-full flex flex-col items-center justify-center text-center p-6 space-y-5">
              <div className="w-14 h-14 rounded-2xl bg-[#A855F7]/15 border border-[#A855F7]/30 flex items-center justify-center text-[#A855F7] shadow-[0_0_30px_rgba(168,85,247,0.25)]">
                <Sparkles className="w-7 h-7" />
              </div>
              <div className="max-w-md">
                <h4 className="text-base font-bold text-[#FAFAFA] mb-1">
                  Que souhaitez-vous analyser sur votre entreprise ?
                </h4>
                <p className="text-xs text-[#A1A1AA]">
                  Interrogez en langage naturel vos indicateurs de ventes, de stock et la performance de votre catalogue.
                </p>
              </div>

              {/* Quick Action Prompt Chips */}
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 w-full max-w-xl mt-2 text-left">
                <button
                  type="button"
                  onClick={() => handleQuickPrompt("Quels sont mes ventes et mon chiffre d'affaires ce mois-ci ?")}
                  className="p-3 rounded-xl bg-[#18181B] hover:bg-[#202024] border border-[#27272A] hover:border-[#FACC15]/40 text-left transition-all hover:scale-[1.01] hover:shadow-[0_0_15px_rgba(250,204,21,0.12)] flex items-center gap-3 text-xs text-[#FAFAFA]"
                >
                  <div className="p-2 rounded-lg bg-[#FACC15]/15 text-[#FACC15] shrink-0">
                    <TrendingUp className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="font-semibold text-[#FAFAFA]">Total des ventes du mois</div>
                    <div className="text-[10px] text-[#71717A]">Chiffre d'affaires & volume</div>
                  </div>
                </button>
                <button
                  type="button"
                  onClick={() => handleQuickPrompt('Quels sont les produits avec un stock faible ?')}
                  className="p-3 rounded-xl bg-[#18181B] hover:bg-[#202024] border border-[#27272A] hover:border-[#EF4444]/40 text-left transition-all hover:scale-[1.01] hover:shadow-[0_0_15px_rgba(239,68,68,0.12)] flex items-center gap-3 text-xs text-[#FAFAFA]"
                >
                  <div className="p-2 rounded-lg bg-[#EF4444]/15 text-[#EF4444] shrink-0">
                    <AlertTriangle className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="font-semibold text-[#FAFAFA]">Alertes de stock faible</div>
                    <div className="text-[10px] text-[#71717A]">Produits sous le seuil d'alerte</div>
                  </div>
                </button>
                <button
                  type="button"
                  onClick={() => handleQuickPrompt('Quel est mon top 5 des meilleurs produits ?')}
                  className="p-3 rounded-xl bg-[#18181B] hover:bg-[#202024] border border-[#27272A] hover:border-[#A855F7]/40 text-left transition-all hover:scale-[1.01] hover:shadow-[0_0_15px_rgba(168,85,247,0.12)] flex items-center gap-3 text-xs text-[#FAFAFA]"
                >
                  <div className="p-2 rounded-lg bg-[#A855F7]/15 text-[#A855F7] shrink-0">
                    <Award className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="font-semibold text-[#FAFAFA]">Top 5 des meilleurs produits</div>
                    <div className="text-[10px] text-[#71717A]">Classement par CA réalisé</div>
                  </div>
                </button>
                <button
                  type="button"
                  onClick={() => handleQuickPrompt('Qui sont mes clients enregistrés ?')}
                  className="p-3 rounded-xl bg-[#18181B] hover:bg-[#202024] border border-[#27272A] hover:border-[#38BDF8]/40 text-left transition-all hover:scale-[1.01] hover:shadow-[0_0_15px_rgba(56,189,248,0.12)] flex items-center gap-3 text-xs text-[#FAFAFA]"
                >
                  <div className="p-2 rounded-lg bg-sky-500/15 text-sky-400 shrink-0">
                    <Users className="w-4 h-4" />
                  </div>
                  <div>
                    <div className="font-semibold text-[#FAFAFA]">Liste de mes clients</div>
                    <div className="text-[10px] text-[#71717A]">Portefeuille & comptes</div>
                  </div>
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
                    className={`w-8 h-8 rounded-xl flex items-center justify-center shrink-0 ${
                      isUser
                        ? 'bg-[#FACC15] text-[#09090B] font-bold shadow-[0_0_12px_rgba(250,204,21,0.25)]'
                        : 'bg-[#A855F7]/20 text-[#A855F7] border border-[#A855F7]/30 shadow-[0_0_15px_rgba(168,85,247,0.25)]'
                    }`}
                  >
                    {isUser ? <User className="w-4 h-4" /> : <Bot className="w-4 h-4" />}
                  </div>

                  <div
                    className={`max-w-[85%] lg:max-w-[75%] rounded-2xl px-4 py-3 text-xs leading-relaxed ${
                      isUser
                        ? 'bg-[#202024] text-[#FAFAFA] border border-[#3F3F46] rounded-tr-none shadow-md'
                        : 'bg-[#18181B] text-[#FAFAFA] border border-[#27272A] rounded-tl-none shadow-md'
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
              <div className="w-8 h-8 rounded-xl bg-[#A855F7]/20 text-[#A855F7] border border-[#A855F7]/30 flex items-center justify-center shrink-0 shadow-[0_0_15px_rgba(168,85,247,0.25)]">
                <Bot className="w-4 h-4" />
              </div>
              <div className="rounded-2xl bg-[#18181B] text-[#A1A1AA] border border-[#A855F7]/40 px-4 py-3 rounded-tl-none text-xs flex items-center gap-2.5 shadow-[0_0_15px_rgba(168,85,247,0.15)]">
                <Loader2 className="w-4 h-4 animate-spin text-[#A855F7]" />
                <span className="text-[#FAFAFA]">Analyse de vos données d'entreprise et exécution des outils décisionnels...</span>
              </div>
            </div>
          )}

          <div ref={messagesEndRef} />
        </div>

        {/* Input Bar */}
        <form onSubmit={handleSend} className="p-3.5 border-t border-[#27272A] bg-[#18181B] flex gap-2.5">
          <input
            ref={inputRef}
            type="text"
            value={inputValue}
            onChange={(e) => setInputValue(e.target.value)}
            placeholder="Posez une question sur vos ventes, stocks, KPIs..."
            disabled={sendMessageMutation.isPending}
            className="flex-1 bg-[#09090B] border border-[#27272A] rounded-xl px-4 py-2.5 text-xs text-[#FAFAFA] placeholder:text-[#71717A] focus:outline-none focus:border-[#FACC15] focus:ring-1 focus:ring-[#FACC15]/20 transition-colors"
          />
          <button
            type="submit"
            disabled={!inputValue.trim() || sendMessageMutation.isPending}
            className="inline-flex items-center justify-center px-4 py-2.5 rounded-xl bg-[#FACC15] hover:bg-[#EAB308] disabled:opacity-40 disabled:cursor-not-allowed text-[#09090B] text-xs font-bold transition-all shadow-[0_0_15px_rgba(250,204,21,0.25)] active:scale-95"
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
