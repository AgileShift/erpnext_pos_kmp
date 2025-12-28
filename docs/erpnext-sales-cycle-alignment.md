# ERPNext Sales Cycle Alignment Notes (POS)

## Official sales cycle (ERPNext docs)
The ERPNext Sales Cycle Integration guide describes a linear flow anchored at Sales Order:

1. **Sales Order** (start of the cycle).
2. **Delivery Note** (stock movement against the Sales Order).
3. **Sales Invoice** (billing document; can be created directly or from Sales Order / Delivery Note).
4. **Payment** (Payment Entry posted against the Sales Order or Sales Invoice).

Quotation is a pre-sales step that can lead into a Sales Order, but it is not part of the core cycle described in the integration guide.

Reference: <https://docs.frappe.io/erpnext/user/manual/en/sales-integration>

## Current POS flow (source comparison)

### BillingViewModel
**File:** `composeApp/src/commonMain/kotlin/com/erpnext/pos/views/billing/BillingViewModel.kt`

* The POS currently creates a **Sales Invoice directly** from the cart (no upstream document).
* If the sale is **paid immediately**, it populates `payments` and then **creates Payment Entry records** per payment line.
* If the sale is **credit**, it sets `payment_terms` + `payment_schedule` and leaves the invoice **Unpaid** until a Payment Entry is posted later.

### SalesInvoiceDto
**File:** `composeApp/src/commonMain/kotlin/com/erpnext/pos/remoteSource/dto/SalesInvoiceDto.kt`

* The DTO is focused on direct POS invoices (`is_pos`, `pos_profile`, `payments`).
* It did **not** explicitly carry `update_stock` or upstream references for Sales Order / Delivery Note at the line level.

## Steps outside the current POS scope (and why)

1. **Quotation creation**
   * Not wired from the POS billing flow. The POS prioritizes immediate checkout; quotations typically belong to CRM / pre-sales and require a separate workflow.

2. **Sales Order creation**
   * The POS does not currently generate Sales Orders from the cart. Sales Orders are usually used for reservation/fulfillment workflows, which the POS does not manage.

3. **Delivery Note creation**
   * Delivery Notes represent stock movement and fulfillment. The POS currently treats stock reduction as part of the direct Sales Invoice flow.

> Note: The codebase already includes v2 repositories and sync units for **Quotation**, **Sales Order**, and **Delivery Note** (e.g. `QuotationRepository`, `SalesOrderRepository`, `DeliveryNoteRepository`), but they are not wired into the Billing flow or exposed as POS actions.

## Compatibility adjustments for the official cycle

1. **Ensure `update_stock` is explicitly set for direct POS invoices**
   * When the POS creates a Sales Invoice without a Delivery Note, ERPNext should update stock. The invoice now includes `update_stock = true`.

2. **Prepare item-level references for upstream documents**
   * The Sales Invoice item DTO now includes:
     * `sales_order` / `so_detail`
     * `delivery_note` / `dn_detail`
   * These fields allow a future POS flow to create a Sales Invoice **from** a Sales Order or Delivery Note while remaining compatible with ERPNext’s official cycle.

## Next steps to enable optional Sales Order / Quotation / Delivery Note creation

* Add POS UI actions to choose the document type (Quotation, Sales Order, Delivery Note, or Sales Invoice).
* Populate the corresponding DTOs and sync units in the existing v2 layer.
* When creating a Sales Invoice **from** Delivery Note or Sales Order:
  * Set `update_stock = false` if stock was already updated by a Delivery Note.
  * Populate `sales_order`/`so_detail` and/or `delivery_note`/`dn_detail` on invoice items.
