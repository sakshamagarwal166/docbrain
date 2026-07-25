export type DocumentStatus = 'UPLOADED' | 'PROCESSING' | 'READY' | 'FAILED';

export interface DocumentResponse {
  id: string;
  originalFilename: string;
  contentType: string;
  fileSizeBytes: number;
  totalChunks: number;
  status: DocumentStatus;
  createdAt: string;
}
