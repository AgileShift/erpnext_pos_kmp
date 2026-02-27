package com.erpnext.pos.views.login

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.erpnext.pos.remoteSource.dto.TokenResponse
import org.koin.compose.koinInject

class LoginCoordinator(
    val viewModel: LoginViewModel
) {
    val screenStateFlow = viewModel.stateFlow

    fun existingSites() {
        return viewModel.fetchSites()
    }

    fun onSiteSelected(site: Site) {
        return viewModel.onSiteSelected(site)
    }

    fun onAddSite(site: String) {
        viewModel.onAddSite(site)
    }

    fun onToggleFavorite(site: Site) {
        viewModel.toggleFavorite(site)
    }

    fun onDeleteSite(site: Site) {
        viewModel.deleteSite(site)
    }

    fun onError(error: String) {
        viewModel.onError(error)
    }

    fun clear() {
        viewModel.clear()
    }

    fun onReset() {
        viewModel.reset()
    }

    fun isAuthenticated(tokens: TokenResponse) {
        //viewModel.isAuthenticated(tokens)
    }
}

@Composable
fun rememberLoginCoordinator(): LoginCoordinator {
    val viewModel: LoginViewModel = koinInject()

    return remember(viewModel) {
        LoginCoordinator(
            viewModel = viewModel
        )
    }
}
