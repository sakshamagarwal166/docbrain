import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useConversations } from '../hooks/useConversations';
import { listDocuments } from '../services/documentService';
import Modal from '../components/Modal';
import { SkeletonList } from '../components/Skeleton';
import type { DocumentResponse } from '../types/document';

export default function ConversationsPage() {
  const { conversations, loading, create, remove } = useConversations();
  const [newOpen, setNewOpen] = useState(false);
  const [deleteId, setDeleteId] = useState<string | null>(null);
  const [readyDocs, setReadyDocs] = useState<DocumentResponse[]>([]);
  const [selectedDocIds, setSelectedDocIds] = useState<string[]>([]);
  const [creating, setCreating] = useState(false);
  const navigate = useNavigate();

  useEffect(() => {
    if (newOpen) {
      listDocuments().then((docs) =>
        setReadyDocs(docs.filter((d) => d.status === 'READY'))
      );
    }
  }, [newOpen]);

  const handleCreate = async () => {
    if (selectedDocIds.length === 0) return;
    setCreating(true);
    const conv = await create(selectedDocIds);
    setCreating(false);
    if (conv) {
      setNewOpen(false);
      setSelectedDocIds([]);
      navigate(`/conversations/${conv.id}`);
    }
  };

  const toggleDoc = (id: string) => {
    setSelectedDocIds((prev) =>
      prev.includes(id) ? prev.filter((d) => d !== id) : [...prev, id]
    );
  };

  const handleDelete = async () => {
    if (deleteId) {
      await remove(deleteId);
      setDeleteId(null);
    }
  };

  const formatDate = (iso: string) =>
    new Date(iso).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });

  return (
    <div className="p-6 max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">Conversations</h1>
        <button onClick={() => setNewOpen(true)} className="btn-primary flex items-center gap-2">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          New Conversation
        </button>
      </div>

      {loading ? (
        <SkeletonList count={3} />
      ) : conversations.length === 0 ? (
        <div className="text-center py-16">
          <svg className="w-16 h-16 mx-auto text-gray-300 dark:text-gray-600 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1}
                  d="M8 12h.01M12 12h.01M16 12h.01M21 12c0 4.418-4.03 8-9 8a9.863 9.863 0 01-4.255-.949L3 20l1.395-3.72C3.512 15.042 3 13.574 3 12c0-4.418 4.03-8 9-8s9 3.582 9 8z" />
          </svg>
          <h3 className="text-lg font-medium text-gray-600 dark:text-gray-400 mb-2">No conversations yet</h3>
          <p className="text-sm text-gray-400 dark:text-gray-500 mb-4">Start a conversation to ask questions about your documents</p>
          <button onClick={() => setNewOpen(true)} className="btn-primary">
            Start your first conversation
          </button>
        </div>
      ) : (
        <div className="space-y-3">
          {conversations.map((conv) => (
            <div
              key={conv.id}
              className="card p-4 cursor-pointer hover:border-primary-300 dark:hover:border-primary-700
                         transition-colors flex items-center justify-between gap-4"
              onClick={() => navigate(`/conversations/${conv.id}`)}
            >
              <div className="flex-1 min-w-0">
                <h3 className="text-sm font-medium truncate mb-1">{conv.title}</h3>
                <div className="flex items-center gap-2 flex-wrap">
                  {conv.documentNames.map((name, i) => (
                    <span key={i} className="inline-flex items-center px-2 py-0.5 rounded text-xs
                                             bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400">
                      {name}
                    </span>
                  ))}
                  <span className="text-xs text-gray-400">&middot; {formatDate(conv.createdAt)}</span>
                </div>
              </div>
              <button
                onClick={(e) => { e.stopPropagation(); setDeleteId(conv.id); }}
                className="text-gray-400 hover:text-red-500 transition-colors p-1"
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                        d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                </svg>
              </button>
            </div>
          ))}
        </div>
      )}

      <Modal open={newOpen} onClose={() => { setNewOpen(false); setSelectedDocIds([]); }} title="New Conversation">
        <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
          Select documents to chat with:
        </p>
        {readyDocs.length === 0 ? (
          <p className="text-sm text-gray-400 py-4 text-center">No ready documents. Upload one first.</p>
        ) : (
          <div className="space-y-2 max-h-60 overflow-y-auto">
            {readyDocs.map((doc) => (
              <label
                key={doc.id}
                className={`flex items-center gap-3 p-3 rounded-lg cursor-pointer transition-colors
                            ${selectedDocIds.includes(doc.id)
                              ? 'bg-primary-50 dark:bg-primary-900/20 border border-primary-200 dark:border-primary-800'
                              : 'hover:bg-gray-50 dark:hover:bg-gray-800 border border-transparent'}`}
              >
                <input
                  type="checkbox"
                  checked={selectedDocIds.includes(doc.id)}
                  onChange={() => toggleDoc(doc.id)}
                  className="rounded text-primary-600 focus:ring-primary-500"
                />
                <div>
                  <p className="text-sm font-medium">{doc.originalFilename}</p>
                  <p className="text-xs text-gray-400">{doc.totalChunks} chunks</p>
                </div>
              </label>
            ))}
          </div>
        )}
        <div className="flex justify-end gap-3 mt-6">
          <button onClick={() => { setNewOpen(false); setSelectedDocIds([]); }} className="btn-secondary">
            Cancel
          </button>
          <button
            onClick={handleCreate}
            className="btn-primary"
            disabled={selectedDocIds.length === 0 || creating}
          >
            {creating ? 'Creating...' : 'Start Conversation'}
          </button>
        </div>
      </Modal>

      {deleteId && (
        <div className="fixed inset-0 z-40 flex items-center justify-center">
          <div className="absolute inset-0 bg-black/50" onClick={() => setDeleteId(null)} />
          <div className="relative card p-6 max-w-sm mx-4">
            <h3 className="text-lg font-semibold mb-2">Delete conversation?</h3>
            <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
              This will permanently delete the conversation and all messages.
            </p>
            <div className="flex justify-end gap-3">
              <button onClick={() => setDeleteId(null)} className="btn-secondary">Cancel</button>
              <button onClick={handleDelete} className="btn-danger">Delete</button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
