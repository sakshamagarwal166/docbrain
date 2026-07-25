import { useState, useEffect, useCallback } from 'react';
import type { ConversationDetailResponse, MessageResponse } from '../types/conversation';
import { getConversation, sendMessage } from '../services/conversationService';
import { useToast } from '../context/ToastContext';

export function useChat(conversationId: string) {
  const [conversation, setConversation] = useState<ConversationDetailResponse | null>(null);
  const [messages, setMessages] = useState<MessageResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const [sending, setSending] = useState(false);
  const { addToast } = useToast();

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    getConversation(conversationId)
      .then((data) => {
        if (!cancelled) {
          setConversation(data);
          setMessages(data.messages);
        }
      })
      .catch(() => addToast('Failed to load conversation', 'error'))
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [conversationId, addToast]);

  const send = useCallback(async (text: string) => {
    setSending(true);
    const userMsg: MessageResponse = {
      id: `temp-${Date.now()}`,
      role: 'USER',
      content: text,
      citations: '[]',
      createdAt: new Date().toISOString(),
    };
    setMessages((prev) => [...prev, userMsg]);

    try {
      const assistantMsg = await sendMessage(conversationId, { message: text });
      setMessages((prev) => [...prev, assistantMsg]);
      return assistantMsg;
    } catch (err: unknown) {
      const status = (err as { response?: { status?: number } })?.response?.status;
      if (status === 429) {
        addToast('Rate limit exceeded. Please wait a moment.', 'error');
      } else {
        addToast('Failed to send message', 'error');
      }
      setMessages((prev) => prev.filter((m) => m.id !== userMsg.id));
      return null;
    } finally {
      setSending(false);
    }
  }, [conversationId, addToast]);

  return { conversation, messages, loading, sending, send };
}
