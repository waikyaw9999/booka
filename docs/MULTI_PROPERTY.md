# Booka multi-property model

Booka is sold as a **shared guest marketplace** with **per-property staff tenancy**.

## Roles

| Role | Audience | Scope |
|------|----------|--------|
| *(anonymous / guest booker)* | Guest site | Browse and book any listed hotel |
| `GUEST` | Optional guest account | Same public booking APIs; no Operations access |
| `ADMIN` | Property manager | Full Operations for **one** `hotelId`; can invite STAFF |
| `STAFF` | Front desk / hotel team | Operations for **one** `hotelId` |

There is **no platform SUPER_ADMIN** in this release. New properties onboard themselves.

## Tenancy rules

- Every `ADMIN` / `STAFF` user has a required `users.hotel_id`.
- Admin APIs resolve the hotel from the authenticated user — never from a client-supplied “act as hotel X” override for ops reads/writes.
- Cross-property booking codes, folios, rooms, blackouts, and tickets are rejected with a business error.
- Guests still see **all** hotels on the public site.

## Onboarding

1. **Self-serve:** `POST /api/v1/auth/register-property` or UI `/register-property`  
   Creates a hotel + manager (`ADMIN`) bound to that hotel, returns a JWT, opens Operations.
2. **Invite team:** Manager uses Operations → **Team** (`POST /api/v1/admin/staff`) to create `STAFF` for the same hotel.
3. **Demo seed:** Five Myanmar properties with separate manager/desk logins (password `password`) — see README / TESTING_GUIDE.

## Surfaces

- **Guest site** (`/`, `/hotels/:id`, `/book`) — public.
- **Operations** (`/login` → `/admin`) — JWT with `ADMIN` or `STAFF` + assigned property.
