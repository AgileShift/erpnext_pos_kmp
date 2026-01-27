package com.erpnext.pos.localSource.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import okio.Path.Companion.toPath

object PreferenceStoreProvider {
    private val lock = Any()
    private val stores = mutableMapOf<String, DataStore<Preferences>>()

    fun get(path: String): DataStore<Preferences> {
        return synchronized(lock) {
            stores.getOrPut(path) {
                PreferenceDataStoreFactory.createWithPath { path.toPath() }
            }
        }
    }
}
