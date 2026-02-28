# ERPNext POS (KMP)

## Purpose
This project delivers an **offline-first Point of Sale (POS)** for **ERPNext v15/v16**, built with **Kotlin Multiplatform (KMP)** to share business logic and UI across **Android, iOS, and Desktop**.

## What it does
- Create and manage Sales Invoices and Payments.
- Manage POS cashbox opening/closing operations.
- Keep local catalog, customers, pricing, and stock for offline work.
- Sync local pending operations with ERPNext when connectivity returns.

## Architecture overview
Layers in shared `commonMain`:
1. **UI** (`ui/`, `views/`, `navigation/`)
2. **Domain** (`domain/models`, `domain/usecases`)
3. **Data** (`data/repositories`, `data/mappers`)
4. **Local** (`localSource/entities`, `localSource/dao`, `localSource/relations`)
5. **Remote** (`remoteSource/api`, `remoteSource/dto`, `remoteSource/sdk`)
6. **Sync** (`sync/SyncManager.kt`, `sync/SyncWorker.kt`)

## Key paths
- Shared code: `composeApp/src/commonMain/kotlin/com/erpnext/pos`
- Android: `composeApp/src/androidMain`
- iOS: `composeApp/src/iosMain` and `iosApp/`
- Desktop: `composeApp/src/desktopMain`

## Offline-first strategy
- Local-first writes with sync status tracking (`PENDING`, `SYNCED`, `FAILED`).
- Push pending local documents to ERPNext.
- Pull incremental updates and reconcile local state.

## Extended project documentation
- `docs/project_full_flow.md`
- `docs/tools_and_stack.md`
- `docs/business_and_sync_flows.md`
- `docs/erpnext_whitelisted_dto_contract.md`
- `docs/dto_field_matrix_v1.md`
- `docs/api_endpoint_inventory.md`
- `docs/multi_instance_login_and_session.md`
- `docs/function_diagram.md`

## Git Hooks (Commit/Push)
This repository includes versioned hooks in `.githooks/`.

1. Install hooks once per clone:
```bash
./scripts/install-git-hooks.sh
```
2. What runs automatically:
- `pre-commit`: runs Spotless only on staged `.kt/.kts`, auto-applies format, re-stages files, then validates.
- `pre-push`: `./gradlew spotlessCheck detekt test :androidApp:compileDebugKotlin`

3. Emergency bypass:
```bash
SKIP_GIT_HOOKS=1 git commit -m "..."
SKIP_GIT_HOOKS=1 git push
```

## Quality Tooling
- **Spotless + ktfmt** for code format (`.kt` and `.gradle.kts`).
- **Detekt** for static analysis with baseline per module:
  - `composeApp/detekt-baseline.xml`
  - `androidApp/detekt-baseline.xml`
  - configured as blocking (`ignoreFailures=false`) to prevent new debt from being merged.
- **Dependabot** for automated dependency update PRs (Gradle + GitHub Actions).

### Local commands
```bash
./gradlew spotlessCheck
./gradlew spotlessApply
./gradlew detekt
```

## CI Automation
GitHub Actions workflows:
- `Quality Gate`: runs on PR/push and executes `spotlessCheck`, `detekt`, `test`, and `:androidApp:compileDebugKotlin`.
- Dependabot (`.github/dependabot.yml`): weekly automated dependency PRs for Gradle and GitHub Actions.
