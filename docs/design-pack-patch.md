# Design pack v2: PATCH (this file overrides docs/design-pack.md wherever they conflict)

Read docs/design-pack.md first, then this patch. If both say something different, the patch wins.

## P1. Pricing (replaces section 5, steps 2 and 4)

ASSUMPTION: one GST rate per booking, not per night.
1. For each night, compute NightlyPlanTariff = base rate + rate plan (unchanged).
2. PreTaxSubtotal = rooms_count x sum of NightlyPlanTariff.
3. AverageNightlyTariff = PreTaxSubtotal / rooms_count / nights.
4. GST slab lookup: ONE lookup, using check_in_date and AverageNightlyTariff.
5. Promo is applied to PreTaxSubtotal (unchanged): DiscountedPreTax = PreTaxSubtotal - Discount.
6. TotalGST = DiscountedPreTax x slab rate / 100. Round HALF_UP to 2 decimals once, at the end.
7. GrandTotal = DiscountedPreTax + TotalGST.
Remove the per-night GST formula and the proportional discount formula.
Test 10 becomes: "a stay that crosses a slab effective-date boundary uses the slab effective on the check-in date".
Label this an ASSUMPTION, flagged "verify before launch".

## P2. GST seed values and display

- gst_tax_slabs is seeded from docs/gst-source.md, which the developer writes by hand
  (rates, tariff cut-offs, source link, date checked). Do not guess any value.
  Until that file exists, seed 0.00 with verification_status = UNVERIFIED.
- Admin UI: loud banner while any slab is UNVERIFIED.
- Guest UI: a small neutral note while UNVERIFIED ("Taxes shown are indicative in this test build").
  No red warning on the guest checkout.

## P3. Booking transaction hardening (adds to section 4)

1. Run the booking transaction at READ COMMITTED isolation. Keep the ordered
   SELECT ... FOR UPDATE on daily_inventory_rates.
2. Retry up to 3 times, with a short backoff, on deadlock or lock-timeout
   (DeadlockLoserDataAccessException, CannotAcquireLockException). After that, return HTTP 503 "Please try again".
3. Idempotency: unique index on idempotency_key. If a duplicate-key error happens
   (two requests at once), catch it and return the existing booking.
   Same key with a different payload returns HTTP 422.
4. Do the active-hold cap check INSIDE the transaction, after the inventory locks.
   Document the small remaining race (two simultaneous requests from one email) as a known limit.
5. Payment webhook and the expiry job both do SELECT ... FOR UPDATE on the booking row first,
   re-check its status, and only then lock the inventory rows in ascending date order.
   The expiry job handles at most 100 bookings per run and skips locked rows (SKIP LOCKED).
6. New endpoint: POST /api/v1/public/bookings/{bookingRef}/cancel-hold. Requires the payment token.
   Releases the allotment immediately; status CANCELLED. This stops a reviewer from being blocked
   by the 2-hold cap after abandoning a checkout.
7. Rate limiting reads the client IP from X-Forwarded-For (configured for the host's proxy),
   and the limits come from environment variables. Without this, every user behind the host's proxy shares one IP.
8. The concurrency test must NOT be annotated @Transactional. It collects unexpected exceptions
   into a list and asserts that the list is empty. It needs a real MySQL (Testcontainers needs Docker; otherwise use a local test schema).

## P4. AI router adjustments (adds to section 6)

1. Before calling Gemini, scrub phone-number and email patterns out of raw_message (replace with [REDACTED]).
   Guests often type their number in the message.
2. Sanitizer regex: replace the trailing \b after currency symbols with (?!\w), because \b does not
   match after the rupee symbol. Test with "100 ₹".
3. The prompt-injection test mocks the AI so that it OBEYS the injection (its draft contains "1 rupee"),
   and asserts that the final draft contains only rate-table prices.
4. Model name and temperature (0) come from environment variables or config.

## P5. Seed data adjustments (adds to section 8)

1. Flyway holds the schema and static reference data only (categories, physical units, rate plans,
   blackouts, promo codes, the GST slab).
2. A Java DemoDataSeeder (idempotent, runs under a "demo" profile) creates: the 4 staff users
   (using PasswordEncoder, so no hand-typed BCrypt hashes), the 181 days of rates and allotments, and the
   25 enquiries with drafts built by QuoteInjectorService from the rate table. The AI is not called.
3. MySQL 8.0.16 or newer is required (CHECK constraints). State this in deployment.md.
4. Photos: free-licence images (Unsplash or Pexels), WebP, each under about 150 KB, credited in the TDD.

## P6. Extra tests (append to section 7; numbering continues from 19)

| # | Test | Expected |
|---|------|----------|
| 20 | Same Idempotency-Key sent twice | Same booking returned; booked_count incremented once |
| 21 | Same key, different payload | HTTP 422 |
| 22 | Third active hold with the same email | HTTP 429 (MaxActiveHoldsExceeded) |
| 23 | Overlapping-range concurrency: 10 threads book 14-16 Nov, 10 threads book 15-17 Nov, allotment 2 on each date | booked_count never exceeds 2 on any date; no unhandled exceptions |
| 24 | cancel-hold with the valid token | Allotment released; status CANCELLED; token cannot be reused |
| 25 | Public booking response | Phone and email masked; full email never appears; unknown ref returns 404 |
| 26 | Guest calls any /admin/** endpoint | HTTP 401; staff token required |
| 27 | Dates outside 1 Nov 2026 - 30 Apr 2027 | HTTP 400 with a clear message |
| 28 | Sixth booking request within a minute from one IP | HTTP 429 |

## P7. Small UI additions

1. Room detail page /rooms/:code: photo gallery, rate plans, availability. (The brief says "room pages".)
2. Step 3 shows a hold countdown (10 minutes) and a "Cancel hold" button.
3. Staff inbox: a visible GM banner when a COMPLAINT enquiry is waiting.
4. The wedding enquiry form has optional structured fields: event date, guest count, budget.
5. Optional depth: GET /api/v1/admin/bookings/export?date=YYYY-MM-DD returning CSV (the "daily booking export").