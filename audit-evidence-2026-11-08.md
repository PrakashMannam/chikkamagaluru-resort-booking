# Dependency Security Evidence — 8 November 2026

- GitHub Reviewed Advisory, CVE-2026-39365 / GHSA-4w7w-66w2-5vf9: https://github.com/advisories/GHSA-4w7w-66w2-5vf9
  - Affected Vite versions include Vite 5 and earlier, and Vite 7 through 7.3.1. The advisory lists 7.3.2 as patched and says exploitation requires an exposed Vite development server plus a predictable sensitive `.map` file.
- Official Vite release support matrix: https://vite.dev/releases
  - On the review date, Vite 7.3 and Vite 8.3 were supported; security fixes are backported to specified supported lines.
- npm registry metadata for `@vitejs/plugin-react@4.3.2`: https://registry.npmjs.org/@vitejs/plugin-react/4.3.2
  - Peer range is Vite `^4.2.0 || ^5.0.0`, so it cannot be retained with Vite 7.
- npm registry metadata for `@vitejs/plugin-react@5`: queried with `npm view @vitejs/plugin-react@5 peerDependencies --json` on 8 Nov 2026; releases in the 5.x line accept Vite `^4.2.0 || ^5.0.0 || ^6.0.0 || ^7.0.0`.
- Chosen locked versions: Vite `^7.3.7` and `@vitejs/plugin-react` `^5.2.0`. After install, `npm audit --audit-level=moderate` reported `found 0 vulnerabilities`; `npm run build` succeeded.

This evidence describes the public package metadata and advisory at the time reviewed. It is not a substitute for repeating `npm audit` against the final lockfile before release.

## Live and repository snapshot checked 2026-11-08

- Current public repository `main`: commit `2c7dc65ee040a1b1e110a4387e2bdb7a0d25e91b`, `Merge pull request #10 from PrakashMannam/docs/submission-tdd-and-brief-alignment`. Source: `https://api.github.com/repos/PrakashMannam/chikkamagaluru-resort-booking/commits/main` (fresh fetch); independently matched `git ls-remote origin refs/heads/main`.
- The latest owner-supplied Railway deployment report (provided separately to this audit and not included in this bundle) records deployment commit `3ea853896f05649082b73714948c51591f7bb3cf`, deployment status `SUCCESS`, and Flyway migrations V7 and V8 applied, schema v8. This predates current `main`.
- Read-only backend health check: `GET https://resort-backend-production.up.railway.app/health` returned `{"status":"ok"}`. This endpoint does not disclose or prove the currently deployed source commit.
- Read-only public frontend check: `https://chikkamagaluru-resort-booking.vercel.app/` loaded. `https://chikkamagaluru-resort-booking.vercel.app/stay` rendered a default 14–16 Nov 2026 stay range, showing production does not yet match the local date-first remediation; no form was submitted.
- These checks did not create or alter any production booking, enquiry, payment, inventory, or database record. Production release alignment remains **NOT VERIFIED**.

- PR #11 (`https://github.com/PrakashMannam/chikkamagaluru-resort-booking/pull/11`) was open and reported two successful Vercel-related checks after the final branch update. Vercel posted the branch preview at `https://chikkamagaluru-resort-booking-git-a-083405-prakash-s-projects22.vercel.app/stay`. Navigating to that route redirected to Vercel login, so the remote preview UI was not inspected; responsive checks used the local built preview. No login or form submission was attempted.
