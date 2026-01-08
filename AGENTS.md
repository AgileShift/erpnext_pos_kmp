# AGENTS.md

## 0) Regla de oro (anti-desastre)

Este repo tiene **dos arquitecturas vivas**:

- **Legacy (v1)**: EN PRODUCCIÓN / EN TRABAJO ACTIVO.
- **v2**: EN PAUSA (parcial), con módulos/utilidades reutilizados por v1.

**NO mezclar capas/DI/contratos entre v1 y v2 antes de tiempo.**
Ya hubo incidentes al mezclar `appModule` y `appModulev2`. Esto es un **risk multiplier**.

> Si un cambio implica “unificar”, “migrar”, “conectar DI” o “mover entidades/DAO/adapters” entre v1
> y v2: **PROHIBIDO sin aprobación explícita.**

---

## 1) Cómo decidir “dónde estoy tocando” (routing)

Antes de cambiar código:

1. Identificar si el archivo pertenece a **v1** o **v2** por su ubicación/naming.
2. Respetar las reglas de esa versión **sin intentar homogeneizar**.

**Heurísticas permitidas (sin adivinar):**

- Buscar en el repo: `v1`, `legacy`, `appModule`, `v2`, `appModulev2`, `ports`, `adapters`, `local`.
- Revisar `settings.gradle(.kts)` para ver módulos reales.
- Si hay carpetas tipo `.../v2/...` o `.../legacy/...`, esa es la frontera.

> Si la frontera no está clara: el agente debe **hacer máximo 2 preguntas** y detenerse.

---

## 2) Legacy (v1) – Reglas de operación

Estado: **trabajo activo**, estabilidad > pureza.

### 2.1 Patrón arquitectónico

- v1 **NO** está obligado a usar Ports/Adapters/Local/Remote como v2.
- No introducir “mini-v2” dentro de v1 por estética.
- Refactors permitidos: **pequeños, localizados, con ROI claro** (bugs, performance, legibilidad).

### 2.2 Cambios permitidos en v1

- Fixes, features, UX, performance.
- Extraer utilidades **solo si** se usan en más de 2 lugares y no rompen el diseño actual.
- Tests: si ya existe infraestructura; si no, no bloquear entregas por tests.

### 2.3 Cambios NO permitidos en v1 (sin aprobación)

- Reescrituras masivas para “alinear” con v2.
- Cambiar contratos de red/DB para que calcen con v2.
- Meter `appModulev2` en el arranque de v1.

---

## 3) v2 – Reglas de conservación (zona estéril)

Estado: **pausado**, se acepta mantenimiento mínimo.

### 3.1 Arquitectura v2 (cuando se toque)

v2 usa enfoque tipo Clean/Ports & Adapters:

- `ports/` interfaces
- `adapters/` implementaciones
- `local/` persistencia
- `remote/` API
- `domain/` reglas de negocio

**Pero**: v2 se diseñará **más ligero** y con una capa de datos (DB) **más plana**.

- No introducir complejidad extra (capas innecesarias, abstracciones por deporte).
- No copiar estructuras pesadas de v1.

### 3.2 Cambios permitidos en v2 (mientras está en pausa)

- Corregir bugs obvios que bloqueen compilación.
- Ajustes pequeños en utils compartidos si son parte de v2 y usados por v1 (ver sección 4).
- No reactivar features enteras.

### 3.3 Cambios NO permitidos en v2 (sin aprobación)

- “Conectar” v2 a v1 a través de DI.
- Migrar pantallas/casos de uso de v1 -> v2 o viceversa.
- Cambiar el modelo de datos de v2 salvo para alinearlo con su objetivo “plano y ligero”.

---

## 4) Código compartido (v1 consume piezas de v2)

Sí, v1 puede usar módulos/utils de v2, pero bajo contrato estricto:

### 4.1 Qué se puede compartir

✅ Compartible (preferido):

- `utils`, helpers puros, extensiones, formateadores
- modelos simples (value objects) si NO acoplan a DB/HTTP
- validaciones genéricas
- recursos multiplataforma (strings, etc.)

🚫 NO compartible (prohibido):

- DI modules (`appModulev2`, wiring de Koin/Hilt/etc.)
- DAOs, Entities de Room, repositorios con DB
- clients HTTP / servicios remotos con endpoints específicos
- ports/adapters completos (a menos que exista una decisión formal de “shared data layer”, que hoy
  NO existe)

### 4.2 Regla: “Shared no conoce a nadie”

Código compartido debe:

- No depender de `local`, `remote`, `di`, ni de implementaciones.
- Ser **determinista** (sin side effects ocultos).
- Evitar singletons globales.

---

## 5) DI (Inyección de dependencias) – Firewall obligatorio

**Prohibición explícita:**

- `appModule` (v1) y `appModulev2` (v2) NO se importan entre sí.
- No se registran bindings de v2 dentro del contenedor de v1.

### 5.1 Política práctica

- Cada versión tiene su “composition root” independiente.
- Si v1 necesita una utilidad de v2, se consume como **código directo** (función/clase pura), no por
  DI.
- Si hace falta una abstracción compartida, se define en un módulo neutral (si ya existe) o se
  mantiene en v1 hasta decisión formal.

### 5.2 Anti-patterns (no hacer)

- “Solo lo agrego al módulo para reusar X” → así empezó el problema.
- “Temporalmente” conectar módulos DI → la temporalidad es un mito.

---

## 6) Sync y Offline-First (aplica donde corresponda)

- v1 y v2 pueden tener estrategias distintas.
- No “unificar” sync entre v1 y v2 sin plan.
- Idempotencia, reintentos finitos, y scoping por `instanceId` si aplica.

---

## 7) Cambios: estilo de trabajo del agente

### 7.1 Antes de tocar código

- Identificar si el cambio cae en v1 o v2.
- Proponer plan mínimo (2–5 pasos).
- Si hay ambigüedad de frontera: máximo 2 preguntas y detenerse.

### 7.2 Entrega esperada

- Explicar: qué + por qué (breve).
- Entregar: diff claro.
- Verificación: comandos/tareas SOLO si existen en el repo (no inventar).

---

## 8) Checklist para PR (DoD)

- Compila en el/los targets tocados.
- No introduce dependencias cruzadas v1<->v2.
- No toca `appModulev2` cuando el cambio es v1 (y viceversa), salvo mantenimiento explícito
  aprobado.
- No rompe offline-first.
- Cambios pequeños, rollback fácil.

---

## 9) Decisiones actuales (para evitar discusiones circulares)

- v1 continúa como línea principal.
- v2 se conserva, se mantiene mínimo.
- v1 puede consumir utils de v2, pero **sin DI y sin data layer**.
- No se mezcla `appModule` con `appModulev2`.
- v2 apunta a ser **más ligero** y con DB **más plana**.
