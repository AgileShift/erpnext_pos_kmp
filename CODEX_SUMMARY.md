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


codex resume 019c20cd-2c22-73a0-bb2b-cef30097f83c