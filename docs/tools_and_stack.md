# Herramientas, stack y capacidades técnicas

## 1. Plataforma
- Kotlin Multiplatform (commonMain, androidMain, iosMain, desktopMain).
- Compose Multiplatform para UI compartida.
- Targets: Android app, iOS framework/app host, Desktop JVM.

## 2. Build y configuración
- Gradle Kotlin DSL.
- Version catalog (`gradle/libs.versions.toml`).
- BuildKonfig para constantes por entorno.
- Room schema export para control evolutivo.

## 3. Librerías principales

### 3.1 Red/API
- Ktor client core.
- Ktor content negotiation.
- Ktor serialization kotlinx json.
- Ktor auth/logging.
- Engines por plataforma: OkHttp/Darwin/CIO.

### 3.2 Persistencia
- Room runtime + paging.
- androidx sqlite bundled.
- DataStore preferences.

### 3.3 Arquitectura
- Koin (DI).
- Coroutines + Flow.
- Kotlinx Serialization.
- Kotlinx Datetime.

### 3.4 Observabilidad
- Sentry SDK/Gradle plugin.
- Logger interno + breadcrumbs.

## 4. Módulos del repo

| Módulo/path | Responsabilidad |
|---|---|
| `composeApp/src/commonMain` | Lógica compartida de app |
| `composeApp/src/androidMain` | Integraciones Android |
| `composeApp/src/iosMain` | Integraciones iOS |
| `composeApp/src/desktopMain` | Integraciones Desktop |
| `remoteSource/*` | API/SDK/DTO remoto |
| `localSource/*` | Room entities/dao/relations |
| `data/repositories/*` | coordinación local-remoto |
| `domain/usecases/*` | casos de uso funcionales |
| `sync/*` | orquestación de sincronización |
| `di/*` | módulos de inyección de dependencias |

## 5. Capacidades clave habilitadas por stack
- Offline-first robusto.
- Sync incremental y push de pendientes.
- Multi instancia (datos de auth por sitio).
- UI homogénea multi plataforma.
- Manejo centralizado de errores/red/autenticación.

## 6. Prácticas operativas recomendadas
- Mantener DTO versionados y estables.
- No mezclar capa de UI con lógica de transporte.
- Validar contratos JSON con pruebas de integración.
- Monitorear métricas de sync y fallos por endpoint.
