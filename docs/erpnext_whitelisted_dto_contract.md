# Contrato JSON real para endpoints en uso (solo v1)

Este documento define los **payloads reales** que la app espera hoy (KMP), usando exclusivamente los DTO de `remoteSource/dto` consumidos por `APIService.kt`.

## 1) Reglas de compatibilidad para `@frappe.whitelist`
- Mantener claves JSON exactamente como en `@SerialName`.
- Mantener nulabilidad y tipos (`String`, `Double`, `Boolean`, listas).
- Mantener wrapper de respuesta esperado por el SDK:
  - `getERPList`: `{"data": [ ... ]}`
  - `getERPSingle`: `{"data": { ... }}`
  - `postERP/putERP`: `{"data": { ... }}`
- Para métodos submit/cancel: aceptar y devolver `message` o `data` con estructura equivalente a `SubmitResponseDto`.

## 2) DTO de autenticación

### 2.1 TokenResponse (response)
```json
{
  "access_token": "<string>",
  "token_type": "Bearer",
  "expires_in": 3600,
  "refresh_token": "<string|null>",
  "id_token": "<string|null>",
  "scope": "<string|null>"
}
```

### 2.2 LoginInfo (config local de instancia)
```json
{
  "url": "https://erp.example.com",
  "redirect_uri": "com.erpnext.pos://oauth/callback",
  "clientId": "client-id",
  "clientSecret": "client-secret",
  "scopes": ["all", "openid"],
  "name": "Instance Name",
  "lastUsedAt": 1738000000000,
  "isFavorite": false
}
```

## 3) DTOs reales de request (create/update)

### 3.1 CustomerCreateDto (request)
```json
{
  "customer_name": "John Doe",
  "customer_type": "Individual",
  "customer_group": null,
  "territory": null,
  "default_currency": null,
  "default_price_list": null,
  "mobile_no": null,
  "email_id": null,
  "tax_id": null,
  "tax_category": null,
  "is_internal_customer": 0,
  "represents_company": null,
  "credit_limits": [
    {
      "company": "ACME",
      "credit_limit": 1000.0
    }
  ],
  "payment_terms": null,
  "customer_details": null
}
```

### 3.2 AddressCreateDto (request)
```json
{
  "address_title": "John Doe",
  "address_type": "Billing",
  "address_line1": null,
  "address_line2": null,
  "city": null,
  "state": null,
  "country": null,
  "email_id": null,
  "phone": null,
  "links": [
    {
      "link_doctype": "Customer",
      "link_name": "CUST-0001"
    }
  ]
}
```

### 3.3 ContactCreateDto (request)
```json
{
  "first_name": "John",
  "email_id": null,
  "mobile_no": null,
  "phone": null,
  "links": [
    {
      "link_doctype": "Customer",
      "link_name": "CUST-0001"
    }
  ]
}
```

### 3.4 ContactUpdateDto (request)
```json
{
  "email_id": null,
  "mobile_no": null,
  "phone": null
}
```

### 3.5 AddressUpdateDto (request)
```json
{
  "address_title": null,
  "address_type": null,
  "address_line1": null,
  "address_line2": null,
  "city": null,
  "state": null,
  "country": null,
  "email_id": null,
  "phone": null
}
```

### 3.6 PaymentEntryCreateDto (request)
```json
{
  "company": "ACME",
  "posting_date": "2026-01-10",
  "payment_type": "Receive",
  "party_type": "Customer",
  "party": "CUST-0001",
  "mode_of_payment": "Cash",
  "paid_amount": 100.0,
  "received_amount": 100.0,
  "paid_from": null,
  "paid_to": null,
  "paid_to_account_currency": null,
  "source_exchange_rate": null,
  "target_exchange_rate": null,
  "reference_no": null,
  "reference_date": null,
  "references": [
    {
      "reference_doctype": "Sales Invoice",
      "reference_name": "ACC-SINV-0001",
      "total_amount": null,
      "outstanding_amount": null,
      "allocated_amount": 100.0
    }
  ]
}
```

### 3.7 SalesInvoiceDto (request create/update)
```json
{
  "name": null,
  "customer": "CUST-0001",
  "customer_name": "John Doe",
  "contact_mobile": null,
  "company": "ACME",
  "posting_date": "2026-01-10",
  "due_date": null,
  "status": null,
  "grand_total": 100.0,
  "outstanding_amount": null,
  "total_taxes_and_charges": 0.0,
  "total": null,
  "net_total": 100.0,
  "rounded_total": null,
  "rounding_adjustment": null,
  "disable_rounded_total": null,
  "discount_amount": null,
  "paid_amount": null,
  "change_amount": null,
  "write_off_amount": null,
  "items": [
    {
      "item_code": "ITEM-001",
      "item_name": null,
      "description": null,
      "qty": 1.0,
      "rate": 100.0,
      "amount": 100.0,
      "discount_percentage": null,
      "warehouse": null,
      "income_account": null,
      "sales_order": null,
      "so_detail": null,
      "delivery_note": null,
      "dn_detail": null,
      "cost_center": null
    }
  ],
  "payments": [
    {
      "mode_of_payment": "Cash",
      "amount": 100.0,
      "account": null,
      "payment_reference": null,
      "type": "Receive"
    }
  ],
  "payment_schedule": [
    {
      "payment_term": null,
      "invoice_portion": 100.0,
      "due_date": "2026-01-10",
      "mode_of_payment": null
    }
  ],
  "payment_terms": null,
  "remarks": null,
  "custom_payment_currency": null,
  "custom_exchange_rate": null,
  "posa_delivery_charges": null,
  "is_pos": 1,
  "update_stock": 1,
  "doctype": "Sales Invoice",
  "pos_profile": null,
  "currency": null,
  "conversion_rate": null,
  "party_account_currency": null,
  "return_against": null,
  "pos_opening_entry": null,
  "debit_to": null,
  "docstatus": null,
  "is_return": 0
}
```

### 3.8 POSOpeningEntryDto (request)
```json
{
  "pos_profile": "POS-1",
  "company": "ACME",
  "user": "user@acme.com",
  "period_start_date": "2026-01-10 08:00:00",
  "period_end_date": "2026-01-10",
  "balance_details": [
    {
      "mode_of_payment": "Cash",
      "opening_amount": 0.0,
      "closing_amount": null
    }
  ],
  "taxes": [
    {
      "account_head": "VAT - ACME",
      "rate": 18.0,
      "amount": 0.0
    }
  ]
}
```

### 3.9 POSClosingEntryDto (request)
```json
{
  "pos_profile": "POS-1",
  "pos_opening_entry": "POS-OPE-0001",
  "user": "user@acme.com",
  "posting_date": "2026-01-10",
  "company": "ACME",
  "period_start_date": "2026-01-10 08:00:00",
  "period_end_date": "2026-01-10 18:00:00",
  "payment_reconciliation": [
    {
      "mode_of_payment": "Cash",
      "opening_amount": 0.0,
      "expected_amount": 100.0,
      "closing_amount": 100.0,
      "difference": 0.0
    }
  ],
  "sales_invoices": [
    {
      "sales_invoice": "ACC-SINV-0001",
      "posting_date": "2026-01-10",
      "customer": "CUST-0001",
      "grand_total": 100.0,
      "paid_amount": 100.0,
      "outstanding_amount": 0.0,
      "is_return": false
    }
  ]
}
```

## 4) DTOs reales de response

### 4.1 DocNameResponseDto
```json
{ "data": { "name": "CUST-0001" } }
```

### 4.2 SubmitResponseDto
```json
{ "message": { "name": "ACC-SINV-0001" } }
```

### 4.3 POSOpeningEntryResponseDto / POSClosingEntryResponse
```json
{ "data": { "name": "POS-OPE-0001" } }
```

### 4.4 PaymentEntryDto
```json
{
  "data": {
    "name": "ACC-PAY-0001",
    "posting_date": "2026-01-10",
    "party": "CUST-0001",
    "party_type": "Customer",
    "payment_type": "Receive",
    "mode_of_payment": "Cash",
    "paid_amount": 100.0,
    "received_amount": 100.0,
    "paid_from_account_currency": null,
    "paid_to_account_currency": "USD",
    "references": [
      {
        "reference_doctype": "Sales Invoice",
        "reference_name": "ACC-SINV-0001",
        "outstanding_amount": 100.0,
        "allocated_amount": 100.0
      }
    ]
  }
}
```

### 4.5 SalesInvoiceDto (response)
Debe incluir todo el contrato del DTO (campos request + campos base_* opcionales):
- `base_grand_total`, `base_total`, `base_net_total`, `base_total_taxes_and_charges`,
- `base_rounding_adjustment`, `base_rounded_total`, `base_discount_amount`,
- `base_paid_amount`, `base_change_amount`, `base_write_off_amount`, `base_outstanding_amount`.

### 4.6 CustomerDto (response)
```json
{
  "data": [
    {
      "name": "CUST-0001",
      "customer_name": "John Doe",
      "territory": null,
      "mobile_no": null,
      "customer_type": "Individual",
      "disabled": 0,
      "credit_limits": [
        {
          "company": "ACME",
          "credit_limit": 1000.0,
          "bypass_credit_limit_check": 0
        }
      ],
      "primary_address": null,
      "email_id": null,
      "image": null
    }
  ]
}
```

### 4.7 ContactListDto / AddressListDto (response)
```json
{
  "data": [
    {
      "name": "CNT-0001",
      "email_id": null,
      "mobile_no": null,
      "phone": null,
      "links": [{ "link_doctype": "Customer", "link_name": "CUST-0001" }]
    }
  ]
}
```

## 5) Checklist final para migración sin tocar código
- [ ] Cada endpoint `@whitelisted` devuelve wrapper correcto (`data` o `message`).
- [ ] JSON keys iguales a DTO real (incluyendo snake_case de `@SerialName`).
- [ ] Tipos `IntAsBooleanSerializer` enviados como `0/1` o parseables como boolean por app.
- [ ] No se elimina ningún campo requerido de request.
- [ ] Responses de create/read/update parsean directamente con DTO actuales.
