## Checklist de siguientes tareas (pendiente por completar)
- [ ] Lo mas importante y primero, debemos de confirmar
- [ ] Primero, antes que todo, necesitamos que si el token esta invalido manadr el refresh token en automatico y volver a autenticar, sin que de problemas antes de hacer cualquer peticion, deberiamos de hacer esto con el Ktor plugin de autenticacion, Hearbeath no funciona, igual el token expira y tenemos que volver a inciiar sesion
- [ ] **Agregar una barra de carga mientras ejecuta cualquier proceso (guardado, lectura, actualizacion, eliminacion) en cualquiera de las vistas para darle contexto visual al usuario**
- [ ] La barra superior del Home me gustaria que fuera fija para cualquier pantalla en la que estemos
- [ ] Solucionar detalle con navegacion, si llamo a otra pantalla desde alguna otra no puedo regersar atras desde el menu, eso no deberia de pasar
- [ ] Solucionar, si estoy en el proceso de Checkout dentro de Billing, poder regresar al carrito de compra
- [ ] Agregar capacidad de scrolling en Home y tambien hacer collapsible por secciones de BI
- [ ] En las ordenes de venta poner opcion para reservar inventario por la cantidad de tiempo que la misma orden
- [ ] Verificar que el tema se apligue automaticamente segun el sistema, por favor
- [ ] Ajustar los temas para hacerlo mas llamativos y coorporativos, ajustar toda la UI a esos temas
- [ ] Localizar los textos dentro del viewModel
- [ ] Localizar los textos de los menus
- [X] Terminar las vistas con los flujos e informacion real para Quotation, Delivery Note, Sales Order
- [X] Tenemos que seguir el ciclo de venta de ERP, esto quiere decir que la factura puede ser creada (Quotation → Sales Order → Delivery Note → Sales Invoice → Payment).
- [ ] Refactorizar profundamente todo el codigo, aplicando principios KISS y SOLID
- [ ] Tenemos que crear politicas de sincronizacion, en cuantoa  tiempo los Customer, Items y Invoices deben de sincronizarse mas seguido que la tasa de cambio, informacion de empresa, etc

codex resume 019ba8ce-bdba-7200-97ae-4230e0ddccef