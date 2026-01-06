package com.erpnext.pos.base

import androidx.paging.ExperimentalPagingApi
import androidx.paging.LoadType
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.PagingSource
import androidx.paging.PagingState
import androidx.paging.RemoteMediator
import com.erpnext.pos.utils.AppSentry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {
    class Loading<T>(data: T?) : Resource<T>(data)
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
}

inline fun <ResultType, RequestType> networkBoundResource(
    crossinline query: suspend () -> Flow<ResultType>,                   // 🔹 Obtiene datos del cache local
    crossinline fetch: suspend () -> RequestType,                        // 🔹 Llama al API remoto
    crossinline saveFetchResult: suspend (RequestType) -> Unit,          // 🔹 Guarda el resultado remoto
    crossinline shouldFetch: suspend (ResultType) -> Boolean = { true }, // 🔹 Condición opcional de refresco
    crossinline onFetchFailed: (Throwable) -> Unit = {}                  // 🔹 Manejo opcional de errores
): Flow<Resource<ResultType>> = flow {
    val localFlow = query()
    val localData = localFlow.firstOrNull()

    emit(Resource.Loading(localData))

    val flow = if (localData != null && !shouldFetch(localData)) {
        // 🔸 Si no necesitamos fetch, devolvemos cache directamente
        localFlow.map { Resource.Success(it) }
    } else {
        try {
            val remoteData = fetch()
            saveFetchResult(remoteData)
            query().map { Resource.Success(it) }
        } catch (throwable: Throwable) {
            onFetchFailed(throwable)
            AppSentry.capture(throwable, "networkBoundResource fetch failed")
            localFlow.map { Resource.Error(throwable.message ?: "Error de red", it) }
        }
    }

    emitAll(flow)
}

/**
 * NetworkBoundResource Paged
 *
 * Diseñado para integrarse con Room + Paging3 y fuentes remotas.
 *
 * @param query -> Retorna el PagingSource local desde Room
 * @param fetch -> Lógica de obtención remota (acepta page y pageSize)
 * @param saveFetchResult -> Guarda los resultados de red en base local
 * @param clearLocalData -> Limpia la data local (opcional, útil para refresh)
 * @param pageSize -> Tamaño de página estándar
 * @param shouldFetch -> Lógica opcional que determina si debe hacerse fetch remoto
 */
fun <T : Any, V : Any> networkBoundResourcePaged(
    query: () -> PagingSource<Int, T>,
    fetch: suspend (page: Int, pageSize: Int) -> List<V>,
    saveFetchResult: suspend (List<V>) -> Unit,
    clearLocalData: (suspend () -> Unit)? = null,
    shouldFetch: suspend () -> Boolean = { true },
    pageSize: Int = 20
): Flow<PagingData<T>> {

    return Pager(
        config = PagingConfig(
            pageSize = pageSize,
            enablePlaceholders = false,
            prefetchDistance = 3,
            initialLoadSize = pageSize * 2
        ),
        pagingSourceFactory = { query() }
    ).flow.onStart {
        // 🔄 Refresco inicial
        try {
            if (shouldFetch()) {
                clearLocalData?.invoke()
                val remoteData = fetch(0, pageSize)
                if (remoteData.isNotEmpty()) {
                    saveFetchResult(remoteData)
                }
            }
        } catch (e: Exception) {
            print("Error inicial al sincronizar datos: ${e.message}")
        }
    }.catch { e ->
        print("Error en flujo de Paging: ${e.message}")
        AppSentry.capture(e, "networkBoundResourcePaged flow failed")
    }
}

/**
 * Extensión avanzada para refrescar manualmente los datos.
 * Ideal para ViewModels que necesiten `refresh` desde UI.
 */
suspend fun <V : Any> refreshNetworkBoundPaged(
    fetch: suspend (page: Int, pageSize: Int) -> List<V>,
    saveFetchResult: suspend (List<V>) -> Unit,
    clearLocalData: (suspend () -> Unit)? = null,
    pageSize: Int = 20
) {
    try {
        clearLocalData?.invoke()
        val remoteData = fetch(0, pageSize)
        if (remoteData.isNotEmpty()) saveFetchResult(remoteData)
    } catch (e: Exception) {
        print("Error en refresh manual de NetworkBoundResourcePaged: ${e.message}")
        AppSentry.capture(e, "refreshNetworkBoundPaged failed")
        throw e
    }
}
