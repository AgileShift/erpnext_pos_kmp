# AGENTS.md — ERPNext POS (Offline-first)

## Objetivo del proyecto
Este repositorio implementa un POS offline-first integrado con ERPNext/Frappe version 16.x.x.
Prioridades: consistencia de datos, tolerancia a fallos (self-healing), multi-moneda (NIO/USD/etc,), y compatibilidad con flujos POS (apertura/cierre, pagos, impresión)
apegado totalmente a ERPNext y Frappe.

## Alcance (qué sí)
- Mejoras y fixes en app POS (UI, dominio, sync, storage local).
- Integración ERPNext vía API (auth, method, retries, idempotencia).
- Pruebas automatizadas y refactors con cobertura.

## Fuera de alcance (qué no)
- Cambios “rompedores” en contratos de API sin migración.
- Cambios grandes de arquitectura sin propuesta + plan.
- Reescrituras masivas no solicitadas.

## Estructura del repo
- /androidApp/    App POS (Kotlin/KMP, Compose, etc.)
- /androidMain/   App POS (Kotlin/KMP, Compose, etc.)
- /commonMain/    Codigo compartido entre apps (KMP Core)
- /domain/        Entidades, casos de uso, reglas de negocio
- /data/          Repositorios, DTOs, mappers, persistence
- /sync/          Orquestación sync (push/pull, colas, reintentos)
- /di/            Inyeccion de dependencias con Koin
- /localization/  Multilenguaje
- /localSource/   Fuente de datos local (SQLite)
- /remoteSource/  Fuente de datos remota (API)
- /ui/            UI (Android/iOS/Desktop (Multiplataforma))
- /utils/         Utilidades generales
- /test/          Tests unitarios,

## Setup local (comandos reales)
> Actualiza estos comandos con los tuyos
- Instalar deps: `./gradlew --version`
- Build: `./gradlew build`
- Tests: `./gradlew test`
- Lint (si aplica): `./gradlew lint`
- Formato (si aplica): `./gradlew ktfmtFormat` o `./gradlew spotlessApply`

## Reglas de ingeniería (no negociables)
- No introduzcas dependencias nuevas sin justificar impacto.
- Mantén cambios pequeños, revisables, y con pruebas.
- Todo cambio que afecte Sync debe:
    1) ser idempotente,
    2) registrar logs útiles,
    3) manejar reintentos y errores intermitentes,
    4) no duplicar submits en ERPNext.

## Convenciones de Sync (offline-first)
- Push pendientes antes de pull incremental.
- Pull incremental por DocType con cursores/updated_since cuando exista.
- Conflictos: preferir reglas explícitas (no “last write wins” silencioso).
- Nunca borres local sin una estrategia (tombstones / soft delete).

## Seguridad y datos
- Nunca loguear tokens/credenciales.
- Sanitizar información sensible en logs.
- No exponer datos de producción en pruebas/fixtures.

## Estándar de entrega (Definition of Done)
- Compila sin warnings nuevos.
- Tests pasan (unit/integration donde aplique).
- Incluye notas de cambio (qué, por qué, riesgo).
- Si toca flujos críticos (pagos, caja, facturación): añade prueba o caso reproducible.

## Modo de trabajo esperado
Antes de cambiar código:
1) Resume el plan en 5-10 líneas.
2) Indica archivos que tocarás.
3) Ejecuta tests relevantes.
4) Propón verificación manual mínima (pasos POS).