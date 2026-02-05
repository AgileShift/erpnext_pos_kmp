## Checklist de siguientes tareas (pendiente por completar)
- [ ] Primero, antes que todo, necesitamos que si el token esta invalido manadr el refresh token en automatico y volver a autenticar, sin que de problemas antes de hacer cualquer peticion, deberiamos de hacer esto con el Ktor plugin de autenticacion, Hearbeath no funciona, igual el token expira y tenemos que volver a inciiar sesion
- [ ] En las ordenes de venta poner opcion para reservar inventario por la cantidad de tiempo que la misma orden
- [ ] Verificar que el tema se apligue automaticamente segun el sistema, por favor
- [ ] Localizar los textos dentro del viewModel
- [ ] Localizar los textos de los menus
- [ ] Refactorizar profundamente todo el codigo, aplicando principios KISS y SOLID
- [ ] Tenemos que crear politicas de sincronizacion, en cuantoa  tiempo los Customer, Items y Invoices deben de sincronizarse mas seguido que la tasa de cambio, informacion de empresa, etc
- [ ] Cuando es pago multimoneda y queda en credito el dialogo de exito no aparece, por favor verificar ese escenario por favor


- Features
  - Posibilidad de vender inventario negativo (Reserva)
  - Apertura de caja puede hacer un backNavigation nuevamente a Home
  - Al abrir 


BI -> codex resume 019c296c-474c-71f2-962c-658d5d2683e1 --profile cloud
Invoices ->  codex resume 019c2b8a-bd44-7002-a5d3-65bb134b7c4f --profile cloud
Sincronizacion - Carga -> codex resume 019c2a52-16a3-7c93-9431-5ef974a74bfd --profile cloud

BI -> Error:

Vertically scrollable component was measured with an infinity maximum height constraints, which
is disallowed. One of the common reasons is nesting layout like LazyColumn and Column(Modifier.verticalScroll()).
If you want to add a header before the list of items please add a header as a separate item() before the main items() inside
the LazyColumn scope.