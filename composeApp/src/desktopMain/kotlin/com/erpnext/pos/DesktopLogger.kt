package com.erpnext.pos

import java.io.File
import java.util.logging.FileHandler
import java.util.logging.Level
import java.util.logging.Logger
import java.util.logging.SimpleFormatter

object DesktopLogger {
    private const val LOG_DIR_NAME = ".erpnext-pos/logs"
    private const val LOG_FILE_NAME = "app.log"
    private val logger: Logger = Logger.getLogger("ERPNextPOS")
    @Volatile private var initialized = false

    fun init() {
        if (initialized) return
        synchronized(this) {
            if (initialized) return
            runCatching {
                val logDir = File(System.getProperty("user.home"), LOG_DIR_NAME)
                logDir.mkdirs()
                val fileHandler = FileHandler(File(logDir, LOG_FILE_NAME).absolutePath, true)
                fileHandler.formatter = SimpleFormatter()
                logger.addHandler(fileHandler)
                logger.level = Level.ALL
                initialized = true
            }.onFailure {
                System.err.println("DesktopLogger init failed: ${it.message}")
                initialized = true
            }
        }
    }

    fun info(message: String) {
        if (!initialized) init()
        logger.info(message)
        com.erpnext.pos.utils.AppSentry.breadcrumb(message)
    }

    fun warn(message: String, throwable: Throwable? = null) {
        if (!initialized) init()
        if (throwable != null) logger.log(Level.WARNING, message, throwable) else logger.warning(message)
        if (throwable != null) {
            com.erpnext.pos.utils.AppSentry.capture(throwable, message)
        } else {
            com.erpnext.pos.utils.AppSentry.breadcrumb(message)
        }
    }
}
