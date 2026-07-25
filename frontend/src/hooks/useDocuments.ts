import { useState, useEffect, useCallback, useRef } from 'react';
import type { DocumentResponse } from '../types/document';
import { listDocuments, uploadDocument, deleteDocument } from '../services/documentService';
import { useToast } from '../context/ToastContext';

export function useDocuments() {
  const [documents, setDocuments] = useState<DocumentResponse[]>([]);
  const [loading, setLoading] = useState(true);
  const { addToast } = useToast();
  const pollRef = useRef<ReturnType<typeof setInterval>>();

  const fetch = useCallback(async () => {
    try {
      const docs = await listDocuments();
      setDocuments(docs);
    } catch {
      addToast('Failed to load documents', 'error');
    } finally {
      setLoading(false);
    }
  }, [addToast]);

  useEffect(() => {
    fetch();
  }, [fetch]);

  useEffect(() => {
    const hasProcessing = documents.some((d) => d.status === 'PROCESSING' || d.status === 'UPLOADED');
    if (hasProcessing) {
      pollRef.current = setInterval(fetch, 3000);
    } else if (pollRef.current) {
      clearInterval(pollRef.current);
    }
    return () => {
      if (pollRef.current) clearInterval(pollRef.current);
    };
  }, [documents, fetch]);

  const upload = useCallback(async (file: File) => {
    try {
      const doc = await uploadDocument(file);
      setDocuments((prev) => [doc, ...prev]);
      addToast('Document uploaded successfully', 'success');
      return doc;
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message || 'Upload failed';
      addToast(msg, 'error');
      throw err;
    }
  }, [addToast]);

  const remove = useCallback(async (id: string) => {
    try {
      await deleteDocument(id);
      setDocuments((prev) => prev.filter((d) => d.id !== id));
      addToast('Document deleted', 'success');
    } catch {
      addToast('Failed to delete document', 'error');
    }
  }, [addToast]);

  return { documents, loading, upload, remove, refresh: fetch };
}
