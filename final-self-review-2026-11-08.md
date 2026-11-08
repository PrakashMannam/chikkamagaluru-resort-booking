# Independent product self-review — 8 November 2026

**Verdict: roughly 92% complete by functional readiness, not 100%.** The core guest booking and staff triage workflows are present, and candidate branch `audit/final-hardening` now has additional fixes and passing frontend checks. Important release gates remain: the production build still shows the fixed November dates, the reported payment-hold defect has not been reproduced or verified against a deployed fix, and MySQL-backed concurrency has not been tested. This percentage is a judgment, not a formal acceptance score.

The source baseline reviewed was repository `main` at `2c7dc65`; the candidate changes are pushed in [PR #11][7] but are not merged into `main` or verified in production. The supplied deployment report records Railway commit `3ea8538` and schema v8, but the public health endpoint does not identify the deployed source revision. No production booking, enquiry, payment, inventory, migration, or other data write was made.

## What is now improved in the candidate

The Stay page no longer silently chooses a fixed November 2026 range. With no query dates, it presents empty date fields, explains that dates are needed, and labels the first action **Check Availability**. Checkout and room-detail routes now reject invalid or missing date ranges rather than restoring stale defaults. Staff inventory and CSV date presets use rolling dates. Relevant frontend coverage is in `frontend/test/date_selection_and_cta.test.mjs` and `frontend/test/hold_expiry_regression.test.mjs`.

The payment UI now distinguishes an explicit expired-hold response from an unrelated HTTP 409 conflict. Server-expiry parsing covers UTC, offset-bearing, timezone-less, malformed, and epoch values. The reported live “hold expired within a minute” defect was not reproduced on production because that would require creating a booking. Treat the change in `frontend/src/pages/GuestPaymentPage.jsx` and `frontend/src/utils/dateUtils.js` as a candidate fix until a non-production end-to-end payment test and deployment check pass.

The candidate also adds a keyboard-accessible compact navigation, removes the duplicate shared header action at relevant routes and narrow widths, adds an accessible label to the guest selector, and neutralizes the offsite meeting-space default. Backend hardening removes a cancellation-token URL channel, adds an in-process staff-login rate limit, expands PII redaction, and strengthens AI draft guardrails. Regression coverage is in the corresponding backend test files. The front-end build toolchain was updated to Vite 7.3.7 and React plugin 5.2.0; the saved dependency evidence records the advisory and compatibility checks in `audit-evidence-2026-11-08.md`.

## Findings and remaining gates

### High — the fresh-hold expiry fix is not live-verified

**Evidence:** `frontend/src/pages/GuestPaymentPage.jsx`, `frontend/src/utils/dateUtils.js`, and `frontend/test/hold_expiry_regression.test.mjs`. The regression tests cover timestamp parsing and ensure a generic conflict is not labeled as expiry. The deployed source revision is not confirmed.

**Reproduction:** A safe live reproduction would need to create a booking hold and invoke the simulated payment flow. I did not do this because it writes production data. Re-run the complete booking journey in an isolated staging database after the candidate is merged and deployed; verify that a fresh hold remains payable and that a truly expired hold is rejected.

**Impact:** Until this candidate is released and verified, guests may still encounter the reported false-expiry message. Do not close this issue based only on unit tests.

### Medium — production Stay still uses fixed dates

**Evidence:** Opening the live route `https://chikkamagaluru-resort-booking.vercel.app/stay` without a query displayed check-in **14 Nov 2026**, check-out **16 Nov 2026**, and an automatic availability-loading state. The local candidate instead showed blank date inputs and the explicit date-first state. No search was submitted in either check.

**Reproduction:** Open the production `/stay` route with no query parameters and observe the date fields. Compare with the same route in the local candidate. The changed files are `frontend/src/pages/GuestAvailabilityPage.jsx`, `frontend/src/utils/dateUtils.js`, `frontend/src/pages/GuestCheckoutPage.jsx`, and `frontend/src/pages/RoomDetailPage.jsx`.

**Impact:** The production experience can present a stale stay range and imply a search the guest did not request. Merge and deploy the candidate, then verify the live page again.

### High release gate — zero-overbooking behavior is not proven against MySQL

**Evidence:** `backend/src/main/java/com/companya/resort/service/BookingTransactionService.java` uses database locking, but the audit environment had no `TEST_DB_URL`. The backend suite therefore did not exercise MySQL migration behavior or concurrent inventory transactions. The assignment explicitly calls allotment control the hard part (`docs/isocode-assignment-brief.md`, “Core Build”).

**Reproduction:** Configure a dedicated, disposable MySQL 8 test database using `TEST_DB_URL`, `TEST_DB_USER`, and `TEST_DB_PASSWORD`; run the backend Maven test suite from `backend/`; then run concurrent hold attempts against a deliberately small allotment and assert that confirmed plus active holds never exceed it. Never point this test at production.

**Impact:** The design is present, but “zero overbooking” remains **NOT VERIFIED** until the database-backed concurrency path passes.

### High, conditional — verify demo staff credentials are not active in production

**Evidence:** Demo identities and credentials are documented in `deployment.md` and seeded by `backend/src/main/java/com/companya/resort/seeder/DemoDataSeeder.java`. The Railway runtime profile and active production staff-account state were not inspected in this read-only audit.

**Reproduction:** Privately verify the Railway active profile and account state. If the demo profile or its public sample credentials are enabled in production, disable or rotate them before treating the service as a real staff system. Do not test the known demo credential against a production login.

**Impact:** This is a conditional exposure, not a confirmed production compromise. It must be resolved before commercial use.

### Medium — AI safeguards reduce risk but cannot guarantee every generated claim

**Evidence:** `backend/src/main/java/com/companya/resort/service/GeminiAiService.java`, `backend/src/main/java/com/companya/resort/util/DraftPriceSanitizer.java`, and `backend/src/main/java/com/companya/resort/util/PiiRedactor.java`, with tests under `backend/src/test/java/com/companya/resort/`. The sanitizer and prompt are defense-in-depth. This audit did not send a new request to Gemini or verify live model output.

**Reproduction:** In a non-production environment, submit a synthetic offsite enquiry requesting amenities not confirmed in `backend/src/main/resources/db/migration/V3__reference_data.sql`, such as Wi-Fi or AV. Review the generated draft and confirm it defers unknown details to staff. Also verify complaint and low-confidence routing. Staff approval must remain required before dispatch.

**Impact:** AI routing and drafting are implemented, but no prompt or sanitizer can guarantee all free-form model output. Live model behavior remains **NOT VERIFIED** in this run.

### Medium — rates and property copy still need owner confirmation

**Evidence:** `backend/src/main/resources/db/migration/V3__reference_data.sql` contains seeded room names, descriptions, capacities, amenities, and demo tariffs. `V4__gst_slabs.sql` marks tax slabs as `UNVERIFIED`. These limits are recorded in the TDD and deployment guide.

**Reproduction:** Have resort management confirm the sellable inventory, room capacities, amenities, tariff plans, blackout dates, and booking window. Have a qualified tax adviser validate the tax calculation before using it commercially. Do not present seeded/demo values as independently verified property facts or statutory tax advice.

**Impact:** These are data and legal-validation gates, not evidence of a calculation bug. Tax figures are provisional; the documents correctly avoid certifying DPDP or tax compliance.

### Low — responsive verification is broader, but not complete

**Evidence:** The local built preview was measured in same-origin browser frames at 390px and 768px for `/`, `/stay`, `/enquire?type=offsite`, and `/staff/login`. Every measured frame had equal document `clientWidth` and `scrollWidth`: 375/375px at 390px, and 753/753px on routes with a vertical scrollbar or 768/768px on staff login at 768px. The mobile menu opened and closed with the expected `aria-expanded` state.

**Reproduction:** Open the listed routes in a browser at both widths and compare the page’s client and scroll widths. No enquiry or login was submitted. The remaining guest checkout, payment, confirmation, room-detail, and staff-operation pages, 1280px desktop layouts, keyboard-only navigation, and actual-device tap targets still need explicit checks. Evidence is summarized in Section 8 of `TDD.md`.

**Impact:** No overflow was observed on the measured routes. This is not a claim that every route or control is responsive or accessible.

## Scope check against the brief

The tracked brief identifies guest booking, reservations staff plus GM roles, rate/allotment administration, a centralized enquiry inbox, AI classification and reply drafts, complaint and low-confidence GM routing, simulated payments/messages, and inventory control as the core build. The code contains those principal flows; the evidence-based status matrix is in Section 2 of `TDD.md`.

A dedicated GM financial analytics dashboard is listed as a future roadmap item in Section 7 of `TDD.md`, not a required Core Build screen in the tracked brief. A guest login is not listed as a Core Build requirement. A production card/UPI gateway, PMS integration, and real WhatsApp/email delivery are also not required; the brief permits mocked external systems. Do not spend time adding those items to claim completion while the release and database-verification gates above remain open.

## Verification run

The local frontend suite passed **25/25** tests, the production frontend build succeeded, and `npm audit --audit-level=moderate` reported zero known vulnerabilities. The backend unit/mock suite passed **238 tests** with no reported failures, errors, or skips; MySQL-backed tests were not run because `TEST_DB_URL` was not configured. `git diff --check` passed. These are local candidate results, not production test results. Detailed commands and scope are in `deployment.md` Section 7.

## Completion estimate and next release gates

I estimate the project at **about 92% overall**. The local candidate is stronger than the deployed experience, but the estimate cannot reach 100% until the reported expiry defect is validated in staging, the date-first Stay fix is merged and visibly deployed, and the MySQL concurrency tests pass. Before any commercial launch, also verify demo credentials, confirm property/rate/tax data with the owner and tax adviser, and complete responsive and accessibility checks on the remaining routes. The exact production source commit is still **NOT VERIFIED**.

## References

[1]: https://github.com/PrakashMannam/chikkamagaluru-resort-booking/tree/2c7dc65 "Repository main snapshot reviewed"
[2]: https://github.com/PrakashMannam/chikkamagaluru-resort-booking/blob/2c7dc65/docs/isocode-assignment-brief.md "Tracked IsoCode Labs assignment brief"
[3]: https://chikkamagaluru-resort-booking.vercel.app/stay "Production Stay page inspected without submitting a search"
[4]: https://resort-backend-production.up.railway.app/health "Read-only Railway health endpoint"
[5]: https://github.com/advisories/GHSA-4w7w-66w2-5vf9 "Vite security advisory reviewed for dependency remediation"
[6]: https://vite.dev/releases "Official Vite release support information reviewed"

[7]: https://github.com/PrakashMannam/chikkamagaluru-resort-booking/pull/11 "Pull request for the audit candidate"
