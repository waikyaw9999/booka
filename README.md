# Booka — Hotel Booking & Property Management

Full-stack hotel booking platform with guest booking flows, Front Office PMS tools, housekeeping, folios, channel reservation hooks, and dual-audience RAG assistants powered by Groq. Online payments (Stripe Checkout) are deferred — guest bookings confirm without payment for now.

## Stack

| Layer | Technology |
|-------|------------|
| Backend | Java 17, Spring Boot 3, JPA, Liquibase, JWT, Stripe Java SDK |
| Frontend | React 19, TypeScript, Vite, Tailwind CSS v4 |
| State | Zustand (auth, booking draft, chat), TanStack Query |
| Database | PostgreSQL 16 |
| AI | Groq via OpenAI-compatible API |
| Payments | Stripe Checkout + webhooks (deferred; guest path skips Checkout) |

## Features

- **Guests**: browse hotels, availability, book rooms (confirmed without online payment for now), AI help assistant
- **Front Office**: room board, check-in/out, room assignment
- **Housekeeping & maintenance**: CLEAN / DIRTY / INSPECTING + maintenance tickets
- **Folios**: room/tax lines, POS F&B charges routed to the guest bill
- **CRS / channels**: confirmation codes, walk-in reservations, inbound OTA hooks (`X-Channel-Api-Key`)
- **Staff/Admin**: dashboard, blackout dates, staff RAG (public + SOPs)

## Quick start (Docker)

1. Copy environment file:
   ```bash
   cp .env.example .env
   ```

2. Add keys to `.env` as needed:
   ```
   GROQ_API_KEY=gsk_...
   STRIPE_SECRET_KEY=sk_test_...
   STRIPE_WEBHOOK_SECRET=whsec_...
   CHANNEL_API_KEY=dev-channel-key
   ```

3. Start all services:
   ```bash
   docker compose up --build
   ```

4. Open:
   - Frontend: http://localhost:5173
   - Backend API: http://localhost:8080

For local Stripe webhooks: `stripe listen --forward-to localhost:8080/api/v1/payments/stripe/webhook`

## Production deploy (VPS + Docker Hub)

CI/CD ships images to Docker Hub, then SSHs to the VPS to pull and restart Compose. Repo assets: `.github/workflows/deploy.yml`, `docker-compose.prod.yml`, `.env.example`.

Full setup (GitHub secrets, one-time VPS steps, rollout order): **[docs/VPS_DOCKER_CICD.md](docs/VPS_DOCKER_CICD.md)**.

## Demo credentials

| Role | Email | Password |
|------|-------|----------|
| Admin | admin@booka.com | password |
| Staff | staff@booka.com | password |

## Local development

### Backend (requires Java 17 + Maven + PostgreSQL)

```bash
cd backend
mvn spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
```

## API overview

| Endpoint | Access | Description |
|----------|--------|-------------|
| `GET /api/v1/hotels` | Public | List hotels |
| `POST /api/v1/hotels/{id}/availability` | Public | Check room availability |
| `POST /api/v1/bookings` | Public | Create confirmed direct booking + folio (`N_A` payment) |
| `POST /api/v1/bookings/{id}/checkout-session` | Public | Start Stripe Checkout (unused by guest UI; for later) |
| `POST /api/v1/payments/stripe/webhook` | Stripe | Confirm payment / booking (when Checkout enabled) |
| `POST /api/v1/channels/{channel}/reservations` | Channel key | Idempotent OTA upsert |
| `GET /api/v1/admin/rooms` | Staff | Front Office room board |
| `POST /api/v1/admin/bookings/{id}/check-in\|check-out\|assign-room` | Staff | FO actions |
| `PATCH /api/v1/admin/rooms/{id}/housekeeping` | Staff | Housekeeping status |
| `GET/POST/PATCH /api/v1/admin/maintenance-tickets` | Staff | Maintenance tickets |
| `GET/POST /api/v1/admin/folios/{bookingId}[/charges]` | Staff | Folio + POS charges |
| `GET/POST/DELETE /api/v1/admin/blackout-dates` | Staff | Blackout dates |
| `POST /api/v1/rag/public/chat` | Public | Guest RAG |
| `POST /api/v1/rag/chat` | Staff | Staff RAG |

## Project structure

```
booka/
├── .github/workflows/   # CI/CD (test → Docker Hub → VPS)
├── backend/             # Spring Boot API
├── frontend/            # React SPA
├── docker-compose.yml
├── docker-compose.prod.yml
├── docs/VPS_DOCKER_CICD.md
└── .cursorrules         # AI coding guidelines
```
