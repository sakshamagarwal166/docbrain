export interface ChunkSearchResult {
  chunkId: string;
  documentId: string;
  documentName: string;
  chunkIndex: number;
  content: string;
  similarityScore: number;
  metadata: string;
}

export interface CitedSource {
  documentId: string;
  documentName: string;
  chunkIndex: number;
  pageNumber: number | null;
  relevantText: string;
}
