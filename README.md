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
