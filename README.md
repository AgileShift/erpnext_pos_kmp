This is a Kotlin Multiplatform project targeting Android, iOS, Desktop.

* `/composeApp` is for code that will be shared across your Compose Multiplatform applications.
  It contains several subfolders:
  - `commonMain` is for code that’s common for all targets.
  - Other folders are for Kotlin code that will be compiled for only the platform indicated in the folder name.
    For example, if you want to use Apple’s CoreCrypto for the iOS part of your Kotlin app,
    `iosMain` would be the right folder for such calls.

* `/iosApp` contains iOS applications. Even if you’re sharing your UI with Compose Multiplatform, 
  you need this entry point for your iOS app. This is also where you should add SwiftUI code for your project.


Learn more about [Kotlin Multiplatform](https://www.jetbrains.com/help/kotlin-multiplatform-dev/get-started.html)…

## V2 local composition rules (flat entities)

The V2 sync layer stores ERPNext documents as **flat tables** and composes them locally.
Each document is reconstructed by joining its header rows with their related detail rows
using the shared identifiers (`instanceId`, `companyId`, and the document ID).

| DocType | Header entity | Detail entities | Join keys |
| --- | --- | --- | --- |
| Quotation | `QuotationEntity` | `QuotationItemEntity`, `QuotationTaxEntity`, `QuotationCustomerLinkEntity` | `quotationId` |
| Sales Order | `SalesOrderEntity` | `SalesOrderItemEntity` | `salesOrderId` |
| Payment Entry | `PaymentEntryEntity` | `PaymentEntryReferenceEntity` | `paymentEntryId` |
| Delivery Note | `DeliveryNoteEntity` | `DeliveryNoteItemEntity`, `DeliveryNoteLinkEntity` | `deliveryNoteId` |
| Pricing Rule | `PricingRuleEntity` | _none_ | `pricingRuleId` |

Composition steps:
1. Fetch headers by `instanceId`, `companyId`, and document ID.
2. Fetch detail rows using the same composite key.
3. Merge detail rows into the in-memory view model without embedding them in the stored entity.

## Incremental sync criteria (V2)

Incremental sync should filter ERPNext list queries by **date**, **modified**, **route**, and
**warehouse** depending on the document type. The helper functions in
`remoteSource/sdk/v2/SyncFilters.kt` use these criteria:

* **Quotation**: `transaction_date >= fromDate`, optional `modified >= modifiedSince`, optional `route = routeId`, `territory = territoryId`.
* **Sales Order**: `transaction_date >= fromDate`, optional `modified >= modifiedSince`, optional `route = routeId`, `territory = territoryId`.
* **Payment Entry**: `posting_date >= fromDate`, optional `modified >= modifiedSince`, `territory = territoryId`.
* **Delivery Note**: `posting_date >= fromDate`, optional `modified >= modifiedSince`, optional `set_warehouse = warehouseId`, `territory = territoryId`.
* **Pricing Rule**: `modified >= modifiedSince`, optional `for_price_list = priceList`, `valid_upto >= fromDate`, `territory = territoryId`.
