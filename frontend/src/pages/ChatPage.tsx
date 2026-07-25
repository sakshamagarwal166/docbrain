import { useState, useRef, useEffect, type FormEvent, type KeyboardEvent } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useChat } from '../hooks/useChat';
import { SkeletonChat } from '../components/Skeleton';
import type { CitedSource } from '../types/chunk';
import type { MessageResponse } from '../types/conversation';

function parseCitations(raw: string): CitedSource[] {
  try {
    const parsed = JSON.parse(raw);
    return Array.isArray(parsed) ? parsed : [];
  } catch {
    return [];
  }
}

function ChatMessage({ msg, onCitationClick }: {
  msg: MessageResponse;
  onCitationClick?: (idx: number) => void;
}) {
  const isUser = msg.role === 'USER';
  const citations = parseCitations(msg.citations);

  return (
    <div className={`flex ${isUser ? 'justify-end' : 'justify-start'} mb-4`}>
      <div className={`max-w-[80%] ${isUser ? 'order-2' : 'order-1'}`}>
        {!isUser && (
          <div className="flex items-center gap-2 mb-1">
            <div className="w-6 h-6 bg-primary-600 rounded-md flex items-center justify-center">
              <span className="text-white text-xs font-bold">D</span>
            </div>
            <span className="text-xs text-gray-500 dark:text-gray-400">DocBrain</span>
          </div>
        )}
        <div
          className={`px-4 py-3 rounded-2xl text-sm leading-relaxed ${
            isUser
              ? 'bg-primary-600 text-white rounded-br-md'
              : 'bg-gray-100 dark:bg-gray-800 text-gray-900 dark:text-gray-100 rounded-bl-md'
          }`}
        >
          <p className="whitespace-pre-wrap">{msg.content}</p>
        </div>

        {!isUser && citations.length > 0 && (
          <div className="mt-2 space-y-1.5">
            <p className="text-xs font-medium text-gray-500 dark:text-gray-400 px-1">Sources</p>
            {citations.map((c, i) => (
              <button
                key={i}
                id={`citation-${i}`}
                onClick={() => onCitationClick?.(i)}
                className="flex items-start gap-2 w-full text-left p-2 rounded-lg
                           bg-gray-50 dark:bg-gray-800/50 hover:bg-gray-100 dark:hover:bg-gray-700/50
                           border border-gray-200 dark:border-gray-700 transition-colors"
              >
                <span className="flex-shrink-0 w-5 h-5 bg-primary-100 dark:bg-primary-900/30
                                 text-primary-700 dark:text-primary-300 rounded text-xs
                                 font-mono font-bold flex items-center justify-center mt-0.5">
                  {i + 1}
                </span>
                <div className="min-w-0">
                  <p className="text-xs font-medium text-gray-700 dark:text-gray-300 truncate">
                    {c.documentName}
                    {c.pageNumber && ` · p.${c.pageNumber}`}
                    {c.chunkIndex !== null && ` · chunk ${c.chunkIndex}`}
                  </p>
                  <p className="text-xs text-gray-500 dark:text-gray-400 line-clamp-2 font-mono">
                    {c.relevantText}
                  </p>
                </div>
              </button>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

export default function ChatPage() {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { conversation, messages, loading, sending, send } = useChat(id!);
  const [input, setInput] = useState('');
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const textareaRef = useRef<HTMLTextAreaElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    if (!input.trim() || sending) return;
    const text = input.trim();
    setInput('');
    if (textareaRef.current) textareaRef.current.style.height = 'auto';
    await send(text);
  };

  const handleKeyDown = (e: KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSubmit(e);
    }
  };

  const handleTextareaInput = () => {
    const el = textareaRef.current;
    if (el) {
      el.style.height = 'auto';
      el.style.height = Math.min(el.scrollHeight, 150) + 'px';
    }
  };

  const scrollToCitation = (idx: number) => {
    document.getElementById(`citation-${idx}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' });
  };

  if (loading) {
    return (
      <div className="flex flex-col h-full">
        <div className="border-b border-gray-200 dark:border-gray-800 px-6 py-4">
          <div className="h-5 bg-gray-200 dark:bg-gray-700 rounded w-48 animate-pulse" />
        </div>
        <div className="flex-1 overflow-auto p-6">
          <SkeletonChat />
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full">
      <div className="border-b border-gray-200 dark:border-gray-800 px-4 sm:px-6 py-3 flex items-center gap-3
                      bg-white dark:bg-gray-900">
        <button
          onClick={() => navigate('/conversations')}
          className="text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
        >
          <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15 19l-7-7 7-7" />
          </svg>
        </button>
        <div className="flex-1 min-w-0">
          <h2 className="text-sm font-semibold truncate">{conversation?.title}</h2>
          <div className="flex gap-1.5 flex-wrap mt-0.5">
            {conversation?.documentNames.map((name, i) => (
              <span key={i} className="text-xs px-1.5 py-0.5 rounded bg-gray-100 dark:bg-gray-800
                                       text-gray-500 dark:text-gray-400">
                {name}
              </span>
            ))}
          </div>
        </div>
      </div>

      <div className="flex-1 overflow-auto px-4 sm:px-6 py-4">
        {messages.length === 0 ? (
          <div className="flex items-center justify-center h-full">
            <div className="text-center">
              <div className="w-12 h-12 bg-primary-100 dark:bg-primary-900/30 rounded-xl
                              flex items-center justify-center mx-auto mb-3">
                <svg className="w-6 h-6 text-primary-600 dark:text-primary-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                        d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
                </svg>
              </div>
              <p className="text-sm text-gray-500 dark:text-gray-400">
                Ask a question about your documents
              </p>
            </div>
          </div>
        ) : (
          messages.map((msg) => (
            <ChatMessage key={msg.id} msg={msg} onCitationClick={scrollToCitation} />
          ))
        )}

        {sending && (
          <div className="flex justify-start mb-4">
            <div className="flex items-center gap-2">
              <div className="w-6 h-6 bg-primary-600 rounded-md flex items-center justify-center">
                <span className="text-white text-xs font-bold">D</span>
              </div>
              <div className="flex gap-1 px-4 py-3 bg-gray-100 dark:bg-gray-800 rounded-2xl rounded-bl-md">
                <span className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '0ms' }} />
                <span className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '150ms' }} />
                <span className="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style={{ animationDelay: '300ms' }} />
              </div>
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      <div className="border-t border-gray-200 dark:border-gray-800 bg-white dark:bg-gray-900 px-4 sm:px-6 py-3">
        <form onSubmit={handleSubmit} className="flex items-end gap-3">
          <textarea
            ref={textareaRef}
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            onInput={handleTextareaInput}
            placeholder="Ask a question..."
            rows={1}
            className="input-field resize-none"
            disabled={sending}
          />
          <button
            type="submit"
            disabled={!input.trim() || sending}
            className="btn-primary flex-shrink-0 p-2.5"
          >
            <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                    d="M12 19l9 2-9-18-9 18 9-2zm0 0v-8" />
            </svg>
          </button>
        </form>
      </div>
    </div>
  );
}
