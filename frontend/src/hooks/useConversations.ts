import { useState, useEffect, useCallback } from 'react';
import type { ConversationResponse } from '../types/conversation';
import {
  listConversations,
  createConversation,
  deleteConversation,
} from '../services/conversationService';
import { useToast } from '../context/ToastContext';

export function useConversations() {
  const [conversations, setConversations] = useState<ConversationResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const { addToast } = useToast();

  const fetch = useCallback(async () => {
    try {
      const convs = await listConversations();
      setConversations(convs);
    } catch {
      addToast('Failed to load conversations', 'error');
    } finally {
      setLoading(false);
    }
  }, [addToast]);

  useEffect(() => {
    fetch();
  }, [fetch]);

  const create = useCallback(async (documentIds: string[]) => {
    try {
      const conv = await createConversation({ documentIds });
      setConversations((prev) => [conv, ...prev]);
      addToast('Conversation created', 'success');
      return conv;
    } catch {
      addToast('Failed to create conversation', 'error');
      return null;
    }
  }, [addToast]);

  const remove = useCallback(async (id: string) => {
    try {
      await deleteConversation(id);
      setConversations((prev) => prev.filter((c) => c.id !== id));
      addToast('Conversation deleted', 'success');
    } catch {
      addToast('Failed to delete conversation', 'error');
    }
  }, [addToast]);

  return { conversations, loading, create, remove, refresh: fetch };
}
