# DocBrain

AI-powered Document Q&A application using Retrieval-Augmented Generation (RAG).

Upload documents (PDF, DOCX, TXT), the system chunks and embeds them, and later you can ask questions answered by an LLM using relevant chunks as context.

## Tech Stack

- Java 17, Spring Boot 3.3
- PostgreSQL 16 + pgvector
- Flyway migrations
- JWT authentication (Spring Security + jjwt)
- Apache Tika (document parsing)
- SpringDoc OpenAPI (Swagger UI)
- Docker Compose

## Prerequisites

- Java 17+
- Maven 3.8+
- Docker & Docker Compose

## Getting Started

1. **Start PostgreSQL:**

```bash
docker-compose up -d
```

2. **Run the application:**

```bash
cd backend
mvn spring-boot:run
```

3. **Open Swagger UI:**

http://localhost:8080/swagger-ui.html

## API Endpoints

### Authentication
- `POST /api/auth/register` — Register a new user
- `POST /api/auth/login` — Login, returns JWT

### Documents (requires JWT)
- `POST /api/documents/upload` — Upload a document (PDF, DOCX, TXT, max 10MB)
- `GET /api/documents` — List user's documents
- `GET /api/documents/{id}` — Get document details
- `DELETE /api/documents/{id}` — Delete document and chunks

## Configuration

Copy `.env.example` to `.env` and customize values. Key settings:

| Variable | Default | Description |
|---|---|---|
| `POSTGRES_DB` | docbrain | Database name |
| `POSTGRES_USER` | docbrain | Database user |
| `POSTGRES_PASSWORD` | docbrain | Database password |
| `JWT_SECRET` | (dev default) | JWT signing secret (change in prod) |
| `CHUNK_SIZE` | 1000 | Text chunk size in characters |
| `CHUNK_OVERLAP` | 200 | Overlap between chunks |
| `EMBEDDING_DIMENSION` | 1536 | Embedding vector dimension |

## Project Structure

```
backend/src/main/java/com/docbrain/
├── config/          # Async, OpenAPI configuration
├── controller/      # REST controllers
├── dto/             # Request/response DTOs
├── exception/       # Custom exceptions, global handler
├── model/           # JPA entities
├── repository/      # Spring Data JPA repositories
├── security/        # JWT auth, Spring Security config
└── service/         # Business logic
```
