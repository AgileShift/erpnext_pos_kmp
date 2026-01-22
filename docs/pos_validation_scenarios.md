# POS Validation Scenarios (Legacy)

Fecha: 2026-01-22
Ambiente: staging

## 1) Sync inicial online
- Accion: En la app, ejecutar "Sync Now" estando online.
- Esperado: Muestra "Syncing..." y termina en OK sin errores.

## 2) Pago remoto (ERPNext) de factura pendiente
- Accion: En ERPNext, pagar una factura pendiente creada en la app. Luego "Sync Now" en la app.
- Esperado: La factura local cambia a Paid/Partly Paid y el outstanding baja o queda en 0.

## 3) Pago remoto parcial
- Accion: En ERPNext, pagar parcialmente una factura. Luego "Sync Now".
- Esperado: Estado Partly Paid y outstanding correcto.

## 4) Factura ya no pendiente en ERPNext
- Accion: En ERPNext, pagar o cancelar una factura pendiente. Luego "Sync Now".
- Esperado: En la app ya no aparece como pendiente y refleja el estado real.

## 5) Venta contado online (1 moneda)
- Accion: Vender en la app con pago completo en una moneda.
- Esperado: Factura creada, submit correcto, mensaje de exito, y popup visual. Regresa al carrito tras 1.2s.

## 6) Venta credito con pago parcial (online)
- Accion: Venta a credito con pago parcial (ej. 50 USD).
- Esperado: Sales Invoice en Draft->Submit, Payment Entry creado y submit, estado Partly Paid.

## 7) Venta credito sin pago
- Accion: Venta a credito sin pagos.
- Esperado: Factura con outstanding total, estado Unpaid, sin Payment Entry.

## 8) Venta multi-moneda (cash)
- Accion: Vender en NIO y USD con pagos separados.
- Esperado: Cada pago se registra con su moneda y cuenta; totales por moneda correctos.

## 9) Offline venta y sync
- Accion: Sin internet, crear venta. Luego reconectar y "Sync Now".
- Esperado: Se crea en servidor como draft y luego submit. Local pasa a Synced.

## 10) Apertura de caja (multimoneda)
- Accion: Abrir caja con montos por moneda (NIO, USD) sin conversion.
- Esperado: Montos de apertura se guardan por moneda sin conversion.

## 11) Pagos del turno
- Accion: Hacer dos ventas en NIO durante el turno.
- Esperado: "Pagos recibidos" muestra la suma real en NIO y 0 en USD.

## 12) Cierre de caja (online)
- Accion: Cerrar caja con el turno activo.
- Esperado: Se envia PCE en draft y submit; estado local pasa a cerrado.

## 13) Cierre de caja con facturas pendientes de sync
- Accion: Tener facturas pendientes locales y cerrar.
- Esperado: Primero se sincronizan facturas y pagos, luego se envia PCE.

## 14) Reapertura con POE existente
- Accion: Abrir caja con POE local abierto no sincronizado.
- Esperado: Se repara/sincroniza POE y la app entra al flujo normal.

## 15) Cliente - saldo pendiente correcto
- Accion: Revisar cliente con facturas pendientes (multi-moneda).
- Esperado: Total pendiente y creditos calculados con tasas correctas.

## 16) Error handling
- Accion: Forzar error de red durante submit.
- Esperado: Queda pendiente local y se reintenta en sync; mensaje claro al usuario.
