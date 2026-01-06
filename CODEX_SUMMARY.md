# Codex Summary (2026-01-04)

## Contexto
- Objetivo: Mejorar Home con indicadores BI, estados (WiFi/DB/Printer), Shift Open en tiempo real, y BI con métricas y gráficos.
- Se añadió costo real desde inventario (`valuation_rate`) para margen real.

## Cambios clave (Home UI/UX)
- Top bar:
  - WiFi on/off con color verde/gris + tooltip.
  - DB sync/health con color verde/ámbar/rojo/gris + tooltip.
  - Printer placeholder on/off (gris) + tooltip.
  - Menú perfil con Configuración/Cerrar sesión.
- Chip Shift Open:
  - Check circular outlined a la izquierda.
  - Verde cuando abierto, rojo cuando cerrado.
  - “--” cuando cerrado.
  - Duración actualiza cada segundo.

## BI (Home)
- KPIs: Ventas hoy, Facturas, Ticket promedio, Clientes hoy, CxC.
- Comparativos: vs ayer, vs semana pasada.
- Margen real:
  - Margen hoy ($ y %).
  - Margen 7 días ($ y %).
- Cobertura de costo (% de ítems vendidos con costo).
- Top productos por ventas (7 días).
- Top productos por margen (7 días, excluye ítems sin costo).
- Gráfico: línea de ventas últimos 7 días.

## Costos y margen real
- `valuation_rate` (costo) viene de Bin en `getInventoryForWarehouse`.
- Persistido en local (`tabItem`) para cálculo de margen.

## Cambios de datos/DB
- `tabItem` ahora tiene `valuation_rate` (Double, default 0).
- DB version incrementada a 13, con AutoMigration 12→13.

## Archivos editados (recientes)
- `composeApp/src/commonMain/kotlin/com/erpnext/pos/views/home/HomeScreen.kt`
- `composeApp/src/commonMain/kotlin/com/erpnext/pos/views/home/HomeMetrics.kt`
- `composeApp/src/commonMain/kotlin/com/erpnext/pos/domain/usecases/LoadHomeMetricsUseCase.kt`
- `composeApp/src/commonMain/kotlin/com/erpnext/pos/localSource/dao/SalesInvoiceDao.kt`
- `composeApp/src/commonMain/kotlin/com/erpnext/pos/remoteSource/dto/WarehouseItemDto.kt`
- `composeApp/src/commonMain/kotlin/com/erpnext/pos/remoteSource/api/APIService.kt`
- `composeApp/src/commonMain/kotlin/com/erpnext/pos/localSource/entities/ItemEntity.kt`
- `composeApp/src/commonMain/kotlin/com/erpnext/pos/remoteSource/mapper/ItemDtoToEntity.kt`
- `composeApp/src/commonMain/kotlin/com/erpnext/pos/data/AppDatabase.kt`

## Métricas y queries añadidas (SalesInvoiceDao)
- `getSalesCountForDate`, `getDistinctCustomersForDate`, `getDailySalesTotals`
- `getTopProductsBySales`
- `getEstimatedMarginTotal`
- `countItemsWithCost`, `countItemsInRange`
- `getTopProductsByMargin`

## Estado actual
### Completado
- UI Home con status icons + tooltips.
- Shift Open realtime.
- BI completo + gráfico.
- Costo persistido y margen real.
- Top productos por ventas y por margen.
- Cobertura de costo.

### Pendiente / Recomendado
- Verificar migración 12→13 en Room (si falla, crear migración manual).
- Ejecutar build/tests para validar cambios.
- Confirmar si se desea:
  - Top productos por margen en UI ya; ajustar diseño si necesario.
  - Margen por producto con % en lista (ya agregado).

## Notas adicionales
- Salud DB se calcula con `lastSyncAt` + conexión + `SyncState`.
- Margen solo es confiable cuando `valuation_rate > 0`; por eso cobertura %.

## Próximos pasos sugeridos
1) Ejecutar build para validar (si el proyecto lo permite en el entorno).
2) Revisar UI (espaciado) de BI si requiere ajuste visual.
3) Confirmar con negocio si el costo correcto es `valuation_rate` o `standard_rate`.

## Checklist de siguientes tareas (pendiente por completar)
- [ ] Localizar los textos dentro del viewModel
- [ ] Localizar los textos de los menus
- [ ] Necesitamos refrescar en tiempo real cuando pagamos una factura
- [ ] Tenemos que asegurarnos que la factura se paga local y remoto
- [ ] Terminar las vistas con los flujos e informacion real para Quotation, Delivery Note, Sales Order 
- [ ] Unir las ventas y pagos para enviar el POS Closing Entry
- [ ] Crear la pantalla de conciliacion
- [x] Validar los pagos en efectivo, fallo, guarda la factura pero no envia los Payment Entry
- [ ] Tenemos que guardar el debit_to de las facturas, para cuando apliquemos un pago no tengamos que ir a remoto a buscarlo
- [ ] La barra superior del Home me gustaria que fuera fija para cualquier pantalla en la que estemos
- [ ] Dejo de aparecer el credito disponible de los clientes
- [ ] Verificar que el saldo pendiente que se mira en la vista de Customer este en la ambas monedas, en la moneda de la factura que generalmente es la moneda del POS y en la moneda del party_account_currency de la factura, validar que podamos obtener esa informacion
- [ ] En la pantalla de Registro de Pago, al seleccionar la cuenta por defecto ya conocemos la Moneda de esa cuenta, esto para remover el campo de Moneda y nos saltamos ese paso mas automatico, tenemos que copiar el behavior de Billing
- [ ] Solucionar detalla con navegacion, si llamo a otra pantalla desde alguna otra no puedo regersar atras desde el menu, eso no deberia de pasar
- [ ] Agregar capacidad de scrolling en Home y tambien hacer collapsible por secciones de BI
- [ ] Tenemos que seguir el ciclo de venta de ERP, esto quiere decir que la factura puede ser creada (Quotation → Sales Order → Delivery Note → Sales Invoice → Payment).
- [ ] Mejorar y mapear las variantes de los productos
- [ ] Paginacion automatica de todos los llamados al API para enviar "Request Failed - URI Too large"
- [ ] Paginar resultados en la vistas con PagingData e investigar la forma de hacerlo dentro de los Dropdown