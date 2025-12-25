# ERPNext POS (KMP)

## Purpose
This project delivers an **offline-first Point of Sale (POS)** for **ERPNext v15/v16**, built with **Kotlin Multiplatform (KMP)** to share business logic and UI across **Android, iOS, and Desktop**. It prioritizes local operations and resilient sync so sales can continue even without connectivity.

## What it does
- **Sales flow:** create Sales Invoices, Sales Orders, Quotations, Delivery Notes, and Payments (including offline creation).
- **Cash management:** opening/closing entries and cashbox tracking.
- **Catalog & inventory:** items, pricing, stock, and customer data cached locally.
- **Sync:** bidirectional pull/push with background orchestration and status tracking.
- **Offline operation:** persist data locally first and sync when connectivity returns.

## How it works
- **Pull sync (remote → local):**
  - Uses incremental filters (date, modified, route, territory, warehouse) defined in `remoteSource/sdk/v2/SyncFilters.kt`.
  - Remote DTOs are mapped to local entities through `remoteSource/mapper` and `data/mappers`.
- **Push sync (local → remote):**
  - Offline-created entities are stored with `SyncStatus.PENDING` and later pushed by use cases in `domain/usecases/v2/sync`.
  - Status transitions are managed in `localSource/dao/v2` and `data/repositories/v2/SyncRepository.kt`.
- **Room (KMP) local store:**
  - Room entities and DAOs live under `localSource/entities` and `localSource/dao` (including V2 schemas).
  - `data/AppDatabase.kt` wires the local database for common code.
- **DTOs, mappers, repositories:**
  - **DTOs**: `remoteSource/dto` and `remoteSource/dto/v2` mirror ERPNext payloads.
  - **Mappers**: `remoteSource/mapper` and `data/mappers` map between DTOs, entities, and domain models.
  - **Repositories**: `data/repositories` and `data/repositories/v2` coordinate local/remote access.

## Architecture overview
**Layers (shared in `commonMain`):**
1. **UI**: Compose screens and coordinators (`ui/`, `views/`, `navigation/`).
2. **Domain**: Models, policies, and use cases (`domain/models`, `domain/usecases`, `domain/usecases/v2`).
3. **Data**: Repository implementations and adapters (`data/repositories`, `data/adapters`).
4. **Local**: Room entities/DAOs and relations (`localSource/entities`, `localSource/dao`, `localSource/relations`).
5. **Remote**: API clients, DTOs, SDK helpers (`remoteSource/api`, `remoteSource/dto`, `remoteSource/sdk`).
6. **Sync**: Orchestration and state (`sync/SyncManager.kt`, `sync/SyncWorker.kt`).

**Data flow (simplified):**
```
UI → ViewModel/Coordinator → Use Case → Repository
   → (Local DAO/Room) + (Remote API/DTO) → Sync state updates
```

## Key modules & paths
- **Shared KMP code**: `composeApp/src/commonMain/kotlin/com/erpnext/pos`
- **Platform entrypoints**:
  - Android: `composeApp/src/androidMain`
  - iOS: `composeApp/src/iosMain` and `iosApp/`
  - Desktop: `composeApp/src/desktopMain`
- **Database & entities**:
  - `composeApp/src/commonMain/kotlin/com/erpnext/pos/data/AppDatabase.kt`
  - `composeApp/src/commonMain/kotlin/com/erpnext/pos/localSource/entities`
  - `composeApp/src/commonMain/kotlin/com/erpnext/pos/localSource/dao`
- **Repositories**:
  - `composeApp/src/commonMain/kotlin/com/erpnext/pos/data/repositories`
  - `composeApp/src/commonMain/kotlin/com/erpnext/pos/data/repositories/v2`
- **Domain use cases**:
  - Offline creation: `composeApp/src/commonMain/kotlin/com/erpnext/pos/domain/usecases/v2` (e.g., `CreateInvoiceOfflineUseCase.kt`)
  - Sync units: `composeApp/src/commonMain/kotlin/com/erpnext/pos/domain/usecases/v2/sync`
- **Remote integration**:
  - API & SDK: `composeApp/src/commonMain/kotlin/com/erpnext/pos/remoteSource/api`, `.../sdk`, `.../sdk/v2`
  - DTOs: `composeApp/src/commonMain/kotlin/com/erpnext/pos/remoteSource/dto`
- **Sync orchestration**:
  - `composeApp/src/commonMain/kotlin/com/erpnext/pos/sync/SyncManager.kt`
  - `composeApp/src/commonMain/kotlin/com/erpnext/pos/sync/SyncWorker.kt`
- **DI modules**:
  - `composeApp/src/commonMain/kotlin/com/erpnext/pos/di/AppModule.kt`
  - `composeApp/src/commonMain/kotlin/com/erpnext/pos/di/v2/AppModule.kt`

## Offline-first strategy
- **Local-first writes:**
  - Offline use cases (e.g., `CreateInvoiceOfflineUseCase.kt`) persist entities locally with `SyncStatus.PENDING`.
- **Pending queue & retries:**
  - Pending items are fetched from Room and pushed in sync units (`domain/usecases/v2/sync`).
  - Failures are marked `FAILED`, counters are refreshed, and backoff policies apply (see `SyncPendingInvoicesUseCase.kt`).
- **Conflict handling:**
  - The server remains the source of truth; failed pushes are kept locally for retry and surfaced via sync status counters.
  - Incremental pull uses server `modified` filters to refresh local snapshots.
- **Operational visibility:**
  - Sync state and counters are stored in `localSource/entities/v2/SyncStateEntity.kt` and exposed in the UI.

## KMP consistency
- **Shared logic/UI** in `commonMain` ensures consistent behavior on Android, iOS, and Desktop.
- **Platform hooks** exist only where needed (entrypoints, platform services), keeping business logic and sync fully shared.
- **Room + DTO + Repository patterns** are centralized in common code to avoid platform divergence.
