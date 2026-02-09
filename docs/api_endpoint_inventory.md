# Inventario de endpoints consumidos por la app (solo v1)

Este inventario cubre los endpoints usados por `APIService.kt`.

## 1) Convención de transporte esperada
- List: `GET /api/resource/{Doctype}` -> `{"data": [...]}`
- Single: `GET /api/resource/{Doctype}/{name}` -> `{"data": {...}}`
- Create: `POST /api/resource/{Doctype}` -> `{"data": {...}}`
- Update: `PUT /api/resource/{Doctype}/{name}` -> `{"data": {...}}`
- Methods: `/api/method/...` -> `{"message": ...}` o `{"data": ...}`

## 2) OAuth / sesión
| Método app | Endpoint actual | Request | Response |
|---|---|---|---|
| exchangeCode | `/api/method/frappe.integrations.oauth2.get_token` | form-urlencoded | `TokenResponse` |
| refreshToken | `/api/method/frappe.integrations.oauth2.get_token` | form-urlencoded | `TokenResponse` |
| revoke | `/api/method/frappe.integrations.oauth2.revoke_token` | form-urlencoded | ack |

## 3) Métodos Frappe/ERP
| Método app | Endpoint actual | Request | Response |
|---|---|---|---|
| getExchangeRate | `/api/method/erpnext.setup.utils.get_exchange_rate` | query params | `ExchangeRateResponse.message` |
| setValue | `/api/method/frappe.client.set_value` | form-urlencoded | ack |
| submitPOSOpeningEntry | `/api/method/frappe.client.submit` | `doc` form field | `SubmitResponseDto` |
| submitPOSClosingEntry | `/api/method/frappe.client.submit` | `doc` form field | `SubmitResponseDto` |
| submitSalesInvoice | `/api/method/frappe.client.submit` | `doc` form field | `SubmitResponseDto` |
| submitPaymentEntry | `/api/method/frappe.client.submit` | `doc` form field | `SubmitResponseDto` |
| cancelSalesInvoice | `/api/method/frappe.client.cancel` | doctype+name | `SubmitResponseDto` |

## 4) Doctypes consumidos
- Company
- Stock Settings
- Payment Entry
- Sales Invoice
- Payment Term
- Delivery Charges
- Customer Group
- Territory
- Contact
- Address
- User
- Currency
- System Settings
- Mode of Payment
- Account
- Item Group
- Item
- POS Opening Entry
- POS Closing Entry
- POS Profile
- POS Profile Detail
- Bin
- Item Price
- Customer

## 5) Mapa método -> DTO
| Método app | Doctype/Method | Request DTO | Response DTO |
|---|---|---|---|
| createCustomer | Customer | `CustomerCreateDto` | `DocNameResponseDto` |
| createAddress | Address | `AddressCreateDto` | `DocNameResponseDto` |
| createContact | Contact | `ContactCreateDto` | `DocNameResponseDto` |
| updateContact | Contact | `ContactUpdateDto` | `ContactListDto` |
| updateAddress | Address | `AddressUpdateDto` | `AddressListDto` |
| createPaymentEntry | Payment Entry | `PaymentEntryCreateDto` | `SubmitResponseDto` |
| createSalesInvoice | Sales Invoice | `SalesInvoiceDto` | `SalesInvoiceDto` |
| updateSalesInvoice | Sales Invoice | `SalesInvoiceDto` | `SalesInvoiceDto` |
| openCashbox | POS Opening Entry | `POSOpeningEntryDto` | `POSOpeningEntryResponseDto` |
| closeCashbox | POS Closing Entry | `POSClosingEntryDto` | `POSClosingEntryResponse` |
| getPaymentEntryByName | Payment Entry | - | `PaymentEntryDto` |
| getSalesInvoiceByName | Sales Invoice | - | `SalesInvoiceDto` |
| getPOSOpeningEntry | POS Opening Entry | - | `POSOpeningEntryDetailDto` |
| getPOSProfileDetails | POS Profile | - | `POSProfileDto` |

## 6) Requisito para migrar a `@whitelisted`
Cambiar la URL del endpoint manteniendo exactamente el JSON esperado por cada DTO (request y response) descrito en `docs/erpnext_whitelisted_dto_contract.md`.
