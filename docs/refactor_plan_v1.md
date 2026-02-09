# Plan de refactorización (v1)

Este plan mantiene la arquitectura v1 y evita mezclarla con arquitectura paralela. El objetivo es estabilizar pagos multimoneda, sincronización offline-first y UX de POS.

## Objetivos principales
- Flujo de pagos consistente (Billing y Customer) con conversión correcta.
- Sync offline-first confiable (facturas/pagos sin duplicados).
- UX de Billing enfocada en retail/restaurantes (rápida, clara, sin fricción).
- Código más predecible y fácil de mantener sin romper producción.

## Fase 0: Diagnóstico (1-2 días)
- Inventario de flujos críticos: venta, pago, sincronización, caja.
- Identificar puntos de conversión (invoice currency, party currency, payment currency).
- Definir “source of truth” para tasa de cambio (POSContext.exchangeRate).

## Fase 1: Pagos y monedas (2-4 días)
- Unificar lógica de pagos en un helper común (v1) para Billing/Customer.
- Garantizar:
  - Si moneda de pago == moneda de factura, tasa = 1.0.
  - Si difiere, tasa = payment -> invoice (o payment -> receivable si aplica).
  - Monto aplicado = min(pago convertido, outstanding en receivable).
- Persistir tasa usada al crear factura (conversion_rate/custom_exchange_rate).
- Evitar duplicados usando paymentReference determinístico.

## Fase 2: Sync offline-first (2-3 días)
- Push v1 de facturas pendientes (sin depender de repos arquitectura paralela).
- Definir estrategia para pagos offline:
  - Opción A: PaymentEntry local + push dedicado.
  - Opción B: Ajuste de invoice + push invoice.
- Idempotencia en sincronización (por reference id/uuid).
- Registrar errores de sync con cola reintentable.

## Fase 3: UX Billing (3-5 días)
- Layout POS claro: carrito, pago, total y cambio visibles.
- Vista “rápida” para retail y “mesa” para restaurantes (configurable).
- Estado del pedido visible (pendiente, crédito, parcialmente pagado).
- Reforzar atajos y validaciones (no errores silenciosos).

## Fase 4: Limpieza de módulos v1 (2-4 días)
- Dividir Billing en capas pequeñas (UI → VM → handlers).
- Extraer utilidades de pago/moneda a `utils` v1.
- Reducir dependencias implícitas y side-effects.

## Fase 5: Tests mínimos (1-2 días)
- Tests de conversión (USD/NIO).
- Tests de idempotencia de pagos.
- Tests de sync pending.

## Entregables
- Documentación de flujos críticos.
- Helpers compartidos para pagos (v1).
- Sync push v1 estable.
- UI Billing renovada.

## Riesgos
- Cambios en ERPNext que alteren la moneda de `outstanding_amount`.
- Diferencias de tasa entre POS y servidor (solución: enviar conversion_rate).

## Nota
Este plan es incremental y mantiene v1 en producción sin introducir dependencias con arquitectura paralela.
