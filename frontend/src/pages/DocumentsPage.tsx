import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useDocuments } from '../hooks/useDocuments';
import { useConversations } from '../hooks/useConversations';
import UploadModal from '../components/UploadModal';
import { SkeletonList } from '../components/Skeleton';
import type { DocumentStatus } from '../types/document';

const statusConfig: Record<DocumentStatus, { label: string; color: string }> = {
  UPLOADED: { label: 'Uploaded', color: 'bg-gray-100 text-gray-600 dark:bg-gray-700 dark:text-gray-300' },
  PROCESSING: { label: 'Processing', color: 'bg-yellow-100 text-yellow-700 dark:bg-yellow-900/30 dark:text-yellow-400' },
  READY: { label: 'Ready', color: 'bg-green-100 text-green-700 dark:bg-green-900/30 dark:text-green-400' },
  FAILED: { label: 'Failed', color: 'bg-red-100 text-red-700 dark:bg-red-900/30 dark:text-red-400' },
};

export default function DocumentsPage() {
  const { documents, loading, upload, remove } = useDocuments();
  const { create } = useConversations();
  const [uploadOpen, setUploadOpen] = useState(false);
  const [deleteId, setDeleteId] = useState<string | null>(null);
  const navigate = useNavigate();

  const handleStartChat = async (docId: string) => {
    const conv = await create([docId]);
    if (conv) navigate(`/conversations/${conv.id}`);
  };

  const handleDelete = async () => {
    if (deleteId) {
      await remove(deleteId);
      setDeleteId(null);
    }
  };

  const formatDate = (iso: string) =>
    new Date(iso).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });

  const formatSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <div className="p-6 max-w-5xl mx-auto">
      <div className="flex items-center justify-between mb-6">
        <h1 className="text-2xl font-bold text-gray-900 dark:text-white">My Documents</h1>
        <button onClick={() => setUploadOpen(true)} className="btn-primary flex items-center gap-2">
          <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
          </svg>
          Upload
        </button>
      </div>

      {loading ? (
        <SkeletonList count={3} />
      ) : documents.length === 0 ? (
        <div className="text-center py-16">
          <svg className="w-16 h-16 mx-auto text-gray-300 dark:text-gray-600 mb-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1}
                  d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z" />
          </svg>
          <h3 className="text-lg font-medium text-gray-600 dark:text-gray-400 mb-2">No documents yet</h3>
          <p className="text-sm text-gray-400 dark:text-gray-500 mb-4">Upload a PDF, DOCX, or TXT to get started</p>
          <button onClick={() => setUploadOpen(true)} className="btn-primary">
            Upload your first document
          </button>
        </div>
      ) : (
        <div className="space-y-3">
          {documents.map((doc) => {
            const status = statusConfig[doc.status];
            return (
              <div key={doc.id} className="card p-4 flex items-center justify-between gap-4">
                <div className="flex-1 min-w-0">
                  <div className="flex items-center gap-3 mb-1">
                    <h3 className="text-sm font-medium truncate">{doc.originalFilename}</h3>
                    <span className={`inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium ${status.color}`}>
                      {doc.status === 'PROCESSING' && (
                        <svg className="w-3 h-3 animate-spin" viewBox="0 0 24 24">
                          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4" fill="none" />
                          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z" />
                        </svg>
                      )}
                      {status.label}
                    </span>
                  </div>
                  <p className="text-xs text-gray-500 dark:text-gray-400">
                    {formatSize(doc.fileSizeBytes)} &middot; {doc.totalChunks} chunks &middot; {formatDate(doc.createdAt)}
                  </p>
                </div>
                <div className="flex items-center gap-2">
                  {doc.status === 'READY' && (
                    <button
                      onClick={() => handleStartChat(doc.id)}
                      className="btn-primary text-xs py-1.5 px-3"
                    >
                      Start Chat
                    </button>
                  )}
                  <button
                    onClick={() => setDeleteId(doc.id)}
                    className="text-gray-400 hover:text-red-500 transition-colors p-1"
                  >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2}
                            d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                    </svg>
                  </button>
                </div>
              </div>
            );
          })}
        </div>
      )}

      <UploadModal open={uploadOpen} onClose={() => setUploadOpen(false)} onUpload={upload} />

      {deleteId && (
        <div className="fixed inset-0 z-40 flex items-center justify-center">
          <div className="absolute inset-0 bg-black/50" onClick={() => setDeleteId(null)} />
          <div className="relative card p-6 max-w-sm mx-4">
            <h3 className="text-lg font-semibold mb-2">Delete document?</h3>
            <p className="text-sm text-gray-500 dark:text-gray-400 mb-4">
              This will permanently delete the document and all its chunks.
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
