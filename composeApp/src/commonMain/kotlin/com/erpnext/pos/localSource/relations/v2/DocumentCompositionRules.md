# Local document composition (v2)

This module reconstructs ERPNext documents locally by composing flat header rows with their
related child tables. All joins must use the composite key `(instanceId, companyId, documentId)`
where `documentId` is the primary identifier for the document type.

## Quotation
- **Header:** `QuotationEntity` (`quotations` table)
- **Items:** `QuotationItemEntity` (`quotation_items` table)
- **Taxes:** `QuotationTaxEntity` (`quotation_taxes` table)
- **Customer links:** `QuotationCustomerLinkEntity` (`quotation_customer_links` table)

To rebuild a quotation, load the header by `quotationId`, then attach all items, taxes, and
customer links with the same `quotationId`.

## Sales Order
- **Header:** `SalesOrderEntity` (`sales_orders` table)
- **Items:** `SalesOrderItemEntity` (`sales_order_items` table)

To rebuild a sales order, load the header by `salesOrderId`, then attach all items with the
same `salesOrderId`.

## Payment Entry
- **Header:** `PaymentEntryEntity` (`payment_entries` table)
- **References:** `PaymentEntryReferenceEntity` (`payment_entry_references` table)

To rebuild a payment entry, load the header by `paymentEntryId`, then attach all references
with the same `paymentEntryId`.

## Delivery Note
- **Header:** `DeliveryNoteEntity` (`delivery_notes` table)
- **Items:** `DeliveryNoteItemEntity` (`delivery_note_items` table)
- **Links:** `DeliveryNoteLinkEntity` (`delivery_note_links` table)

To rebuild a delivery note, load the header by `deliveryNoteId`, then attach all items and
links with the same `deliveryNoteId`.

## Pricing Rule
- **Header:** `PricingRuleEntity` (`pricing_rules` table)

Pricing rules are stored as a flat document with no child tables. Rebuild them directly from
`PricingRuleEntity`.
