# Plan de validación del flujo POS

Este documento resume los escenarios críticos que debemos cubrir antes de considerar el flujo POS como estable. Está escrito desde la experiencia real de un ERPNext + POS (multimoneda, pagos en efectivo/crédito, sincronización offline/online) y se adhiere a la regla “lógica primero, UI luego”.

## 1. Sincronización inmediata y consistente (pull/push)
1. Iniciar sesión con conexión y confirmar que todos los documentos locales pendientes (POE, facturas, pagos, PCE) se envían inmediatamente y se actualizan con `docstatus = 1` luego de `frappe.client.submit`. Esperar que la app no muestre errores y que el contador de pendientes quede en cero.
2. Crear una factura fuera de línea, cerrarla (pago parcial o total) y luego reconectar. Verificar que la sincronización automática (push) envía la factura, los Payment Entry y que el cierre solo ocurre cuando el servidor confirma el `docstatus` definitivo.
3. Desde el dashboard ERPNext cancelar una factura o marcar un pago. Luego, forzar un pull y validar que la tabla local se alinea (mismos montos, estados, `docstatus`), sin eliminar el registro manualmente.

## 2. Apertura y cierre de caja
1. Aperturar caja en varias monedas (NIO, USD) y confirmar que el registro inicial respeta las denominaciones originales sin convertirlos a la moneda base.
2. Generar varias facturas en el turno y registrar pagos en efectivo y crédito. Antes de cerrar, ejecutar la sincronización pull/push para asegurarse de que los pagos tienen ID remoto, referencia a la POE vigente y `docstatus = 1`.
3. Cerrar la caja y verificar que el `POS Closing Entry` incluye solo facturas con nombres remotos válidos (sin prefijos `LOCAL-` o valores `None`) y que sus `balance_details` se calculan con los `PaymentEntry` reales. Validar que no aparece el error “Duplicate POS Invoices found”.
4. En caso de retorno/cancelación dentro del turno (cliente llega y devuelve o cancela), verificar que la factura no se suma nuevamente al cierre y que aparece marcada como `docstatus = 2` en ERPNext.

## 3. Facturas y pagos multimoneda
1. Crear facturas con pagos parciales en dos monedas distintas. Confirmar que cada Payment Entry se genera con la moneda/cuenta correcta, que el `reference_no` se conserva y que los montos se registran en la tabla local (`tabSalesInvoicePayment`).
2. Verificar que el `Reconciliation` calcula la expectativa real (apertura + pagos) y que al final los `expectedByMode` por moneda reflejan el efectivo físico recibido.
3. Emitir una factura de crédito y aplicar múltiples pagos parciales. Al cerrar, comprobar que las notas de crédito o retenciones no se re-sumen como ingresos y que el total esperado se ajusta.

## 4. Cancelaciones y devoluciones
1. Desde el cliente, seleccionar una factura y ejecutar la acción “Cancelar” y “Retorno”. Corroborar que el flujo sigue a `CancelSalesInvoiceUseCase`, que la nota de crédito se crea con relación a la factura original y que los Payment Entry se invierten adecuadamente.
2. Probar el caso de una factura parcialmente pagada que luego se cancela. Validar que el cierre no la considera y que al hacer el pull la tabla local se actualiza (pagos marcados como reverso).

## 5. UI y mensajes
1. Confirmar que todo evento con conexión dispara un feedback inmediato (popup o dialogo) indicando “Pago procesado”, “Factura enviada” o “Sincronización completada”.
2. Verificar que el popup aparece en cualquier tipo de factura (contado, crédito parcial, crédito total), que dura ~2.5 segundos y luego regresa suavemente al carrito.
3. Revisar el debug banner: eliminar el mensaje “Multiple cash modes are …”, agregar indicador de progreso (spinner/texto) cada vez que se ejecuta una sincronización push/pull y mostrar errores claros.

## 6. Token y estabilidad
1. Simular expiración del token; el heartbeat debe refrescar automáticamente el token antes de que expire y reintentar cualquier operación fallida (apertura/cierre/factura/pago).
2. Verificar que no existen operaciones bloqueadas por tokens caducados: abrir caja, generar factura, cerrar y sincronizar, todo sin fallos de autenticación.

## 7. Salud de la base local
1. Comprobar que la tabla local nunca borra facturas activas sin razón. Si una factura se elimina, debería identificarse en el log y respawnear al hacer el fetch remoto.
2. Validar que cada factura/local payment guarda el `opening_entry` correcto y se vincula al `POS Opening Entry` vigente para poder reconstruir cierres y balances.

## Observaciones
- Toda prueba debe registrar el valor esperado (números, `docstatus`, IDs remotos) y el resultado real; si hay diferencia se debe documentar para refinamiento.
- Esta hoja se puede expandir con casos de prueba UI adicionales (cancelado desde menú contextual del cliente, grid de facturas, etc.) cuando la lógica central esté estabilizada.
