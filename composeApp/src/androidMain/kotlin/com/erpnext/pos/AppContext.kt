package com.erpnext.pos

import android.content.Context

object AppContext {
    private lateinit var appContext: Context
    fun init(context: Context) {
        appContext = context.applicationContext
    }

    fun get(): Context = appContext
}