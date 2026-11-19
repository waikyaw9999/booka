# Booka — Hands-on Testing Guide

Step-by-step walkthrough for the team. Follow the sections in order for a full guest → front desk → ops path (~60–90 minutes). For a Pass/Fail checklist, see [`TEST_CASES.md`](./TEST_CASES.md).

---

## 1. Prerequisites

### Stack

1. From the repo root:
   ```bash
   cp .env.example .env   # if you do not already have .env
   docker compose up --build
   ```
2. Wait until Postgres, backend (Liquibase), and frontend are healthy.
3. Open:
   - **Guest site:** http://localhost:5173  
   - **Staff login:** http://localhost:5173/login  
   - **API (optional):** http://localhost:8080/actuator/health (or any known public API)

Production (if testing there): `https://booka.juneko.online` — same flows; demo password hints should **not** appear on the login page.

### Migrations you should have

Liquibase runs on backend start. Confirm the DB includes at least:

| Change | What it gives you |
|--------|-------------------|
| `004` | Hotel / room image tables |
| `005`–`006` | Commercial copy + 5 Myanmar properties, rooms, images, blackouts, `BK-DEMO-*` |
| `007` | Commercial ops columns (guest VIP fields, night audit tables, etc.) |
| `008` | Per-property manager / front-desk logins |

If image or inventory APIs return 500 after a schema change, rebuild the backend image so Liquibase re-applies (do not rely on an old jar).

### Optional

- `GROQ_API_KEY` in `.env` — richer concierge answers. Without it, FAQ/availability still run from documents + inventory tools; answers may be thinner.
- Stripe keys are **not** required for guest booking (pay at property).

### Demo accounts

**Password for every demo user:** `password`

| Property | Manager | Front desk |
|----------|---------|------------|
| Booka Grand Hotel | `admin@booka.com` | `staff@booka.com` |
| Harbourview Suites | `harbour.admin@booka.com` | `harbour.staff@booka.com` |
| Inle Lake Lodge | `inle.admin@booka.com` | `inle.staff@booka.com` |
| Bagan Sandstone Inn | `bagan.admin@booka.com` | `bagan.staff@booka.com` |
| Mandalay Heritage House | `mandalay.admin@booka.com` | `mandalay.staff@booka.com` |

### Handy seed data

| Item | Value |
|------|--------|
| Unassigned booking (assign practice) | `BK-DEMO-104` — Harbourview Suites, Dec 5–8 2026, no room yet |
| Folio practice | `BK-DEMO-101` — Booka Grand |
| Property-wide blackout | Harbourview **2026-12-10 → 2026-12-13** (festival) |
| Another blackout | Bagan **2026-12-20 → 2026-12-22** (wedding buyout) |
| Concierge date phrasing | “December 20 to 22” (must resolve to Dec 20–22, not Dec 1–3) |

Keep a notepad: you will write down a **new** confirmation code from the guest booking walkthrough and reuse it at the desk.

---

## 2. Guest walkthrough — discover and book

**Goal:** Browse a hotel, search dates, confirm a stay with pay-at-property (no Stripe redirect).

### 2.1 Home

1. Open the guest home URL.
2. Confirm the brand **Booka** reads as the hero-level identity (fonts: Sora / DM Sans; light hospitality palette).
3. Scroll to featured stays.

**Expect:** About **five** properties (Booka Grand, Harbourview, Inle, Bagan, Mandalay) with images (or Unsplash fallback), city, and short copy. Browser tab shows the Booka favicon.

### 2.2 Hotel detail

1. Open **Booka Grand Hotel** (or any property).
2. Scan hero / gallery, amenities, and cancellation policy.
3. Find the availability form (check-in, check-out, guests).

**Expect:** Gallery or fallback imagery; form present; no raw technical errors.

### 2.3 Dates and availability

1. Set check-in to **today + 7 days**, check-out to **today + 9 days**, guests = **2**.
2. Run search.

**Expect:** Room types with nightly rate, **room subtotal** for the stay, tax note, and available count. Labels should not claim “online payment complete.”

**Optional negative check:** On **Harbourview Suites**, search check-in **2026-12-10**, check-out **2026-12-13**.

**Expect:** Friendly empty / no rooms message (seed blackout). No stack traces or “500” text.

### 2.4 Checkout (pay at property)

1. Choose a room type → **Book** / continue.
2. Enter guest name and email (use a test address).
3. Review the stay summary before confirming.

**Expect:**

- Room subtotal  
- Tax (~10%)  
- **Total due at property**  
- CTA language like “Confirm now · pay at property” (not Stripe / “secure checkout” promises)

4. Confirm the booking.

**Expect:** Success screen with a confirmation code like `BK-…`, stay dates, and pay-at-property messaging.

5. **Write down the confirmation code** — you need it in §4.

### 2.5 Quick negative (optional)

- Open `/hotels/99999` → friendly not-found, not a stack dump.
- Shrink the browser to phone width on home → hotel → book → usable layout.

---

## 3. Guest concierge (RAG)

**Goal:** Prove FAQ grounding, date clarification, natural-language stay dates, and **View & book**.

Use the chat widget on home (or hotel) with `GROQ_API_KEY` set if you want richer wording.

### 3.1 FAQ

Ask: *What is the cancellation policy?*

**Expect:** Answer grounded in hotel/FAQ content; citations when the UI shows them. It should not invent a policy that contradicts the hotel page.

### 3.2 Missing dates

Ask: *Any rooms available?*

**Expect:** Asks for check-in/out (and guests), and/or shows suggested upcoming date-range chips you can click.

### 3.3 Exact natural dates

Ask: *Are there available rooms for December 20 to 22?*

**Expect:** The assistant uses **December 20 → December 22** (or 2026-12-20 → 2026-12-22), **not** a December 1–3 sample range.  
Note: Bagan is blacked out those nights; other hotels may still show options. Focus on the **dates in the reply/cards**, not only “sold out.”

### 3.4 View & book deep link

1. After a dated availability answer with room cards, click **View & book**.
2. Confirm you land on the hotel page with those dates applied and availability searched (or search starts automatically).

**Expect:** Navigation works; dates match the chat; you can continue into booking if inventory exists.

---

## 4. Staff login and property scope

**Goal:** Reach Operations for the right hotel.

1. Open `/login`.
2. Sign in as `staff@booka.com` / `password`.

**Expect:** Operations shell; sidebar property name **Booka Grand Hotel** (not only “Property #1”). Menu includes Dashboard, Front Office, Inventory, Reservations, Housekeeping, Maintenance, Folios, Blackouts, Assistant (labels may vary slightly).

3. Log out. Sign in as `harbour.staff@booka.com` / `password`.

**Expect:** Harbourview-scoped rooms and reservations only.

4. Log out again. Sign in as `staff@booka.com` for the front-desk path below (or stay on Harbourview if you will assign `BK-DEMO-104`).

**Optional:** While logged out, open `/admin` → redirected to login.

---

## 5. Front Office — assign, check-in, check-out

**Goal:** Use a confirmation code (not only numeric IDs) through a desk stay.

Two good paths (pick one):

| Path | Login | Booking to assign |
|------|--------|-------------------|
| **A — your new guest booking** | `staff@booka.com` if you booked Booka Grand | Code from §2.4 |
| **B — seed unassigned** | `harbour.staff@booka.com` | `BK-DEMO-104` (Dec 5–8 2026, room type suite) |

### 5.1 Room board

1. Open **Front Office**.
2. Scan the room grid: inventory status, housekeeping status, guest when occupied, thumbnails if present.

### 5.2 Assign by confirmation code

1. Select an **available** room that matches the booking’s room type (or any allowed assign target for that type).
2. Assign using the confirmation code (`BK-…` / `BK-DEMO-104`).

**Expect:** Assignment succeeds with the code string; board shows the guest/booking on that room. Do not require only a numeric booking id.

### 5.3 Check-in

1. Ensure housekeeping is **CLEAN** (use Housekeeping tab first if needed — see §6).
2. Check in the assigned **CONFIRMED** booking.

**Expect:** Booking → **CHECKED_IN**; room inventory → **OCCUPIED**.

**Optional:** If the room is DIRTY, try check-in → blocked or force-confirm prompt.

### 5.4 Check-out

1. Check out the same room/booking.

**Expect:** Booking → **CHECKED_OUT**; room → **AVAILABLE**; housekeeping → **DIRTY**.

---

## 6. Housekeeping and maintenance

Still logged into the same property.

### 6.1 Housekeeping flow

1. Open **Housekeeping**.
2. Find the room you just checked out (should be **DIRTY**).
3. Move **DIRTY → INSPECTING → CLEAN**.

**Expect:** Each transition saves; CLEAN normally after INSPECTING (force only if the UI offers it).

### 6.2 Maintenance (optional)

1. Open **Maintenance**.
2. Create a ticket for a room; optionally mark out of order.
3. Resolve the ticket when done.

**Expect:** Ticket listed; OOO sets room to **MAINTENANCE**; resolve restores **AVAILABLE** when designed to.

---

## 7. Folios, blackouts, inventory images

### 7.1 Folio + POS charge

1. Open **Folios**.
2. Search `BK-DEMO-101` (Booka Grand login) or your new booking code.
3. Confirm room + tax lines exist.
4. Post a small F&B / POS charge with an `externalRef` such as `TEST-POS-001`.
5. Submit the **same** external ref again.

**Expect:** First post adds a line; second post does not duplicate (idempotent).

### 7.2 Blackouts

1. Open **Blackouts** (Harbourview login to see the festival blackout, or Booka Grand to create a short test range).
2. Create a short **future** blackout (hotel-wide or one room type), note the dates.
3. On the guest site, search that hotel for overlapping nights.

**Expect:** Guest search shows no sellable inventory for the blacked-out scope. Deleting the blackout does **not** cancel existing bookings.

### 7.3 Inventory images

1. Open **Inventory**.
2. Add a property image via https URL (any public hotel photo URL); set primary if offered.
3. Optionally add a room image (URL or file upload).
4. Refresh the guest hotel page.

**Expect:** Primary/gallery updates on the guest site; room thumbs may appear on FO / room types.

---

## 8. Staff assistant (optional)

1. Open **Assistant** in Operations.
2. Ask: *What is the check-in SOP if the room is still dirty?*
3. Ask: *How many bookings next month?* — confirm the month if prompted.
4. Ask: *What blackouts do we have?* or *What is today’s occupancy?*

**Expect:** SOP-style answers for procedures; live counts/blackouts from property APIs (not invented). Guests must never see staff SOP content via the public chat.

---

## 9. End-to-end smoke (recap)

Run this once as a single story:

1. Guest books pay-at-property → copy `BK-…`.
2. Staff logs into that property → Front Office → assign by code → check-in (CLEAN room).
3. Folios → post one POS charge.
4. Check-out → Housekeeping DIRTY → INSPECTING → CLEAN.

**Expect:** No technical error dumps in the UI; statuses match §5–§7.

**Multi-property check:** Repeat assign/check-in once on Harbourview with `BK-DEMO-104` (if still unassigned) and confirm Booka Grand rooms do not appear.

---

## 10. How to report issues

```
Guide section / case ID:
Environment: local / prod
Account used:
Confirmation code (if any):
Steps:
Expected:
Actual:
Screenshot:
```

Cross-reference Pass/Fail IDs in [`TEST_CASES.md`](./TEST_CASES.md) (e.g. G-07, FO-02, R-03).

---

## Suggested session plan

| Block | Time | Sections |
|-------|------|----------|
| Guest book | 20 min | §2 |
| Concierge | 10 min | §3 |
| Desk + HK | 25 min | §4–§6 |
| Folio / blackout / images | 15 min | §7 |
| E2E recap / second property | 10–20 min | §9 |
