Rules for this repo:
- ONE task per message. Stop and show me the result when done.
- Never read, print, or commit .env files or secrets. Keep .env.example
  with variable names only.
- Never commit or push. I do the commits myself.
- Never edit an applied Flyway migration (V1 to V4). Schema changes go
  in a new V file.
- Validate inputs on the backend. Check roles on every endpoint.
- Write a test for any business logic and run it. Never write test
  names or test output from memory; show the real output.
- Use BigDecimal for money, never floats.
- Do not add features I did not ask for. Do not touch files I did not name.
- Read docs/design-pack.md, then docs/design-pack-v2-patch.md.
  The patch wins on conflicts.
- Backend tasks: work only in /backend.
- Frontend tasks: work only in /frontend, and also read
  docs/frontend-rules.md, docs/design-direction.md and
  docs/design-direction-patch.md (the patch wins on conflicts).
- Keep output short: file lists and summaries, not file contents.