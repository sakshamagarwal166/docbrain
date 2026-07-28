# DocBrain

[![CI](https://github.com/sakshamagarwal166/docbrain/actions/workflows/ci.yml/badge.svg)](https://github.com/sakshamagarwal166/docbrain/actions/workflows/ci.yml)

AI-powered Document Q&A application using Retrieval-Augmented Generation (RAG). Upload documents, ask questions, get cited answers.

## Architecture

```
                         ┌─────────────────────────────────────────────┐
                         │              Docker Compose                 │
                         │                                             │
   User ──► Browser ──►  │  ┌───────────┐    ┌──────────────────────┐  │
                         │  │  Frontend │    │      Backend         │  │
                         │  │  (Nginx)  │───►│   (Spring Boot)      │  │
                         │  │  port 80  │/api│    port 8080         │  │
                         │  └───────────┘    └──────────┬───────────┘  │
                         │                              │              │
                         │                   ┌──────────▼───────────┐  │
                         │                   │   PostgreSQL 16      │  │
                         │                   │   + pgvector         │  │
                         │                   └──────────────────────┘  │
                         └─────────────────────────────────────────────┘
```

### RAG Pipeline

```
  Upload Flow                          Query Flow
  ───────────                          ──────────
  Document (PDF/DOCX/TXT)              User Question
       │                                    │
       ▼                                    ▼
  Tika Parse → Text                   Embed Question
       │                                    │
       ▼                                    ▼
  Recursive Chunking                  Vector Search (pgvector)
       │                              top-k similar chunks
       ▼                                    │
  Embed Chunks (OpenAI)                     ▼
       │                              Build Context + History
       ▼                                    │
  Store in pgvector                         ▼
                                      LLM Generation (GPT-4o)
                                            │
                                            ▼
                                      Answer + Citations
```

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18, TypeScript, TailwindCSS, Vite |
| Backend | Java 17, Spring Boot 3.3, Spring Security |
| Database | PostgreSQL 16 + pgvector |
| AI | OpenAI (GPT-4o-mini, text-embedding-3-small) |
| Infrastructure | Docker, Nginx, GitHub Actions |
| Tools | Flyway, Apache Tika, Swagger UI |

## Features

- **Document Upload** — PDF, DOCX, TXT (max 10MB) with async processing
- **RAG-Powered Q&A** — Answers grounded in your documents with source citations
- **Multi-Turn Conversations** — Conversational context across follow-up questions
- **JWT Authentication** — Secure user accounts with token-based auth
- **Dark Mode** — System-aware theme with manual toggle
- **Responsive Design** — Works on desktop and mobile
- **Mock Mode** — Full functionality without an API key for development

## Getting Started

### Quick Start (Docker Compose)

Run the entire stack with one command:

```bash
cp .env.example .env
docker-compose up --build
```

Open [http://localhost](http://localhost) — register, upload a document, and start chatting.

### Development Setup

For local development with hot reload:

1. **Start the database:**

```bash
docker-compose -f docker-compose.dev.yml up -d
```

2. **Run the backend:**

```bash
cd backend
mvn spring-boot:run
```

3. **Run the frontend:**

```bash
cd frontend
npm install
npm run dev
```

Open [http://localhost:5173](http://localhost:5173)

### Using OpenAI (Real AI Responses)

Set these in your `.env` file:

```env
AI_PROVIDER=openai
OPENAI_API_KEY=sk-your-key-here
```

Without an API key, the app runs in mock mode — all features work, but responses are placeholder text.

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `AI_PROVIDER` | `mock` | `mock` (no API key) or `openai` |
| `OPENAI_API_KEY` | — | Required when provider is `openai` |
| `OPENAI_CHAT_MODEL` | `gpt-4o-mini` | Chat completions model |
| `OPENAI_EMBEDDING_MODEL` | `text-embedding-3-small` | Embeddings model |
| `POSTGRES_DB` | `docbrain` | Database name |
| `POSTGRES_USER` | `docbrain` | Database user |
| `POSTGRES_PASSWORD` | `docbrain` | Database password |
| `JWT_SECRET` | dev default | HMAC secret (change in production) |
| `RAG_TOP_K` | `5` | Chunks retrieved per query |
| `RAG_MAX_HISTORY_PAIRS` | `5` | Conversation history pairs sent to LLM |
| `CHUNK_SIZE` | `1000` | Text chunk size in characters |
| `CHUNK_OVERLAP` | `200` | Overlap between chunks |
| `RATE_LIMIT_MAX_REQUESTS` | `20` | Max requests per minute per user |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost` | Allowed CORS origins |

## API Documentation

Interactive API docs available at [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html) when the backend is running.

### Endpoints

| Method | Path | Auth | Description |
|---|---|---|---|
| POST | `/api/auth/register` | No | Register a new user |
| POST | `/api/auth/login` | No | Login, returns JWT |
| POST | `/api/documents/upload` | Yes | Upload document (PDF, DOCX, TXT) |
| GET | `/api/documents` | Yes | List user's documents |
| GET | `/api/documents/{id}` | Yes | Get document details |
| DELETE | `/api/documents/{id}` | Yes | Delete document and chunks |
| POST | `/api/conversations` | Yes | Create conversation with documents |
| GET | `/api/conversations` | Yes | List conversations |
| GET | `/api/conversations/{id}` | Yes | Get conversation with messages |
| POST | `/api/conversations/{id}/messages` | Yes | Send message, get AI response |
| DELETE | `/api/conversations/{id}` | Yes | Delete conversation |
| POST | `/api/chat/query` | Yes | One-shot RAG query |
| GET | `/api/chunks/search` | Yes | Vector similarity search |
| GET | `/actuator/health` | No | Health check |

## Project Structure

```
docbrain/
├── backend/
│   ├── Dockerfile
│   └── src/main/java/com/docbrain/
│       ├── config/         # CORS, async, AI provider, request logging
│       ├── controller/     # REST controllers
│       ├── dto/            # Request/response objects
│       ├── exception/      # Custom exceptions, global handler
│       ├── model/          # JPA entities
│       ├── repository/     # Data access (including pgvector queries)
│       ├── security/       # JWT, Spring Security, rate limiter
│       └── service/        # RAG pipeline, chunking, embedding, LLM
├── frontend/
│   ├── Dockerfile
│   ├── nginx.conf
│   └── src/
│       ├── components/     # Layout, Sidebar, Modal, Toast, Skeleton
│       ├── context/        # Auth, Toast, Theme providers
│       ├── hooks/          # useDocuments, useConversations, useChat
│       ├── pages/          # Login, Register, Documents, Conversations, Chat, 404
│       ├── services/       # API client, auth, document, conversation services
│       └── types/          # TypeScript interfaces
├── docker-compose.yml      # Full stack (db + backend + frontend)
├── docker-compose.dev.yml  # Database only (for local development)
├── .github/workflows/      # CI pipeline
└── .env.example            # Environment variable template
```

## Screenshots

<img width="599" height="609" alt="Screenshot 2026-07-26 190056" src="https://github.com/user-attachments/assets/5696f8f4-8f97-48aa-958e-a39bac2cb08a" />
<img width="1440" height="900" alt="Screenshot 2026-07-26 190154" src="https://github.com/user-attachments/assets/0ce8331a-6ca4-4b3f-a334-7ac1a726a46b" />
<img width="1440" height="900" alt="Screenshot 2026-07-26 190742" src="https://github.com/user-attachments/assets/9ef363cb-43eb-4a02-aef6-5001f16b6ad8" />
<img width="1440" height="900" alt="Screenshot 2026-07-26 190829" src="https://github.com/user-attachments/assets/711e8bd8-c99b-4620-8b9f-c6a4576d5896" />

## License

MIT
