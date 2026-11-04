# Frontend rules (Antigravity must read this before every frontend task)

SCOPE
- Work only inside /frontend. Never edit /backend, migrations or /docs
  (except where a task names a file). Never commit. Never push.
- One screen or one feature per task. Stop when it is done.
- Do not add dependencies without asking. Allowed: react-router-dom,
  react-day-picker, date-fns. NOT allowed: Tailwind, any UI library,
  three.js, any animation library, any state library.

HONESTY
- Never say a screen works unless you ran it. Run npm run build after every task
  and show the last 5 lines.
- If an API field you need does not exist, STOP and tell me. Never invent
  a field or an endpoint. Read the controllers and DTOs in /backend to
  see the real shapes.
- No fake or mock data in real screens. Mock data is allowed only in /styleguide.
- Every button must do something real or not exist. No dead buttons.

API
- Base URL comes from VITE_API_URL (add the NAME to .env.example, no value).
- Money: display exactly the strings or numbers the server returns.
  Never calculate prices, GST or discounts in the browser.
- Dates are plain "YYYY-MM-DD" strings. Never build dates with new Date("2026-11-14")
  (it parses as UTC and can shift a day). Use a small helper that parses
  the parts as local dates. Booking window in the picker: 2026-11-02
  to 2027-04-30; the server stays the judge.
- POST /public/bookings: create one Idempotency-Key (crypto.randomUUID())
  per booking attempt. Reuse it on a retry or double-click with the same
  form. Make a new key only if the guest changes any booking field.
- The oneTimePaymentToken is returned once. Keep it in React state and
  sessionStorage under the booking ref. Never put it in a URL, never log it.
- The booking confirmation page uses GET /public/bookings/{ref} and shows
  only the masked phone and email the server returns.
- Show a 10-minute hold countdown from expiresAt. When it hits zero, show the
  expired state and a button to start again. Offer "Cancel hold".
- Map errors to calm messages: 409 "just taken" or conflict, 429 "please
  wait a minute" (use Retry-After), 422 invalid promo or mismatch, 503
  "please try again", network failure "tap to retry" with the typed form
  kept. Never show a raw error, stack trace or status code to a guest.
- Staff JWT: keep it in sessionStorage, send as Authorization: Bearer, and on
  401 go to the login screen. No secret or API key may ever be placed in
  the frontend (everything VITE_ is public).

DESIGN
- Use only tokens from src/styles/tokens.css and components from
  src/components/ui. Do not invent colours, fonts or spacing. Do not edit
  tokens.css unless I ask.
- Mobile first, designed at 375 px. No horizontal scroll. Tap targets at
  least 44 px. Visible keyboard focus. Respect prefers-reduced-motion.
- Every screen needs a loading state (skeleton with the same layout), an
  empty state, and an error state with a next action.
- One primary action per screen. Microcopy from docs/design-direction.md.
- Images: explicit width and height, loading="lazy" except the hero, a
  solid-colour placeholder. Local files only from /frontend/public/images.
  Never hotlink an image. If a photo is missing, use a neutral placeholder
  and tell me which file to add.
- Lazy-load every route with React.lazy. Keep the JavaScript small.

CHECK BEFORE FINISHING A TASK
1. npm run build passes. 2. No console errors. 3. Works at 375 px and at
1280 px. 4. Every state implemented. 5. Report anything not verified.
OUTPUT: the file list, the build result (last 5 lines), and anything
you could not verify. Nothing else.