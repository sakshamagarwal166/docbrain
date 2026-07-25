# DocBrain

AI-powered Document Q&A application using Retrieval-Augmented Generation (RAG).

Upload documents (PDF, DOCX, TXT), the system chunks and embeds them, then ask questions answered by an LLM using relevant chunks as context.

## Tech Stack

- Java 17, Spring Boot 3.3
- PostgreSQL 16 + pgvector
- Flyway migrations
- JWT authentication (Spring Security + jjwt)
- Apache Tika (document parsing)
- OpenAI API (GPT-4o-mini + text-embedding-3-small)
- SpringDoc OpenAPI (Swagger UI)
- Docker Compose

## Architecture — RAG Pipeline

```
User Question
    │
    ▼
┌─────────────────┐
│ Embed Question   │  (EmbeddingService)
└────────┬────────┘
         ▼
┌─────────────────┐
│ Vector Search    │  (pgvector cosine similarity)
│ top-k chunks     │
└────────┬────────┘
         ▼
┌─────────────────┐
│ Build Context    │  (format chunks + metadata)
└────────┬────────┘
         ▼
┌─────────────────┐
│ LLM Generation   │  (system prompt + context + question)
└────────┬────────┘
         ▼
   Answer + Citations
```

## Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose
- OpenAI API key (optional — app works in mock mode without it)

## Getting Started

1. **Start PostgreSQL:**

```bash
docker-compose up -d
```

2. **Run the application (mock mode, no API key needed):**

```bash
cd backend
mvn spring-boot:run
```

3. **Run with OpenAI (real embeddings + LLM):**

```bash
cd backend
AI_PROVIDER=openai OPENAI_API_KEY=sk-your-key mvn spring-boot:run
```

4. **Open Swagger UI:**

http://localhost:8080/swagger-ui.html

## Example Usage

```bash
# 1. Register
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"user@example.com","password":"password123"}'

# 2. Upload a document (use the token from register response)
curl -X POST http://localhost:8080/api/documents/upload \
  -H "Authorization: Bearer <token>" \
  -F "file=@mydocument.pdf"

# 3. Create a conversation
curl -X POST http://localhost:8080/api/conversations \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"documentIds":["<document-id>"]}'

# 4. Ask a question
curl -X POST http://localhost:8080/api/conversations/<conv-id>/messages \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{"message":"What is this document about?"}'
```

## API Endpoints

### Authentication
- `POST /api/auth/register` — Register a new user
- `POST /api/auth/login` — Login, returns JWT

### Documents (requires JWT)
- `POST /api/documents/upload` — Upload a document (PDF, DOCX, TXT, max 10MB)
- `GET /api/documents` — List user's documents
- `GET /api/documents/{id}` — Get document details
- `DELETE /api/documents/{id}` — Delete document and chunks
- `POST /api/documents/{id}/reprocess` — Re-embed a document
- `POST /api/documents/reprocess-all` — Re-embed all documents

### Chat (requires JWT)
- `POST /api/chat/query` — One-shot RAG query against selected documents

### Conversations (requires JWT)
- `POST /api/conversations` — Create conversation with selected documents
- `GET /api/conversations` — List conversations
- `GET /api/conversations/{id}` — Get full conversation with messages
- `POST /api/conversations/{id}/messages` — Send message, get AI response
- `DELETE /api/conversations/{id}` — Delete conversation

### Chunks (requires JWT)
- `GET /api/chunks/search?q=...&documentIds=...&topK=5` — Vector similarity search (no LLM)

## Configuration

Copy `.env.example` to `.env` and customize values. Key settings:

| Variable | Default | Description |
|---|---|---|
| `AI_PROVIDER` | mock | `mock` or `openai` |
| `OPENAI_API_KEY` | — | Required when provider is `openai` |
| `OPENAI_CHAT_MODEL` | gpt-4o-mini | Chat completions model |
| `OPENAI_EMBEDDING_MODEL` | text-embedding-3-small | Embeddings model |
| `RAG_TOP_K` | 5 | Number of chunks retrieved per query |
| `RAG_MAX_HISTORY_PAIRS` | 5 | Conversation history pairs sent to LLM |
| `RATE_LIMIT_MAX_REQUESTS` | 20 | Max query requests per minute per user |
| `CHUNK_SIZE` | 1000 | Text chunk size in characters |
| `CHUNK_OVERLAP` | 200 | Overlap between chunks |

## Project Structure

```
backend/src/main/java/com/docbrain/
├── config/          # Async, OpenAPI, AI provider configuration
├── controller/      # REST controllers (Auth, Document, Chat, Conversation, Chunk)
├── dto/             # Request/response DTOs
├── exception/       # Custom exceptions, global handler
├── model/           # JPA entities (User, Document, Chunk, Conversation, Message)
├── repository/      # Spring Data JPA repositories
├── security/        # JWT auth, Spring Security config, rate limiter
└── service/         # Business logic (RAG pipeline, chunking, embedding, LLM)
```
