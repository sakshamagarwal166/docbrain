import api from './api';
import type { DocumentResponse } from '../types/document';

export async function listDocuments(): Promise<DocumentResponse[]> {
  const res = await api.get<DocumentResponse[]>('/documents');
  return res.data;
}

export async function getDocument(id: string): Promise<DocumentResponse> {
  const res = await api.get<DocumentResponse>(`/documents/${id}`);
  return res.data;
}

export async function uploadDocument(file: File): Promise<DocumentResponse> {
  const formData = new FormData();
  formData.append('file', file);
  const res = await api.post<DocumentResponse>('/documents/upload', formData, {
    headers: { 'Content-Type': 'multipart/form-data' },
  });
  return res.data;
}

export async function deleteDocument(id: string): Promise<void> {
  await api.delete(`/documents/${id}`);
}
