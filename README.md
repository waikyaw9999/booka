# Booka — Hotel Booking & Management

Full-stack hotel booking platform with guest booking flows, staff admin dashboard, and dual-audience RAG assistants powered by Groq (OpenAI-compatible API).

## Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 17, Spring Boot 3, JPA, Liquibase, JWT |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS v4 |
| State | Zustand (auth, booking draft, chat), TanStack Query |
| Database | PostgreSQL 16 |
| AI | Groq (`llama-3.3-70b-versatile`) via OpenAI-compatible API |

## Features

- **Guests**: browse hotels, check availability, book rooms, AI help assistant (public docs only)
- **Staff/Admin**: dashboard, booking management, check-in/out status updates, staff RAG assistant (public + internal SOPs), **blackout date management**
- **RAG**: retrieval with audience filtering (`PUBLIC` / `STAFF`), Groq generation, source citations, rate limiting

## Quick start (Docker)

1. Copy environment file:
   ```bash
   cp .env.example .env
   ```

2. Add your Groq API key to `.env`:
   ```
   GROQ_API_KEY=gsk_...
   ```

3. Start all services:
   ```bash
   docker compose up --build
   ```

4. Open:
   - Frontend: http://localhost:5173
   - Backend API: http://localhost:8080

## Demo credentials

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@booka.com | password |
| Staff | staff@booka.com | password |

## Local development

### Backend (requires Java 17 + Maven + PostgreSQL)

```bash
cd backend
# Start PostgreSQL (or use docker compose up postgres)
mvn spring-boot:run
```

Default dev profile connects to `jdbc:postgresql://localhost:5432/booka`.

### Frontend

```bash
cd frontend
npm install
npm run dev
```

Vite proxies `/api` to `http://localhost:8080`.

## API overview

| Endpoint | Access | Description |
|----------|--------|-------------|
| `GET /api/v1/hotels` | Public | List hotels |
| `POST /api/v1/hotels/{id}/availability` | Public | Check room availability |
| `POST /api/v1/bookings` | Public | Create booking |
| `GET /api/v1/hotels/{id}/blackout-dates` | Public | List blackout periods (guest date hints) |
| `POST /api/v1/rag/public/chat` | Public | Guest RAG assistant |
| `POST /api/v1/rag/chat` | Staff | Staff RAG assistant |
| `GET /api/v1/admin/dashboard` | Staff | Dashboard stats |
| `GET /api/v1/admin/bookings` | Staff | List bookings |
| `GET/POST/DELETE /api/v1/admin/blackout-dates` | Staff | Manage blackout dates |

## RAG notes

- Groq does not provide embeddings; the backend uses a local semantic+keyword scoring approach with optional stored embeddings.
- Without `GROQ_API_KEY`, the assistant falls back to retrieved document excerpts.
- Staff users can access both `PUBLIC` and `STAFF` knowledge; guests only see `PUBLIC` content.

## Project structure

```
booka/
├── backend/          # Spring Boot API
├── frontend/         # React SPA
├── docker-compose.yml
└── .cursorrules      # AI coding guidelines
```
