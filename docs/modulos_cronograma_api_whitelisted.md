# Documento técnico de módulos, cronograma de desarrollo y API ERPNext `@frappe.whitelist`

## 1) Objetivo
Este documento define, para la app POS KMP conectada a ERPNext v15/v16:

1. El desglose técnico de módulos dentro de la aplicación.
2. Un cronograma futuro estimado de desarrollo por módulo.
3. Un contrato de integración recomendado vía métodos `@frappe.whitelist` para reducir acoplamiento con endpoints genéricos.

> Base técnica considerada: arquitectura KMP offline-first, navegación por rutas, capa de sincronización y `APIService` actual.

---

## 2) Módulos técnicos de la app (estado y alcance)

## M0. Core de plataforma y arquitectura compartida
**Objetivo:** soportar código compartido y comportamiento consistente Android/iOS/Desktop.

**Submódulos:**
- `commonMain` (UI, dominio, datos, sync).
- `androidMain`, `iosMain`, `desktopMain` (adaptadores de plataforma: storage, red, notificaciones, logger, paths).
- Inicialización de DI y utilidades base.

**Responsabilidades:**
- Uniformar reglas de negocio en una sola base KMP.
- Encapsular diferencias de runtime por plataforma.
- Mantener observabilidad y utilidades transversales (logs, sentry, time/network providers).

---

## M1. Autenticación OAuth2 y sesión multi-instancia
**Objetivo:** login seguro, refresh automático y selección de instancia ERP activa.

**Submódulos involucrados:**
- `views/login/*`
- `navigation/AuthNavigator.kt`
- `remoteSource/oauth/*`
- `auth/*` (session refresher, token heartbeat, instance switcher)

**Responsabilidades funcionales:**
- Alta y selección de instancia (`baseUrl`, `clientId`, `redirectUri`, scopes).
- Intercambio de `authorization_code` y persistencia de tokens.
- Refresh token en background y control de expiración.
- Logout controlado y limpieza de sesión local por instancia.

**Riesgos técnicos:**
- Manejo de expiración simultánea en sesiones largas.
- Aislamiento total de datos entre instancias.

---

## M2. Home/Dashboard operativo
**Objetivo:** mostrar estado de caja, conectividad, sincronización y accesos rápidos.

**Submódulos involucrados:**
- `views/home/*`
- `navigation/BottomBarNavigation*`
- widgets globales en `navigation/GlobalTopBar*`

**Responsabilidades funcionales:**
- Estado de caja actual (abierta/cerrada).
- Estado de sincronización (pendientes/errores/última corrida).
- Resumen comercial rápido (ventas del día, alertas).

---

## M3. Inventario y catálogo de productos
**Objetivo:** consulta de items, precios y stock para venta rápida.

**Submódulos involucrados:**
- `views/inventory/*`
- componentes `views/inventory/components/*`
- catálogos y entidades locales (`item`, `item_price`, `bin`, `item_group`)

**Responsabilidades funcionales:**
- Búsqueda y filtrado por texto, grupo y disponibilidad.
- Render de producto, precio efectivo y stock contextual.
- Soporte offline (lectura desde DB local con refresh incremental).

**Dependencias ERP:**
- `Item`, `Item Price`, `Bin`, `Item Group`, `Stock Settings`.

---

## M4. Billing / Carrito / Sales Flow
**Objetivo:** construir la venta POS de forma robusta en modo online/offline.

**Submódulos involucrados:**
- `views/billing/*`
- `views/salesflow/*`
- `views/payment/*` (handler de pagos)

**Responsabilidades funcionales:**
- Agregar/quitar items y recalcular totales, impuestos, descuentos.
- Soportar múltiples medios de pago en la misma venta.
- Generar `Sales Invoice` local (`PENDING`) y consolidar su push remoto.

**Dependencias ERP:**
- `Sales Invoice`, `Mode of Payment`, `Account`, `POS Profile`, reglas de impuestos.

---

## M5. Facturas (listado y detalle)
**Objetivo:** consulta operativa de ventas y estado de cobro.

**Submódulos involucrados:**
- `views/invoice/*`
- `views/invoice/components/*`

**Responsabilidades funcionales:**
- Listado por estado/fecha/cliente.
- Detalle de factura y saldo pendiente.
- Acciones de seguimiento (anulación, reenviar a pago, consulta de pagos relacionados).

---

## M6. Clientes
**Objetivo:** administrar alta, actualización y consulta de clientes con datos de contacto/dirección.

**Submódulos involucrados:**
- `views/customer/*`
- DTO y repositorios para `Customer`, `Contact`, `Address`

**Responsabilidades funcionales:**
- Alta rápida de cliente desde flujo de venta.
- Edición de contacto y dirección.
- Validaciones mínimas para operación POS (nombre, móvil/email opcional según política).

---

## M7. Payment Entry
**Objetivo:** registrar cobros aplicados a facturas y conciliarlos contra ERP.

**Submódulos involucrados:**
- `views/paymententry/*`
- creación y submit de `Payment Entry`

**Responsabilidades funcionales:**
- Crear pago por factura o pago independiente.
- Aplicar referencias (`references`) con monto asignado.
- Sincronización y confirmación de estado en ERP.

---

## M8. Caja y reconciliación (POS Opening / POS Closing)
**Objetivo:** controlar apertura/cierre de caja y consolidar resumen de jornada.

**Submódulos involucrados:**
- `views/reconciliation/*`
- `views/CashBoxManager.kt`
- lógica de `POS Opening Entry` y `POS Closing Entry`

**Responsabilidades funcionales:**
- Abrir caja con montos iniciales por medio de pago.
- Cerrar caja con resumen de ventas/cobros/diferencias.
- Bloqueos de negocio (no vender si caja no está abierta, según política).

---

## M9. Documentos comerciales extendidos
**Objetivo:** habilitar flujo comercial completo más allá de POS puro.

**Submódulos involucrados:**
- `views/quotation/*`
- `views/salesorder/*`
- `views/deliverynote/*`

**Responsabilidades funcionales:**
- Lectura y gestión operativa de Quotation, Sales Order y Delivery Note.
- Conversión asistida entre documentos cuando aplique.

---

## M10. Ajustes y configuración
**Objetivo:** administrar comportamiento funcional, preferencia de instancia y parámetros de ejecución.

**Submódulos involucrados:**
- `views/settings/*`

**Responsabilidades funcionales:**
- Preferencias de idioma/formato.
- Configuración de sincronización y diagnósticos.
- Gestión de instancias ERP y opciones de sesión.

---

## M11. Motor de sincronización offline-first
**Objetivo:** garantizar continuidad operativa con reconciliación robusta.

**Submódulos involucrados:**
- `sync/SyncManager.kt`
- `domain/usecases/sync/*`
- repositorios y estados de sync (`PENDING/SYNCED/FAILED`)

**Responsabilidades funcionales:**
- Push de pendientes por prioridad de negocio.
- Pull incremental por filtros (`modified`, fechas, POS profile, etc.).
- Retry con backoff, detección de conflictos y resolución controlada.

---

## M12. Observabilidad, soporte y calidad
**Objetivo:** trazabilidad de incidentes y estabilidad operativa.

**Submódulos involucrados:**
- utilidades `AppLogger`, `AppSentry`, notificaciones.
- pruebas unitarias de mappers/repositorios y escenarios POS.

**Responsabilidades funcionales:**
- Captura de errores con contexto de módulo.
- Métricas de salud de sync/auth.
- Cobertura de pruebas para flujos críticos de negocio.

---

## 3) Cronograma futuro estimado por módulo (roadmap)

## Supuestos de estimación
- Equipo base: **1 dev KMP senior + 1 dev fullstack ERPNext + 1 QA part-time**.
- Sprint de 2 semanas.
- Se prioriza producción incremental (vertical slices).
- Estimación en semanas efectivas (sin incluir tiempos legales/deploy corporativo).

## Roadmap sugerido (24 semanas)

| Fase | Semanas | Módulos objetivo | Entregables clave |
|---|---:|---|---|
| F1 | 1-2 | M1 + hardening M0 | Login multi-instancia estable, refresh robusto, logout seguro |
| F2 | 3-5 | M3 | Inventario con filtros, cache local consistente, stock/precio confiable |
| F3 | 6-9 | M4 | Billing completo con cálculos e integración de pagos mixtos |
| F4 | 10-12 | M8 | Apertura/cierre de caja + validaciones de operación |
| F5 | 13-15 | M6 + M7 | Alta/edición de clientes y Payment Entry integrado |
| F6 | 16-18 | M5 | Lista/detalle de facturas con acciones operativas |
| F7 | 19-21 | M11 | Sync incremental completo con retry/conflict policy |
| F8 | 22-23 | M9 + M10 | Quotation/SO/DN operativos + ajustes avanzados |
| F9 | 24 | M12 | hardening final, QA regresión, checklist release |

## Estimación por módulo (esfuerzo agregado)

| Módulo | Complejidad | Estimación | Dependencias críticas |
|---|---|---:|---|
| M0 Core compartido | Media | 2-3 semanas | arquitectura estable, DI, runtime por plataforma |
| M1 Auth multi-instancia | Alta | 2 semanas | OAuth2 ERP, token store seguro |
| M2 Home/Dashboard | Media | 1-1.5 semanas | estados de caja/sync, métricas |
| M3 Inventario | Alta | 3 semanas | Item/Bin/Price + estrategia de cache |
| M4 Billing/Sales Flow | Muy alta | 4 semanas | reglas de cálculo, impuestos, offline queue |
| M5 Facturas | Media | 2-2.5 semanas | filtros remotos/locales y performance |
| M6 Clientes | Media | 2 semanas | Customer+Contact+Address contracts |
| M7 Payment Entry | Media-alta | 2 semanas | referencias contra facturas y validación contable |
| M8 Caja/Reconciliación | Alta | 3 semanas | POS Opening/Closing, bloqueos de negocio |
| M9 Docs extendidos | Media | 2 semanas | Quotation/SO/DN endpoints |
| M10 Settings | Baja-media | 1 semana | persistencia de preferencias |
| M11 Sync engine | Muy alta | 3-4 semanas | reglas idempotentes, conflictos, retries |
| M12 Observabilidad/QA | Alta | 2 semanas | telemetría + suite de pruebas |

---

## 4) API ERPNext propuesta con métodos `@frappe.whitelist`

## 4.1 Objetivo de la capa whitelisted
Crear una capa estable para app móvil POS evitando acoplamiento directo con:
- `frappe.client.submit`
- `frappe.client.cancel`
- `frappe.client.set_value`
- endpoints genéricos de `resource` con filtros complejos en cliente.

Beneficios:
- Contratos JSON controlados por versión.
- Menor lógica de negocio en cliente móvil.
- Validaciones server-side coherentes y auditables.

## 4.2 Namespace recomendado
Sugerido en ERPNext app custom:
- `erpnext_pos.api.mobile.v1.*`

Ejemplo:
- `erpnext_pos.api.mobile.v1.auth.exchange_token`
- `erpnext_pos.api.mobile.v1.pos.open_shift`
- `erpnext_pos.api.mobile.v1.sales.submit_invoice`

## 4.3 Catálogo mínimo de métodos `@frappe.whitelist`

| Dominio | Método whitelisted propuesto | Tipo | Reemplaza/encapsula |
|---|---|---|---|
| Auth | `erpnext_pos.api.mobile.v1.auth.exchange_token` | POST | `frappe.integrations.oauth2.get_token` |
| Auth | `erpnext_pos.api.mobile.v1.auth.refresh_token` | POST | `frappe.integrations.oauth2.get_token` |
| Auth | `erpnext_pos.api.mobile.v1.auth.revoke_token` | POST | `frappe.integrations.oauth2.revoke_token` |
| Config | `erpnext_pos.api.mobile.v1.bootstrap.get_context` | GET | múltiples `resource` iniciales |
| Catalog | `erpnext_pos.api.mobile.v1.catalog.get_items` | GET | `Item`, `Item Price`, `Bin` combinados |
| Catalog | `erpnext_pos.api.mobile.v1.catalog.get_customers` | GET | `Customer` + contacto resumido |
| POS | `erpnext_pos.api.mobile.v1.pos.open_shift` | POST | create + submit POS Opening Entry |
| POS | `erpnext_pos.api.mobile.v1.pos.close_shift` | POST | create + submit POS Closing Entry |
| Sales | `erpnext_pos.api.mobile.v1.sales.create_invoice` | POST | create `Sales Invoice` |
| Sales | `erpnext_pos.api.mobile.v1.sales.submit_invoice` | POST | `frappe.client.submit` |
| Sales | `erpnext_pos.api.mobile.v1.sales.cancel_invoice` | POST | `frappe.client.cancel` |
| Payment | `erpnext_pos.api.mobile.v1.payment.create_entry` | POST | create `Payment Entry` |
| Payment | `erpnext_pos.api.mobile.v1.payment.submit_entry` | POST | `frappe.client.submit` |
| Customer | `erpnext_pos.api.mobile.v1.customer.create` | POST | create `Customer` + defaults |
| Customer | `erpnext_pos.api.mobile.v1.customer.update_contact` | POST | set/update Contact |
| Customer | `erpnext_pos.api.mobile.v1.customer.update_address` | POST | set/update Address |
| Sync | `erpnext_pos.api.mobile.v1.sync.pull_changes` | GET | consultas incremental unificadas |
| Sync | `erpnext_pos.api.mobile.v1.sync.push_batch` | POST | lote de documentos pendientes |

## 4.4 Firma de ejemplo en ERPNext (Python)

```python
import frappe

@frappe.whitelist(methods=["POST"])
def submit_invoice(payload: str):
    """Receives mobile invoice payload and returns normalized response."""
    data = frappe.parse_json(payload)
    # 1) validate required fields
    # 2) map payload -> Sales Invoice doc
    # 3) insert + submit (or enqueue)
    # 4) return normalized DTO
    return {
        "ok": True,
        "data": {
            "name": "ACC-SINV-0001",
            "docstatus": 1,
            "status": "Paid"
        },
        "error": None
    }
```

## 4.5 Contrato de respuesta estándar (recomendado)

```json
{
  "ok": true,
  "data": {},
  "error": null,
  "meta": {
    "request_id": "uuid",
    "server_time": "2026-01-10T10:00:00Z",
    "api_version": "v1"
  }
}
```

## 4.6 Políticas transversales para métodos whitelisted
- Validar permisos por rol (`POS User`, `Accounts User`) en servidor.
- Aplicar idempotencia para operaciones críticas (`submit_invoice`, `create_entry`, `open_shift`).
- Usar errores tipados (`code`, `message`, `details`) para mapearlos al dominio KMP.
- Versionar sin romper contratos (`/v1`, `/v2`).
- Incluir trazabilidad (`request_id`) para debugging entre móvil y ERP.

---

## 5) Recomendación de implementación por etapas (app + ERP)

1. **Sprint técnico 0:** definir contratos JSON definitivos por método whitelisted.
2. **Sprint 1-2:** migrar auth + bootstrap (`exchange/refresh/revoke/get_context`).
3. **Sprint 3-4:** migrar POS Shift (`open_shift`, `close_shift`) y Sales Invoice (`create/submit/cancel`).
4. **Sprint 5:** migrar Payment Entry + Customer create/update.
5. **Sprint 6:** activar `sync.pull_changes` y `sync.push_batch` con telemetría y retries.
6. **Sprint 7:** deprecar llamadas directas a `frappe.client.*` en app.

---

## 6) KPIs sugeridos de seguimiento
- **Auth stability:** tasa de refresh exitoso > 99%.
- **Sync reliability:** pendientes >24h < 2%.
- **POS continuity:** sesiones con operación offline recuperada > 98%.
- **API quality:** errores 5xx en métodos mobile < 0.5%.
- **Data consistency:** discrepancias factura local vs ERP < 1%.

---

## 7) Cierre
Con este enfoque por módulos + roadmap + API `@frappe.whitelist`, la app puede evolucionar de una integración genérica a una integración móvil robusta, versionada y orientada a operación POS real en campo, manteniendo compatibilidad con ERPNext v15/v16 y fortaleciendo el modelo offline-first.
