# Booka multi-property model

Booka is sold as a **shared guest marketplace** with **per-property staff tenancy**.

## Roles

| Role | Audience | Scope |
|------|----------|--------|
| *(anonymous / guest booker)* | Guest site | Browse and book any listed hotel |
| `GUEST` | Optional guest account | Same public booking APIs; no Operations access (`hotel_id` null; FK allows null) |
| `ADMIN` | Property manager | Full Operations for **one** `hotelId`; Team + Billing |
| `STAFF` | Front desk / hotel team | Operations for **one** `hotelId` |

**Deferred:** platform `SUPER_ADMIN` / multi-hotel owner org. Not required to sell a single-property account; buyers onboard via register-property. Revisit when Booka needs a central operator console.

## Integrity

- `users.hotel_id` → `hotels.id` FK exists (`fk_users_hotel` from schema init). Guests may be null; STAFF/ADMIN must be set.
- Staff accounts have `users.active`; deactivated users cannot sign in.
- Channel API keys are **per hotel** (`channel_api_keys.hotel_id`). Global keys are deactivated. Inbound reservations must present a key that matches `request.hotelId`, and room types/rooms must belong to that hotel.

## Tenancy rules

- Admin APIs resolve the hotel from the authenticated user.
- Cross-property booking codes, folios, rooms, blackouts, and tickets are rejected.
- Guests still see **all** hotels on the public site.

## Onboarding

1. **Self-serve:** `/register-property` or `POST /api/v1/auth/register-property`  
   Creates hotel (30-day **TRIAL**), seeds Standard + Deluxe rooms (101/102/201), ensures channel keys when `CHANNEL_API_KEY` is set, creates manager (`ADMIN`).
2. **Invite team:** Operations → **Team** — create STAFF, deactivate/reactivate, reset password.
3. **Billing:** Operations → **Billing** — plan/status on the hotel; **Activate Starter (stub payment)** simulates subscription without Stripe Billing yet.
4. **Demo seed:** Five Myanmar properties with separate manager/desk logins (username + password per account in README / TESTING_GUIDE / TEST_CASES). Seeded hotels are marked **STARTER / ACTIVE**.

## Surfaces

- **Guest site** (`/`, `/hotels/:id`, `/book`) — public.
- **Operations** (`/login` → `/admin`) — JWT with `ADMIN` or `STAFF` + assigned property.
