# Booka — Team Feature Test Cases

For a **step-by-step walkthrough** (ordered guest → desk → ops), use [`TESTING_GUIDE.md`](./TESTING_GUIDE.md). This file is the Pass/Fail checklist.

Use this pack for launch / regression testing. Mark each case **Pass / Fail / Blocked** and note environment (local `localhost:5173` or prod `https://booka.juneko.online`).

**Password for all demo staff accounts:** `password`

| Property | Manager | Front desk |
|----------|---------|------------|
| Booka Grand Hotel | admin@booka.com | staff@booka.com |
| Harbourview Suites | harbour.admin@booka.com | harbour.staff@booka.com |
| Inle Lake Lodge | inle.admin@booka.com | inle.staff@booka.com |
| Bagan Sandstone Inn | bagan.admin@booka.com | bagan.staff@booka.com |
| Mandalay Heritage House | mandalay.admin@booka.com | mandalay.staff@booka.com |

**Demo booking codes (seed):** `BK-DEMO-101` … `BK-DEMO-110`

---

## A. Guest site — discovery & booking

| ID | Feature | Steps | Expected |
|----|---------|-------|----------|
| G-01 | Home / catalog | Open guest home. Scroll to Featured stays. | At least several hotels (seed has 5) with images, city, short description. Layout looks light and readable. |
| G-02 | Hotel detail | Open any hotel card. | Hero/gallery images (or fallback), amenities, cancellation policy, availability form. |
| G-03 | Date validation | On hotel detail, set check-out ≤ check-in; try past check-in if allowed by UI. | Check-in min = today; check-out after check-in; invalid ranges blocked or auto-corrected. |
| G-04 | Availability search | Search a valid future range (e.g. +7 to +9 days), 2 guests. | Room types listed with nightly rate, **room subtotal** (not “total stay”), tax note, available count. |
| G-05 | Empty availability | Search dates on a known blackout (e.g. Harbourview Dec 10–13 2026 property-wide). | Friendly empty / no rooms message; no crash; no technical error text. |
| G-06 | Book flow | From availability, Book → enter guest name/email → Confirm. | Checkout shows room subtotal + 10% tax + **total due at property**. CTA is pay-at-property language (no Stripe/secure checkout promises). |
| G-07 | Confirmation | Complete booking. | Success screen with confirmation code, stay summary, “pay at property”. Code looks like `BK-…`. |
| G-08 | Pricing honesty | Note room subtotal on hotel page vs total on checkout. | Checkout total = subtotal × 1.10 (tax). Labels make this clear. |
| G-09 | Broken hotel link | Open `/hotels/99999`. | Friendly “not found” / load error — not a raw stack or “500”. |
| G-10 | Mobile guest | Resize to phone width: home, hotel, book. | Usable nav, forms, and CTAs; no horizontal overflow. |

---

## B. Guest concierge (RAG)

| ID | Feature | Steps | Expected |
|----|---------|-------|----------|
| R-01 | FAQ | Ask “What is the cancellation policy?” on home or hotel. | Grounded answer; citations when available. No inventing policies. |
| R-02 | Clarify dates | Ask “Any rooms available?” without dates. | Asks for dates / shows suggested date-range chips. |
| R-03 | Natural dates | Ask “available rooms for December 20 to 22”. | Uses **Dec 20 → Dec 22**, not a Dec 1–3 sample. |
| R-04 | Availability cards | After a dated availability question, click **View & book**. | Opens hotel with dates applied and availability searched (same-page deep link works). |
| R-05 | Friendly errors | Stop backend briefly and send a chat message (optional). | User-friendly message only — no “Internal Server Error” / exception dumps in the chat UI. |

---

## C. Staff auth & property scope

| ID | Feature | Steps | Expected |
|----|---------|-------|----------|
| S-01 | Staff login | `/login` as `staff@booka.com` / `password`. | Lands in Operations; sidebar shows **Booka Grand Hotel** (not only Property #1). |
| S-02 | Per-property login | Login as `harbour.staff@booka.com`. | Ops scoped to Harbourview; dashboard/rooms match that hotel. |
| S-03 | Manager login | Login as `admin@booka.com`. | Full ops access for Booka Grand. |
| S-04 | Guest vs staff | As guest (no login), open `/admin`. | Redirected to login. |
| S-05 | Demo credentials on login | Check login page in production build. | Demo emails/passwords **not** shown in prod; OK in local DEV only. |

---

## D. Operations — Dashboard & reservations

| ID | Feature | Steps | Expected |
|----|---------|-------|----------|
| D-01 | Dashboard pulse | Open Dashboard. | Occupancy / arrivals / departures / active bookings load; recent reservations visible. |
| D-02 | Reservations list | Open Reservations. | Seed + new bookings listed with code, guest, room, channel, pay, status. |
| D-03 | Status override | Change a booking status via Override dropdown. | Updates with note that FO is preferred for check-in/out. |
| D-04 | Walk-in | Reservations / walk-in form: create stay for today/tomorrow. | Creates CONFIRMED booking with confirmation code; appears in list. |

---

## E. Front Office

| ID | Feature | Steps | Expected |
|----|---------|-------|----------|
| FO-01 | Room board | Open Front Office. | Rooms show status (available/occupied/maintenance), HK status, guest when occupied; thumbnails if seeded. |
| FO-02 | Assign by code | Select an available room; assign with confirmation code (e.g. unassigned `BK-DEMO-104` or a new booking). | Accepts `BK-…` (not only numeric ID); room links to booking. |
| FO-03 | Check-in | Assigned CONFIRMED booking on CLEAN room → Check in. | Booking CHECKED_IN; room OCCUPIED. |
| FO-04 | Dirty check-in | Attempt check-in when HK ≠ CLEAN. | Blocked or requires force confirm. |
| FO-05 | Check-out | Check out occupied room. | Booking CHECKED_OUT; room AVAILABLE; HK becomes DIRTY. |

---

## F. Housekeeping & maintenance

| ID | Feature | Steps | Expected |
|----|---------|-------|----------|
| HK-01 | HK board | Open Housekeeping. | Rooms listed with CLEAN / DIRTY / INSPECTING. |
| HK-02 | Status flow | Move a DIRTY room → INSPECTING → CLEAN. | Transitions succeed; CLEAN normally after INSPECTING (force if offered). |
| M-01 | Create ticket | Maintenance → create ticket for a room (optional OOO). | Ticket appears; if mark OOO, room inventory MAINTENANCE. |
| M-02 | Resolve ticket | Resolve ticket; confirm room sellability restored when appropriate. | Status RESOLVED; room back AVAILABLE if designed that way. |

---

## G. Folios, blackouts, inventory images

| ID | Feature | Steps | Expected |
|----|---------|-------|----------|
| F-01 | Folio by code | Folios → search `BK-DEMO-101` (or new booking code). | Folio opens with room/tax lines. |
| F-02 | POS charge | Post an F&B charge with external ref. | Line appears; repeat same external ref is idempotent (no duplicate). |
| B-01 | List blackouts | Blackouts tab. | Existing seed blackouts listed with dates/reason/scope. |
| B-02 | Create blackout | Add a short future blackout (hotel-wide or room type). | Appears in list; guest availability for overlapping stay returns no rooms. |
| B-03 | Delete blackout | Delete a test blackout. | Removed; does **not** cancel existing bookings. |
| I-01 | Property images | Inventory → Property images: add https URL; set primary; delete. | Gallery updates; guest hotel card/detail shows primary. |
| I-02 | Room images | Inventory → Manage room images; add URL or upload. | Room board / room-type thumbs update when applicable. |
| I-03 | Amenities edit | Edit hotel or room-type amenities; save. | Values persist after refresh. |

---

## H. Staff assistant (RAG)

| ID | Feature | Steps | Expected |
|----|---------|-------|----------|
| SA-01 | SOP | Ask a check-in / housekeeping SOP question. | Staff-oriented answer from SOPs; no guest-only leakage of secrets. |
| SA-02 | Next month bookings | Ask “How many bookings next month?” | Confirms calendar month/date range before counts (or after yes). |
| SA-03 | Blackouts / summary | Ask about blackouts or today’s occupancy. | Uses live property data, not invented numbers. |

---

## I. Cross-cutting / launch smoke

| ID | Feature | Steps | Expected |
|----|---------|-------|----------|
| X-01 | End-to-end happy path | Guest books → note code → staff assigns room by code → check-in → post folio charge → check-out. | Full path works without technical errors in UI. |
| X-02 | Multi-property | Repeat FO smoke on a second property login (e.g. Harbourview). | Data stays scoped; no cross-hotel room bleed. |
| X-03 | Favicon & branding | Check browser tab icon and fonts (Sora/DM Sans). | Favicon loads; UI looks light commercial hospitality (not purple/generic). |
| X-04 | API down | Stop backend; reload guest home. | Friendly load error (not “port 8080” or stack traces). |

---

## Suggested test order (1–2 testers, ~90 minutes)

1. **G-01 → G-07** (guest book)  
2. **R-02 → R-04** (concierge dates)  
3. **S-01 → FO-05** (desk)  
4. **HK-01 → F-02** (ops depth)  
5. **I-01, B-02, X-01** (launch confidence)

## Bug report template

```
Case ID:
Environment: local / prod
Account used:
Steps:
Expected:
Actual:
Screenshot / confirmation code:
```
