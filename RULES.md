Rules for this repo:
- ONE task per message. Stop and show me the diff when done.
- Read docs/design-pack.md, then docs/design-pack-v2-patch.md. The
  patch wins on conflicts.
- Never read, print, or commit .env files or secrets. Keep .env.example
  with variable names only.
- Validate inputs on the backend. Check roles on every endpoint.
- Write a test for any business logic and run it.
- Use BigDecimal for money, never floats.
- Do not add features I did not ask for. Do not touch files I did not name.
- Small commits with clear messages after each task.
