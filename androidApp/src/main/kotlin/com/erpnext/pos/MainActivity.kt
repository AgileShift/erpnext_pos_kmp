package com.erpnext.pos

import AppTheme
import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.erpnext.pos.printing.AndroidBluetoothPermissionHelper
import com.erpnext.pos.utils.AppLogger
import com.erpnext.pos.utils.PdfSavePickerBridge
import com.erpnext.pos.views.login.LoginViewModel
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {

  private val loginViewModel: LoginViewModel by viewModel()
  private val runtimePermissionLauncher =
      registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        val denied = result.filterValues { granted -> !granted }.keys
        if (denied.isEmpty()) {
          AppLogger.info("MainActivity runtime permissions granted")
        } else {
          AppLogger.warn(
              "MainActivity runtime permissions denied: ${denied.joinToString()}",
              reportToSentry = false,
          )
          AlertDialog.Builder(this)
              .setTitle("Permisos pendientes")
              .setMessage(
                  "La app seguirá funcionando, pero Bluetooth, notificaciones o diagnóstico local pueden quedar limitados hasta que concedas los permisos requeridos."
              )
              .setPositiveButton("Entendido", null)
              .show()
        }
      }
  private val bluetoothEnableLauncher =
      registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        val enabled = BluetoothAdapter.getDefaultAdapter()?.isEnabled == true
        if (!enabled) {
          AlertDialog.Builder(this)
              .setTitle("Bluetooth apagado")
              .setMessage(
                  "La impresión por Bluetooth requiere que enciendas Bluetooth. Puedes usar impresión por red (TCP) mientras tanto."
              )
              .setPositiveButton("Entendido", null)
              .show()
        }
      }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    WindowCompat.setDecorFitsSystemWindows(window, false)
    enableEdgeToEdge()
    PdfSavePickerBridge.register(this)

    setContent { AppTheme { AppNavigation() } }

    requestStartupPermissionsIfNeeded()
    promptBluetoothEnableIfNeeded()
    handleIntent(intent)
  }

  override fun onNewIntent(intent: Intent) {
    super.onNewIntent(intent)
    handleIntent(intent)
  }

  override fun onDestroy() {
    PdfSavePickerBridge.unregister()
    super.onDestroy()
  }

  override fun onResume() {
    super.onResume()
    promptBluetoothEnableIfNeeded()
  }

  private fun handleIntent(intent: Intent) {
    val uri = intent.data
    if (uri != null && uri.scheme == "org.erpnext.pos" && uri.host == "oauth2redirect") {
      val error = uri.getQueryParameter("error")
      val errorDescription = uri.getQueryParameter("error_description")
      if (!error.isNullOrBlank()) {
        loginViewModel.onError(errorDescription ?: error)
        return
      }

      val code = uri.getQueryParameter("code")
      val state = uri.getQueryParameter("state")
      if (!code.isNullOrBlank()) {
        loginViewModel.onAuthCodeReceived(code, state)
      }
    }
  }

  private fun requestStartupPermissionsIfNeeded() {
    val permissions = buildList {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.POST_NOTIFICATIONS)
      }
      addAll(AndroidBluetoothPermissionHelper.requiredPermissions())
    }
    val missing =
        permissions.distinct().filter { permission ->
          ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED
        }
    if (missing.isEmpty()) {
      AppLogger.info("MainActivity startup permissions already granted")
      return
    }

    AppLogger.warn(
        "MainActivity requesting startup permissions: ${missing.joinToString()}",
        reportToSentry = false,
    )
    AlertDialog.Builder(this)
        .setTitle("Permisos necesarios")
        .setMessage(
            "Necesitamos permisos de Bluetooth y notificaciones para descubrir impresoras, imprimir por Bluetooth y mostrar eventos importantes del POS."
        )
        .setNegativeButton("Ahora no") { _, _ ->
          AppLogger.warn("MainActivity startup permissions postponed by user", reportToSentry = false)
        }
        .setPositiveButton("Continuar") { _, _ ->
          runtimePermissionLauncher.launch(missing.toTypedArray())
        }
        .show()
  }

  private fun promptBluetoothEnableIfNeeded() {
    val adapter = BluetoothAdapter.getDefaultAdapter() ?: return
    val connectPermission =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
          Manifest.permission.BLUETOOTH_CONNECT
        } else {
          Manifest.permission.BLUETOOTH
        }
    val hasPermission =
        ActivityCompat.checkSelfPermission(this, connectPermission) ==
            PackageManager.PERMISSION_GRANTED
    if (!hasPermission || adapter.isEnabled) return

    AlertDialog.Builder(this)
        .setTitle("Bluetooth apagado")
        .setMessage(
            "Para descubrir e imprimir con impresoras Bluetooth, necesitamos encender Bluetooth."
        )
        .setNegativeButton("Ahora no", null)
        .setPositiveButton("Encender Bluetooth") { _, _ ->
          runCatching {
                bluetoothEnableLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
              }
              .onFailure {
                AppLogger.warn("MainActivity unable to request Bluetooth enable", it, reportToSentry = false)
              }
        }
        .show()
  }
}
