import { useState, useRef, type DragEvent } from 'react';
import Modal from './Modal';

interface UploadModalProps {
  open: boolean;
  onClose: () => void;
  onUpload: (file: File) => Promise<unknown>;
}

const ACCEPTED_TYPES = [
  'application/pdf',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  'text/plain',
];
const MAX_SIZE = 10 * 1024 * 1024;

export default function UploadModal({ open, onClose, onUpload }: UploadModalProps) {
  const [file, setFile] = useState<File | null>(null);
  const [dragging, setDragging] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState('');
  const inputRef = useRef<HTMLInputElement>(null);

  const reset = () => {
    setFile(null);
    setError('');
    setUploading(false);
  };

  const handleClose = () => {
    reset();
    onClose();
  };

  const validate = (f: File): string | null => {
    if (!ACCEPTED_TYPES.includes(f.type) && !f.name.endsWith('.txt')) {
      return 'Only PDF, DOCX, and TXT files are supported';
    }
    if (f.size > MAX_SIZE) return 'File size exceeds 10MB limit';
    return null;
  };

  const handleSelect = (f: File) => {
    const err = validate(f);
    if (err) {
      setError(err);
      setFile(null);
    } else {
      setError('');
      setFile(f);
    }
  };

  const handleDrop = (e: DragEvent) => {
    e.preventDefault();
    setDragging(false);
    const f = e.dataTransfer.files[0];
    if (f) handleSelect(f);
  };

  const handleUpload = async () => {
    if (!file) return;
    setUploading(true);
    try {
      await onUpload(file);
      handleClose();
    } catch {
      setUploading(false);
    }
  };

  const formatSize = (bytes: number) => {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
  };

  return (
    <Modal open={open} onClose={handleClose} title="Upload Document">
      <div
        className={`border-2 border-dashed rounded-xl p-8 text-center cursor-pointer
                    transition-colors ${
                      dragging
                        ? 'border-primary-500 bg-primary-50 dark:bg-primary-900/20'
                        : 'border-gray-300 dark:border-gray-600 hover:border-primary-400'
                    }`}
        onDragOver={(e) => { e.preventDefault(); setDragging(true); }}
        onDragLeave={() => setDragging(false)}
        onDrop={handleDrop}
        onClick={() => inputRef.current?.click()}
      >
        <svg className="w-10 h-10 mx-auto text-gray-400 mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5}
                d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12" />
        </svg>
        <p className="text-sm text-gray-600 dark:text-gray-400">
          Drag and drop a file here, or <span className="text-primary-600 font-medium">browse</span>
        </p>
        <p className="text-xs text-gray-400 mt-1">PDF, DOCX, or TXT (max 10MB)</p>
        <input
          ref={inputRef}
          type="file"
          accept=".pdf,.docx,.txt"
          className="hidden"
          onChange={(e) => {
            const f = e.target.files?.[0];
            if (f) handleSelect(f);
          }}
        />
      </div>

      {error && (
        <p className="text-sm text-red-500 mt-3">{error}</p>
      )}

      {file && !error && (
        <div className="mt-4 flex items-center justify-between p-3 bg-gray-50 dark:bg-gray-800 rounded-lg">
          <div>
            <p className="text-sm font-medium truncate">{file.name}</p>
            <p className="text-xs text-gray-500">{formatSize(file.size)}</p>
          </div>
          <button onClick={() => setFile(null)} className="text-gray-400 hover:text-gray-600 text-sm">
            Remove
          </button>
        </div>
      )}

      <div className="flex justify-end gap-3 mt-6">
        <button onClick={handleClose} className="btn-secondary" disabled={uploading}>
          Cancel
        </button>
        <button onClick={handleUpload} className="btn-primary" disabled={!file || uploading || !!error}>
          {uploading ? 'Uploading...' : 'Upload'}
        </button>
      </div>
    </Modal>
  );
}
