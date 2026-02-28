# AGENTS.md — erpnext_pos_kmp (ERPNext POS KMP, Offline-first)

---

# 1. Mission & Non-Negotiables

This repository implements an offline-first POS built with Kotlin Multiplatform + Compose,
integrated with ERPNext/Frappe APIs.

Primary success criteria:

- Data integrity > UI polish
- Idempotent remote operations (no duplicate submits)
- Deterministic sync flow (push → pull)
- Multi-currency correctness (NIO/USD) with strict rounding discipline
- No secrets in logs
- No production data in fixtures
- Small, reviewable, low-risk changes

This project prioritizes reliability, predictability, and architectural clarity over speed of
feature delivery.

---

# 2. Language Policy

- Code, comments, commit messages, and technical plans MUST be written in English.
- If the user asks in Spanish, explanations may be provided in Spanish.
- Technical sections (plans, diffs, architecture decisions) remain in English.
- Domain-specific Nicaraguan business terminology may be documented in Spanish in `docs/` if
  required.

---

# 3. Repo Structure & Responsibility Boundaries

Primary work happens inside:

- `composeApp/` → shared business logic + UI + sync + data layers
- `androidApp/` → thin Android wrapper
- `iosApp/` → thin iOS wrapper
- `docs/` → ADRs, architectural notes
- `config/detekt/` → static analysis rules
- `.githooks/` → quality enforcement

Within `composeApp/src/commonMain/...`:

### Domain Layer (pure business logic)

- `domain/models/`
- `domain/usecases/`
- `domain/ports/`
- `domain/policy/`
- `domain/sync/`

Domain must NOT depend on:

- DTOs
- Database entities
- Ktor responses
- Platform APIs

---

### Data Layer (implementations)

- `data/repositories/`
- `data/mappers/`
- `data/adapters/`

---

### Local Layer

- `localSource/dao/`
- `localSource/entities/`
- `localSource/preferences/`
- `localSource/datasources/`

---

### Remote Layer

- `remoteSource/api/`
- `remoteSource/dto/`
- `remoteSource/oauth/`
- `remoteSource/sdk/`
- `remoteSource/paging/`

---

### Sync Layer

- `sync/`
- `domain/sync/`

This is a high-risk zone. All changes require strict validation.

---

# 4. Working Agreement (Execution Protocol)

Before coding:

1. Identify affected modules (UI/domain/sync/local/remote).
2. Provide a short technical plan (max 8 bullets).
3. Identify risks (money, auth, sync, schema).
4. Keep change scope minimal (KISS).

After coding:

1. Run mandatory checks.
2. Add or update tests for high-risk logic.
3. Provide:

- Summary of changes
- Verification steps
- Risk assessment

---

# 5. Build & Quality Gates

From repository root:

Install hooks:

- `./scripts/install-git-hooks.sh`

Build:

- `./gradlew clean build`

Tests:

- `./gradlew test`

Static analysis:

- `./gradlew detekt`

Lint (if configured):

- `./gradlew lint`

If unsure:

- `./gradlew tasks`

No change is complete unless quality gates pass.

---

# 6. SOLID Enforcement Rules

Single Responsibility:

- No "God classes"
- Classes should have one reason to change

Open/Closed:

- Prefer extension via ports/interfaces
- Avoid modifying stable contracts

Liskov:

- Do not break substitutability
- Tests must validate behavior compatibility

Interface Segregation:

- Prefer multiple small ports over one large repository

Dependency Inversion:

- Domain depends only on abstractions
- Implementations live in data/local/remote

---

# 7. KISS Enforcement Rules

- Prefer smallest viable solution
- Avoid premature abstraction
- Avoid unnecessary generics
- Avoid over-architecting
- One concern per change
- No unrelated refactors

If existing pattern exists → extend it.
Do not invent new patterns unless justified.

---

# 8. Sync Rules (Offline-First Critical Section)

Canonical flow:

1. Validate session
2. Refresh token if needed
3. Push pending mutations
4. Confirm remote identifiers
5. Pull incrementals (paged)
6. Upsert local state
7. Emit explicit result (Success / Partial / Error)

Non-negotiables:

- All remote writes must be idempotent
- No fire-and-forget submits
- Capture ERPNext document ID/name
- Never delete local data without strategy (soft delete / tombstones)
- No silent conflict resolution

All sync modifications require:

- Retry reasoning
- Idempotency reasoning
- Explicit logging strategy

---

# 9. Money & Multi-Currency Discipline

- Never use Double for monetary calculations
- Prefer integer minor units or precise decimal strategy
- Apply rounding only at defined boundaries:
    - line
    - tax
    - total
- FX rate must be explicit
- Never mix currency contexts silently

Any change touching:

- payment/
- paymententry/
- billing/
- invoice/

Requires:

- Unit test for rounding edge case
- FX scenario validation

---

# 10. Auth & OAuth Rules

- Never log tokens or secrets
- Always sanitize error logs
- Explicit token refresh handling
- Clear separation between:
    - Session validation
    - Token refresh
    - API call retry

Auth failures must:

- Provide safe user message
- Provide sanitized developer log

---

# 11. Logging & Observability

Logs must help diagnose:

- Session expiration
- Sync retries
- ERPNext server errors (sanitized)
- Mapping errors

Never log:

- OAuth tokens
- Refresh tokens
- Passwords
- Full PII

---

# 12. Documentation & Commenting Policy

Goal: clarity without noise.

### Must document:

- Public APIs
- Ports
- Use cases
- Domain models with invariants
- Sync idempotency strategy
- Business rules (taxes, FX, rounding)

### Comment the WHY, not the WHAT.

Do NOT:

- Comment obvious code
- Write narrative essays inside functions
- Duplicate logic explanation already clear from naming

Prefer:

- Small functions
- Clear naming
- Extracted helpers
- Sealed result types

Long explanations belong in:
`docs/adr/XXXX-title.md`

---

# 13. Risk Classification

Safe changes:

- UI improvements within one feature
- Localized bug fixes
- Small mapper corrections

Risky changes:

- sync/
- domain/sync/
- remoteSource/oauth/
- multi-currency flows
- schema changes
- database entities

Risky changes require:

- Explicit reasoning
- Test coverage
- Verification steps

---

# 14. Output Format (Mandatory)

Responses must follow:

1. Plan (max 8 bullets)
2. Files changed + summary
3. Commands executed (or to execute)
4. Verification steps
5. Risks & rollback notes

Keep explanations concise.
Prefer action over narrative.

---

# 15. Do NOT Do List

- Do not introduce new libraries without justification
- Do not refactor unrelated code
- Do not rename packages broadly
- Do not change contracts without updating all callers
- Do not hardcode environment URLs
- Do not expose secrets

---

# 16. Definition of Done

- Builds successfully
- Tests pass
- No new detekt violations
- Critical flows validated
- Logs are meaningful and sanitized
- Change scope is minimal and justified

---

# 17. If Uncertain

- Search existing implementations first
- Follow existing patterns
- Ask for smallest missing detail
- Default to minimal change strategy

---

End of AGENTS.md