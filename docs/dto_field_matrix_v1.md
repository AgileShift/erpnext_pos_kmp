# Matriz de DTO reales (request/response) para endpoints en uso

Fuente: DTOs serializables usados por `APIService` y sus operaciones de red.

## Auth
- `TokenResponse`: `access_token`, `token_type`, `expires_in`, `refresh_token`, `id_token`, `scope`.
- `LoginInfo`: `url`, `redirect_uri`, `clientId`, `clientSecret`, `scopes`, `name`, `lastUsedAt`, `isFavorite`.

## Company / Config
- `CompanyDto`: `name`, `default_currency`, `country`, `tax_id`.
- `CompanySalesTargetDto`: `name`, `monthly_sales_target`.
- `StockSettingsDto`: `allow_negative_stock`.
- `CurrencyDto`: `name`, `currency_name`, `symbol`, `number_format`.
- `ModeOfPaymentDto`: `name`, `mode_of_payment`, `currency`, `enabled`.
- `ModeOfPaymentDetailDto`: `name`, `mode_of_payment`, `enabled`, `type`, `accounts`.
- `ModeOfPaymentAccountDto`: `company`, `default_account`.
- `AccountDetailDto`: `name`, `account_currency`, `account_type`, `company`.
- `PaymentTermDto`: `payment_term_name`, `invoice_portion`, `mode_of_payment`, `due_date_based_on`, `credit_days`, `credit_months`, `discount_type`, `discount`, `description`, `discount_validity`, `discount_validity_based_on`.
- `DeliveryChargeDto`: `label`, `default_rate`.
- `ExchangeRateResponse`: `message`.

## Master data
- `CategoryDto`: `name`.
- `CustomerGroupDto`: `name`, `customer_group_name`, `is_group`, `parent_customer_group`.
- `TerritoryDto`: `name`, `territory_name`, `is_group`, `parent_territory`.
- `UserDto`: `name`, `username`, `first_name`, `last_name`, `email`, `language`, `enabled`.

## Customer / Contact / Address
- `CustomerDto`: `name`, `customer_name`, `territory`, `mobile_no`, `customer_type`, `disabled`, `credit_limits`, `primary_address`, `email_id`, `image`.
- `CustomerCreditLimitDto`: `company`, `credit_limit`, `bypass_credit_limit_check`.
- `OutstandingInfo`: `totalOutstanding`, `pendingInvoices`.
- `ContactChildDto`: `name`, `mobile_no`, `phone`, `email_id`.
- `CustomerAddressDto`: `name`, `address_title`, `address_type`, `address_line1`, `address_line2`, `city`, `country`.
- `ContactListDto`: `name`, `email_id`, `mobile_no`, `phone`, `links`.
- `AddressListDto`: `name`, `address_title`, `address_type`, `address_line1`, `address_line2`, `city`, `state`, `country`, `email_id`, `phone`, `links`.
- `LinkRefDto`: `link_doctype`, `link_name`.
- `CustomerCreateDto`: `customer_name`, `customer_type`, `customer_group`, `territory`, `default_currency`, `default_price_list`, `mobile_no`, `email_id`, `tax_id`, `tax_category`, `is_internal_customer`, `represents_company`, `credit_limits`, `payment_terms`, `customer_details`.
- `CustomerCreditLimitCreateDto`: `company`, `credit_limit`.
- `AddressCreateDto`: `address_title`, `address_type`, `address_line1`, `address_line2`, `city`, `state`, `country`, `email_id`, `phone`, `links`.
- `AddressLinkDto`: `link_doctype`, `link_name`.
- `ContactCreateDto`: `first_name`, `email_id`, `mobile_no`, `phone`, `links`.
- `ContactLinkDto`: `link_doctype`, `link_name`.
- `ContactUpdateDto`: `email_id`, `mobile_no`, `phone`.
- `AddressUpdateDto`: `address_title`, `address_type`, `address_line1`, `address_line2`, `city`, `state`, `country`, `email_id`, `phone`.
- `DocNameResponseDto`: `name`.

## Inventory / Catalog
- `ItemDto`: `item_code`, `item_name`, `item_group`, `description`, `brand`, `image`, `disabled`, `stock_uom`, `standard_rate`, `is_stock_item`, `is_sales_item`, `barcodes`.
- `BarcodeChild`: `barcode`.
- `ItemPriceDto`: `item_code`, `uom`, `price_list`, `price_list_rate`, `selling`, `currency`.
- `BinDto`: `item_code`, `warehouse`, `actual_qty`, `reserved_qty`, `projected_qty`, `stock_uom`, `valuation_rate`.
- `ItemReorderDto`: `parent`, `warehouse`, `warehouse_reorder_level`, `warehouse_reorder_qty`.
- `WarehouseItemDto`: `item_code`, `actual_qty`, `price`, `valuation_rate`, `name`, `item_group`, `description`, `barcode`, `image`, `discount`, `is_service`, `is_stocked`, `stock_uom`, `brand`, `currency`.
- `ItemDetailDto`: `item_code`, `actual_qty`, `rate`, `item_name`, `item_group`, `description`, `barcode`, `image`, `discount`, `isService`, `is_stock_item`, `stock_uom`, `brand`, `standard_rate`, `is_sales_item`.

## Sales / Payments
- `SalesInvoiceDto`: campos completos de factura y respuesta base (`name`, `customer`, `customer_name`, `contact_mobile`, `company`, `posting_date`, `due_date`, `status`, `grand_total`, `outstanding_amount`, `total_taxes_and_charges`, `total`, `net_total`, `rounded_total`, `rounding_adjustment`, `disable_rounded_total`, `discount_amount`, `paid_amount`, `change_amount`, `write_off_amount`, `items`, `payments`, `payment_schedule`, `payment_terms`, `remarks`, `custom_payment_currency`, `custom_exchange_rate`, `posa_delivery_charges`, `is_pos`, `update_stock`, `doctype`, `pos_profile`, `currency`, `conversion_rate`, `party_account_currency`, `return_against`, `pos_opening_entry`, `debit_to`, `docstatus`, `is_return`, `base_grand_total`, `base_total`, `base_net_total`, `base_total_taxes_and_charges`, `base_rounding_adjustment`, `base_rounded_total`, `base_discount_amount`, `base_paid_amount`, `base_change_amount`, `base_write_off_amount`, `base_outstanding_amount`).
- `SalesInvoiceItemDto`: `item_code`, `item_name`, `description`, `qty`, `rate`, `amount`, `discount_percentage`, `warehouse`, `income_account`, `sales_order`, `so_detail`, `delivery_note`, `dn_detail`, `cost_center`.
- `SalesInvoicePaymentDto`: `mode_of_payment`, `amount`, `account`, `payment_reference`, `type`.
- `SalesInvoicePaymentScheduleDto`: `payment_term`, `invoice_portion`, `due_date`, `mode_of_payment`.
- `PaymentEntryDto`: `name`, `posting_date`, `party`, `party_type`, `payment_type`, `mode_of_payment`, `paid_amount`, `received_amount`, `paid_from_account_currency`, `paid_to_account_currency`, `references`.
- `PaymentEntryReferenceDto`: `reference_doctype`, `reference_name`, `outstanding_amount`, `allocated_amount`.
- `PaymentEntryCreateDto`: `company`, `posting_date`, `payment_type`, `party_type`, `party`, `mode_of_payment`, `paid_amount`, `received_amount`, `paid_from`, `paid_to`, `paid_to_account_currency`, `source_exchange_rate`, `target_exchange_rate`, `reference_no`, `reference_date`, `references`, `docstatus`.
- `PaymentEntryReferenceCreateDto`: `reference_doctype`, `reference_name`, `total_amount`, `outstanding_amount`, `allocated_amount`.

## POS Cashbox
- `POSProfileSimpleDto`: `name`, `company`, `currency`.
- `POSProfileDto`: `name`, `warehouse`, `route`, `country`, `company`, `currency`, `income_account`, `expense_account`, `payments`, `branch`, `apply_discount_on`, `cost_center`, `selling_price_list`.
- `PaymentModesDto`: `name`, `default`, `mode_of_payment`, `allow_in_returns`.
- `POSOpeningEntrySummaryDto`: `name`, `pos_profile`, `user`, `status`, `docstatus`, `period_start_date`.
- `POSOpeningEntryDto`: `pos_profile`, `company`, `user`, `period_start_date`, `period_end_date`, `balance_details`, `taxes`, `docstatus`.
- `POSOpeningEntryResponseDto`: `name`.
- `POSOpeningEntryDetailDto`: `name`, `pos_profile`, `company`, `period_start_date`, `posting_date`, `user`, `balance_details`.
- `BalanceDetailsDto`: `mode_of_payment`, `opening_amount`, `closing_amount`.
- `TaxDetailDto`: `account_head`, `rate`, `amount`.
- `POSClosingEntryDto`: `pos_profile`, `pos_opening_entry`, `user`, `posting_date`, `company`, `period_start_date`, `period_end_date`, `payment_reconciliation`, `sales_invoices`, `docstatus`.
- `PaymentReconciliationDto`: `mode_of_payment`, `opening_amount`, `expected_amount`, `closing_amount`, `difference`.
- `POSClosingSalesInvoiceDto`: `sales_invoice`, `posting_date`, `customer`, `grand_total`, `paid_amount`, `outstanding_amount`, `is_return`.
- `POSClosingEntrySummaryDto`: `name`, `pos_opening_entry`, `period_end_date`, `posting_date`, `docstatus`.
- `POSClosingEntryResponse`: `name`.
- `SubmitResponseDto`: `name`.
