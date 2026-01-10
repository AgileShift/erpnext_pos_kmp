# Codex Summary

## Estado actual

- Implementamos el administrador de sincronización (`SyncManager`) para escuchar `NetworkMonitor.isConnected`, respetar `SyncPreferences` (`autoSync` y `wifiOnly`) y disparar `fullSync()` automáticamente cuando vuelve la conexión, manteniendo el estado de sincronización actualizado.
- Ensamblamos feedback para la creación de facturas: ahora el `BillingViewModel` marca `isFinalizingSale` durante el guardado local, el `BillingScreen` muestra un snackbar de carga amistoso y luego muestra errores o éxitos según el caso, y seguimos guardando la factura localmente antes de intentar el push remoto con la cola existente.
- Motelamos la persistencia de DAOs v2 en los módulos de plataforma para asegurar que `SyncRepository` y `PushSyncManager` siempre encuentren sus dependencias aunque arranquemos desde Android o Desktop.
- Los pagos desde la vista de Customer ahora usan únicamente el `PaymentHandler`, la retroalimentación se muestra por `SnackbarController` y el quick-action summary calcula el total real de `outstanding_amount` por facturas (en la moneda de `party_account_currency`) y lo convierte también a la moneda activa del POS.
- Se añadió un `LoadingIndicator` global (barra superior) que se activa automáticamente en `BaseViewModel.executeUseCase`, cubriendo cualquier operación de guardado/lectura/actualización/eliminación.
- Al sincronizar clientes ahora recalculamos el resumen (`pendingInvoices`/`totalPendingAmount`) directamente desde `tabSalesInvoice` después de insertar las facturas remotas, de modo que las facturas locales pendientes (offline) se reflejen como parte del total sin perderse cuando vuelve el sync.
- De cara a mañana, seguimos con la lista ordenada en esta misma sección: después de dejar este estado de offline-first sólido, continuamos con la primera tarea pendiente de la checklist para no romper el orden preestablecido.

## Checklist de siguientes tareas (pendiente por completar)
- [X] Primero, antes que todo, necesitamos que si el token esta invalido manadr el refresh token en automatico y volver a autenticar, sin que de problemas antes de hacer cualquer peticion, deberiamos de hacer esto con el Ktor plugin de autenticacion
- [X] Mejorar los "connection timeout" al sincronizar
- [X] **Tenemos que aplicar la misma logica de pagos, metodos de pago, monedas, cambio de moneda, vuelto a favor, aplicar el monto justo a la factura y demas en la pantalla de Registrar pago en Customer igual que como esta en BillingScreen**
- [X] **Agregar una barra de carga mientras ejecuta cualquier proceso (guardado, lectura, actualizacion, eliminacion) en cualquiera de las vistas para darle contexto visual al usuario**
- [X] Aplicar cambio de moneda en la vista de Customer y Registrar Pago (Customer), a la moneda actual del POS si es diferente de la moneda de la factura, tenemos un caso extrano, la primera vez al crear la factura de credit e ir a la vista de Customer, aparece el monto en dolares, al cerrar la app y volver a entrar aparece como deberia, convertido a la moneda del POS
- [ ] Al realizar el pago desde "Registrar pago (Customer)" lo manda a pagar en el API, pero retorna el mensaje Payment exceeds outstanding amount y no actualiza la vista, sigue apareciendo la factura pendiente que ya se pago, esto implica tambien que extranamente se paga en el API no se actualiza el paid_amount en la DB
- [ ] No se estan registrando los pagos en tabSalesInvoicePayment cuando se paga por "Registrar pago (Customer)"
- [ ] Todo monto (Moneda) dentro de la aplicacion tiene que adaptarse a la moneda del POS actualemente abierto, por ejemplo todos los KPIs BI que tenemos estan bien en NIO, pero al abrir el POS en USD no deberian aparecer esos datos que se facturaron en NIO, que opinas? deberia o no, esta seria una vista de Administradores/Gerentes, les importa por moneda o no? Si es por moneda como hariamos?
- [ ] **Dejo de aparecer el credito disponible de los clientes**
- [ ] **Verificar que el saldo pendiente que se mira en la vista de Customer este en la ambas monedas, en la moneda de la factura que generalmente es la moneda del POS y en la moneda del party_account_currency de la factura, validar que podamos obtener esa informacion**
- [ ] **Crear la pantalla de conciliacion**
- [ ] La barra superior del Home me gustaria que fuera fija para cualquier pantalla en la que estemos
- [ ] Solucionar detalle con navegacion, si llamo a otra pantalla desde alguna otra no puedo regersar atras desde el menu, eso no deberia de pasar
- [ ] Agregar capacidad de scrolling en Home y tambien hacer collapsible por secciones de BI
- [ ] Configurar redondeo/tolerancia por moneda desde configuraciones (alineado con ERPNext)
- [ ] En las ordenes de venta poner opcion para reservar inventario por la cantidad de tiempo que la misma orden
- [ ] Aplicar tema oscuro a movil y desktop, por defecto del sistema o desde Configuracion
- [ ] Localizar los textos dentro del viewModel
- [ ] Localizar los textos de los menus
- [ ] La informacion del Dashboard (Resumen BI) no carga, de donde estamos trayendo la informacion?
- [ ] El flujo de sincronizacion Push esta tomando datos de la tablas v2 cuando aun no estamos guardando la informacion ahi, por el momento estamos trabajando full legacy, corregir
- [ ] Terminar las vistas con los flujos e informacion real para Quotation, Delivery Note, Sales Order
- [ ] Tenemos que seguir el ciclo de venta de ERP, esto quiere decir que la factura puede ser creada (Quotation → Sales Order → Delivery Note → Sales Invoice → Payment).
- [ ] Mejorar y mapear las variantes de los productos
- [ ] Refactorizar profundamente todo el codigo, aplicando principios KISS y SOLID
- [ ] Tenemos que crear politicas de sincronizacion, en cuantoa  tiempo los Customer, Items y Invoices deben de sincronizarse mas seguido que la tasa de cambio, informacion de empresa, etc
- [x] Tenemos que guardar el exchange_rate en local
- [x] Tenemos un problema al inciar, hace un llamado a /api/resource/User/{user} y esto rompe el patron Offline-First
- [x] Necesitamos refrescar en tiempo real cuando pagamos una factura
- [x] Tenemos que asegurarnos que la factura se paga local y remoto
- [x] Validar los pagos en efectivo, fallo, guarda la factura pero no envia los Payment Entry
- [x] Tenemos que guardar el debit_to de las facturas, para cuando apliquemos un pago no tengamos que ir a remoto a buscarlo
- [x] Paginacion automatica de todos los llamados al API para enviar "Request Failed - URI Too large"
- [x] Paginar resultados en la vistas con PagingData e investigar la forma de hacerlo dentro de los Dropdown
- [X] **Unir las ventas y pagos para enviar el POS Closing Entry**
- [X] **Aplicar exactamente la misma logica de pagos en billing que en Customer para la seccion de pagos de facturas pendientes, aunque dupliquemos, o bien podemos hacer un modulo de PaymentUtils o algun otro nombre y poner ahi para que ambas vistas compartan esa logica**
- [X] **En la pantalla de Registro de Pago, al seleccionar la cuenta por defecto ya conocemos la Moneda de esa cuenta, esto para remover el campo de Moneda y nos saltamos ese paso mas automatico, tenemos que copiar el behavior de Billing**
