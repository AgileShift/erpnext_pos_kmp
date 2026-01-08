# Codex Summary

## Checklist de siguientes tareas (pendiente por completar)
- [ ] **Aplicar exactamente la misma logica de pagos en billing que en Customer para la seccion de pagos de facturas pendientes, aunque dupliquemos, o bien podemos hacer un modulo de PaymentUtils o algun otro nombre y poner ahi para que ambas vistas compartan esa logica**
- [ ] **En la pantalla de Registro de Pago, al seleccionar la cuenta por defecto ya conocemos la Moneda de esa cuenta, esto para remover el campo de Moneda y nos saltamos ese paso mas automatico, tenemos que copiar el behavior de Billing**
- [ ] **Dejo de aparecer el credito disponible de los clientes**
- [ ] **Verificar que el saldo pendiente que se mira en la vista de Customer este en la ambas monedas, en la moneda de la factura que generalmente es la moneda del POS y en la moneda del party_account_currency de la factura, validar que podamos obtener esa informacion**
- [ ] **Unir las ventas y pagos para enviar el POS Closing Entry**
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
- [x] Tenemos que guardar el exchange_rate en local
- [x] Tenemos un problema al inciar, hace un llamado a /api/resource/User/{user} y esto rompe el patron Offline-First
- [x] Necesitamos refrescar en tiempo real cuando pagamos una factura
- [x] Tenemos que asegurarnos que la factura se paga local y remoto
- [x] Validar los pagos en efectivo, fallo, guarda la factura pero no envia los Payment Entry
- [x] Tenemos que guardar el debit_to de las facturas, para cuando apliquemos un pago no tengamos que ir a remoto a buscarlo
- [x] Paginacion automatica de todos los llamados al API para enviar "Request Failed - URI Too large"
- [x] Paginar resultados en la vistas con PagingData e investigar la forma de hacerlo dentro de los Dropdown
