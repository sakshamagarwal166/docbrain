import api from './api';
import type { ChunkSearchResult } from '../types/chunk';

export async function searchChunks(
  query: string,
  documentIds: string[],
  topK: number = 5
): Promise<ChunkSearchResult[]> {
  const params = new URLSearchParams();
  params.set('q', query);
  documentIds.forEach((id) => params.append('documentIds', id));
  params.set('topK', String(topK));
  const res = await api.get<ChunkSearchResult[]>(`/chunks/search?${params}`);
  return res.data;
}
