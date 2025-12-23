package com.erpnext.pos.views.login

import AppTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.erpnext.pos.utils.isValidUrlInput
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun LoginScreen(
    state: LoginState, actions: LoginAction
) {
    var siteUrl by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        actions.existingSites()
    }

    Column(
        modifier = Modifier.fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Top
    ) {
        Text(
            text = "ERPNext POS",
            color = MaterialTheme.colorScheme.onSurface,
            style = MaterialTheme.typography.headlineMedium.copy( // Título más grande
                fontWeight = FontWeight.Bold, // Más énfasis en el título
                letterSpacing = 0.5.sp
            )
        )
        Text(
            text = "Inicio de sesion",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.titleSmall.copy( // Título más grande
                fontWeight = FontWeight.Bold, // Más énfasis en el título
                letterSpacing = 0.5.sp
            )
        )

        when (state) {
            is LoginState.Loading -> {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(48.dp),
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 4.dp
                )
            }

            is LoginState.Success -> {
                val sites = state.sites
                if (!sites.isNullOrEmpty()) {
                    Text(
                        text = "Selecciona un sitio existente",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                            .padding(bottom = 12.dp)
                    )

                    sites.forEach { site ->
                        SiteCard(
                            site = site,
                            onClick = { actions.onSiteSelected(site) },
                            modifier = Modifier.fillMaxWidth()
                                .padding(end = 8.dp, start = 8.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HorizontalDivider(
                            modifier = Modifier.weight(1f), thickness = 0.5.dp,
                            Color.Gray
                        )
                        Text(
                            text = "o",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(8.dp)
                        )
                        HorizontalDivider(
                            modifier = Modifier.weight(1f),
                            thickness = 0.5.dp,
                            color = Color.Gray
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 18.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    UrlInputField(
                        url = siteUrl,
                        onUrlChanged = { siteUrl = it },
                    )
                }

                Row( //top = 8.dp, start = 12.dp, end = 12.dp, bottom = 8.dp
                    modifier = Modifier.fillMaxWidth()
                        .padding(vertical = 36.dp, horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = {
                            if (siteUrl.isNotBlank()) actions.onAddSite(siteUrl)
                        },
                        enabled = siteUrl.isNotBlank(),
                        modifier = Modifier.fillMaxWidth()
                            .padding(top = 12.dp, start = 10.dp, end = 10.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(text = "Ingresar")
                    }
                }
            }

            is LoginState.Authenticated -> {
                actions.isAuthenticated(state.tokens)
            }

            is LoginState.Error -> {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = state.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = { actions.onReset() }) {
                        Text(text = "Reintentar")
                    }
                }
            }
        }
    }
}

@Composable
fun SiteCard(
    site: Site, onClick: () -> Unit, modifier: Modifier = Modifier
) {
    Card(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(site.name, style = MaterialTheme.typography.titleMedium)
            Text(site.url, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun UrlInputField(
    url: String,
    onUrlChanged: (String) -> Unit,
) {
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    OutlinedTextField(
        value = url,
        onValueChange = { input ->
            onUrlChanged(input.trim())
            isError = !isValidUrlInput(url)
            errorMessage = if (isError) "URL inválida, debe ser https://ejemplo.com" else ""
        },
        label = { Text("URL del Sitio") },
        placeholder = { Text("https://erp.frappe.cloud") },
        isError = isError,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.5.sp
        ),
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Language,
                contentDescription = null,
                tint = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
            )
        },
        supportingText = {
            if (isError) {
                Text(
                    text = errorMessage,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    text = "Ingrese la URL completa de su instancia",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
    )
}

@Composable
@Preview
fun LoginPreview() {
    AppTheme {
        LoginScreen(
            state = LoginState.Success(emptyList()),
            actions = LoginAction()
        )
    }
}