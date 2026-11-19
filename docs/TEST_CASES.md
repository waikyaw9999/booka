# Booka — Team Feature Test Cases

For a **step-by-step walkthrough** (ordered guest → desk → ops), use [`TESTING_GUIDE.md`](./TESTING_GUIDE.md). This file is the Pass/Fail checklist.

Use this pack for launch / regression testing. Mark each case **Pass / Fail / Blocked** and note environment (local `localhost:5173` or prod `https://booka.juneko.online`).

### Demo accounts (username + password)

Every seeded property has its own manager and front-desk login. Ops data is scoped by hotel — staff from one property cannot open another’s bookings/folios/rooms.

| Property | Role | Username | Password |
|----------|------|----------|----------|
| Booka Grand Hotel | Manager | `admin@booka.com` | `password` |
| Booka Grand Hotel | Front desk | `staff@booka.com` | `password` |
| Harbourview Suites | Manager | `harbour.admin@booka.com` | `password` |
| Harbourview Suites | Front desk | `harbour.staff@booka.com` | `password` |
| Inle Lake Lodge | Manager | `inle.admin@booka.com` | `password` |
| Inle Lake Lodge | Front desk | `inle.staff@booka.com` | `password` |
| Bagan Sandstone Inn | Manager | `bagan.admin@booka.com` | `password` |
| Bagan Sandstone Inn | Front desk | `bagan.staff@booka.com` | `password` |
| Mandalay Heritage House | Manager | `mandalay.admin@booka.com` | `password` |
| Mandalay Heritage House | Front desk | `mandalay.staff@booka.com` | `password` |

**Demo booking codes (seed):** `BK-DEMO-101` … `BK-DEMO-110`

---

## A. Guest site — Agoda-style search → book

Flow under test: **(1) Search → (2) available properties → (3) rooms → (4) room detail → book**.

| ID | Feature | Steps | Expected |
|----|---------|-------|----------|
| G-01 | Home search (step 1) | Open guest home `/`. | Hero shows **Booka** + stay search bar (destination, check-in, check-out, guests, Search). Optional featured properties below. |
| G-02 | Search results (step 2) | Enter city (optional), dates today+7 → today+9, 2 guests → Search. | Lands on `/search` with **Available stays**; only properties with sellable inventory for those dates; each row shows from/night, stay from, room-type count. |
| G-03 | Date validation | On home or `/search`, set check-out ≤ check-in; try past check-in. | Check-in min = today; check-out auto-bumped after check-in; invalid submit blocked. |
| G-04 | Empty property search | Search Harbourview-focused dates that hit the Dec 10–13 2026 blackout (or filter city so only blacked-out inventory applies). | Friendly “No properties for these dates” (or empty list) — no crash / stack / “500”. |
| G-05 | Property rooms (step 3) | From a search hit, open **View rooms** (or `/hotels/:id` with stay query). | Step 3 rooms page: property hero, stay bar, **Available rooms** with rates, counts, **View room** CTA. Dates from search preserved in URL. |
| G-06 | Change dates on property | On rooms page, change dates in the stay bar and search again. | Availability refreshes for the new range without leaving the property. |
| G-07 | Room detail (step 4) | Click **View room** on a room type. | `/hotels/:id/rooms/:roomTypeId` shows image, amenities, stay summary, room subtotal, **Continue to book**. |
| G-08 | Book flow | From room detail → Continue to book → enter guest name/email → Confirm. | Checkout shows room subtotal + 10% tax + **total due at property**. CTA is pay-at-property language (no Stripe promises). |
| G-09 | Confirmation | Complete booking. | Success with confirmation code `BK-…`, stay summary, “pay at property”. |
| G-10 | Pricing honesty | Note room subtotal on room detail vs total on checkout. | Checkout total = subtotal × 1.10 (tax). Labels make this clear. |
| G-11 | Browse without dates | From home, open a featured property card (no dates). | Rooms page loads; prompts to choose dates before listing priced rooms. |
| G-12 | Broken hotel link | Open `/hotels/99999`. | Friendly “not found” — not a raw stack or “500”. |
| G-13 | Mobile guest | Phone width: home search → results → rooms → room detail → book. | Usable nav/forms/CTAs; no horizontal overflow. |

---

## B. Guest concierge (RAG)

| ID | Feature | Steps | Expected |
|----|---------|-------|----------|
| R-01 | FAQ | Ask “What is the cancellation policy?” on home or hotel. | Grounded answer; citations when available. No inventing policies. |
| R-02 | Clarify dates | Ask “Any rooms available?” without dates. | Asks for dates / shows suggested date-range chips. |
| R-03 | Natural dates | Ask “available rooms for December 20 to 22”. | Uses **Dec 20 → Dec 22**, not a Dec 1–3 sample. |
| R-04 | Availability cards | After a dated availability question, click **View & book**. | Opens property rooms (or detail) with dates applied and availability searched. |
| R-05 | Friendly errors | Stop backend briefly and send a chat message (optional). | User-friendly message only — no “Internal Server Error” / exception dumps in the chat UI. |

---

## C. Staff auth & property scope

| ID | Feature | Steps | Expected |
|----|---------|-------|----------|
| S-01 | Staff login | `/login` as `staff@booka.com` / `password`. | Lands in Operations; sidebar shows **Booka Grand Hotel** (not only Property #1). |
| S-02 | Per-property login | Login as `harbour.staff@booka.com`. | Ops scoped to Harbourview; dashboard/rooms match that hotel; cannot load another property’s `BK-DEMO-*` by code. |
| S-06 | Register property | `/register-property` with a unique manager email. | Creates hotel + ADMIN; starter Standard/Deluxe rooms; TRIAL billing; hotel on guest site. |
| S-07 | Invite staff | As manager, Team tab → invite front desk. | New STAFF login works; sees same hotel only. |
| S-08 | Deactivate / reset | Deactivate desk user; try login; reset password; reactivate. | Inactive cannot sign in; new password works. |
| S-09 | Billing stub | Billing tab → Activate Starter. | Plan STARTER / status ACTIVE. |
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
| X-01 | End-to-end happy path | Guest: search → property → room → detail → book → note code → staff assigns by code → check-in → folio charge → check-out. | Full path works without technical errors in UI. |
| X-02 | Multi-property | Repeat FO smoke on a second property login (e.g. Harbourview). | Data stays scoped; no cross-hotel room bleed. |
| X-03 | Favicon & branding | Check browser tab icon and fonts (Sora/DM Sans). | Favicon loads; UI looks light commercial hospitality (not purple/generic). |
| X-04 | API down | Stop backend; reload guest home. | Friendly load error (not “port 8080” or stack traces). |

---

## Suggested test order (1–2 testers, ~90 minutes)

1. **G-01 → G-09** (guest Agoda flow: search → properties → rooms → detail → book)  
2. **R-02 → R-04** (concierge dates)  
3. **S-01 → FO-05** (desk — use credentials table above)  
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
