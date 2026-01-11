# Payment Flow v1 (Billing + Customer Registrar Pago)

## 1) Fuentes de moneda
- **invoice.currency**: moneda de la factura (POSContext.currency).
- **invoice.partyAccountCurrency**: moneda base de la empresa (receivable).
- **payment currency**: proviene del método de pago (Mode of Payment -> account currency).

## 2) PaymentLine (UI -> lógica)
- `enteredAmount`: monto ingresado por el cajero en la moneda del método de pago.
- `exchangeRate`: tasa **payment -> invoice**.
- `baseAmount`: monto convertido a **invoice currency** (`enteredAmount * exchangeRate`).

## 3) Conversión para PaymentEntry
**Objetivo:** crear PaymentEntry en moneda receivable (partyAccountCurrency) usando la misma tasa que usó el cajero.

Regla de tasa **payment -> receivable**:
- Si payment == receivable: **1.0**.
- Si payment == invoice: **invoiceToReceivable**.
- Si invoice == receivable: **paymentToInvoice**.
- Si payment, invoice y receivable son distintos: **paymentToInvoice * invoiceToReceivable**.
- Fallback: resolver con API si no hay tasas locales.

## 4) Persistencia local
- **SalesInvoice.outstandingAmount** se guarda en **partyAccountCurrency**.
- **POSInvoicePayment.amount** se guarda en **partyAccountCurrency**.
- **POSInvoicePayment.enteredAmount** se guarda en la moneda del método de pago.

## 5) Idempotencia
- Cada pago local lleva `paymentReference` estable (ej: `POSPAY-LCL-{invoiceName}-{paymentId}`).
- La sincronización reintenta con el mismo reference.

## 6) Ejemplo (mock)
Supuestos:
- POS currency: **NIO**
- Party account currency (base): **USD**
- Tasa POS: **1 USD = 36.62 NIO**

Factura:
- Total: **915 NIO**
- conversionRate (invoice->base): **0.0273**
- outstanding (base): **25 USD**

Pago 1:
- Método: Cash USD
- enteredAmount: **25 USD**
- paymentToInvoice: **36.62**
- baseAmount (invoice): **915 NIO**
- paymentToReceivable: **1.0** (USD == base)
- PaymentEntry: **25 USD**

Pago 2 (cambio con NIO):
- Método: Cash NIO
- enteredAmount: **100 NIO**
- paymentToInvoice: **1.0**
- baseAmount (invoice): **100 NIO**
- paymentToReceivable: **0.0273**
- delivered: **2.73 USD**
- Si outstanding restante es **0.73 USD**, se aplica **0.73 USD** y se calcula cambio.

## 7) DI v2 (cross)
- `appModulev2` se carga junto al `appModule` en `initKoin`.
- Se removieron bindings duplicados de v2 en `appModule` para evitar conflictos.
