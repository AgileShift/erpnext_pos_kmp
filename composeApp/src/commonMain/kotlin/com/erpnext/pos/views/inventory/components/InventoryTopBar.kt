package com.erpnext.pos.views.inventory.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.OnlinePrediction
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.animation.core.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryTopBar(
    onRefresh: () -> Unit,
    onPredictionClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false
) {
    TopAppBar(
        title = { Text(text = "Inventario") },
        actions = {
            // Rotación animada solo cuando isLoading = true
            val infiniteTransition = rememberInfiniteTransition(label = "refresh_rotation")

            val rotation by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 800, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation_anim"
            )

            // Si no está cargando, congelamos el ícono en 0 grados
            val currentRotation = if (isLoading) rotation else 0f

            IconButton(
                onClick = onRefresh,
                enabled = !isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Actualizar inventario",
                    modifier = Modifier.graphicsLayer {
                        rotationZ = currentRotation
                    }
                )
            }

            IconButton(onClick = { onPredictionClick?.invoke() }) {
                Icon(
                    imageVector = Icons.Default.OnlinePrediction,
                    contentDescription = "Predicciones"
                )
            }
        },
        modifier = modifier
    )
}

