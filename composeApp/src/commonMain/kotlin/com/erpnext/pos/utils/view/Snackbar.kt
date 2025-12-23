package com.erpnext.pos.utils.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class SnackbarType {
    Info, Success, Error, Loading
}

data class UiSnackbar(
    val message: String,
    val type: SnackbarType,
    val position: SnackbarPosition = SnackbarPosition.Bottom
)

enum class SnackbarPosition {
    Top, Bottom
}

class SnackbarController {
    private val _snackbar = MutableStateFlow<UiSnackbar?>(null)
    val snackbar = _snackbar.asStateFlow()

    fun show(
        message: String,
        type: SnackbarType,
        position: SnackbarPosition = SnackbarPosition.Bottom
    ) {
        _snackbar.value = UiSnackbar(message, type, position)
    }

    fun dismiss() {
        _snackbar.value = null
    }
}


//TODO: Necesitamos mejorar los efectos de entrada y salida, y la salida tien que ser automatica y/po al usuario hacer click
//TODO: Agregar la posibilidad de un boton de accion
@Composable
fun SnackbarHost(
    snackbar: UiSnackbar?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val alignment = when (snackbar?.position) {
        SnackbarPosition.Top -> Alignment.TopCenter
        else -> Alignment.BottomCenter
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(14.dp),
        contentAlignment = alignment
    ) {
        AnimatedVisibility(
            visible = snackbar != null,
            enter = slideInVertically { if (alignment == Alignment.TopCenter) -it else it } + fadeIn(),
            exit = slideOutVertically { if (alignment == Alignment.TopCenter) -it else it } + fadeOut()
        ) {
            snackbar?.let {
                ModernSnackbar(it, onDismiss)
            }
        }
    }
}

@Composable
fun ModernSnackbar(snackbar: UiSnackbar, onDismiss: () -> Unit) {
    val colors = when (snackbar.type) {
        SnackbarType.Success -> MaterialTheme.colorScheme.primary
        SnackbarType.Error -> MaterialTheme.colorScheme.error
        SnackbarType.Loading -> MaterialTheme.colorScheme.secondary
        SnackbarType.Info -> MaterialTheme.colorScheme.surfaceVariant
    }

    Surface(
        shape = RoundedCornerShape(24.dp),
        tonalElevation = 8.dp,
        color = colors.copy(alpha = 0.92f),
        modifier = Modifier
            .fillMaxWidth(0.92f)
            .clickable { onDismiss() }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (snackbar.type == SnackbarType.Loading) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Spacer(Modifier.width(12.dp))
            }

            Text(
                text = snackbar.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun rememberSnackbarController(): SnackbarController = remember { SnackbarController() }