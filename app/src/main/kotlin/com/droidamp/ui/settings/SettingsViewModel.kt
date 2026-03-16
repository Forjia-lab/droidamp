package com.droidamp.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.droidamp.data.api.ServerUrlProvider
import com.droidamp.data.repository.NavidromeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val serverUrlProvider: ServerUrlProvider,
    private val repo: NavidromeRepository,
) : ViewModel() {

    private val _url      = MutableStateFlow(serverUrlProvider.baseUrl())
    private val _username = MutableStateFlow(serverUrlProvider.username())
    private val _password = MutableStateFlow(serverUrlProvider.password())
    private val _pingStatus = MutableStateFlow<String?>(null)

    val url        = _url.asStateFlow()
    val username   = _username.asStateFlow()
    val password   = _password.asStateFlow()
    val pingStatus = _pingStatus.asStateFlow()

    fun setUrl(v: String)      { _url.value = v; _pingStatus.value = null }
    fun setUsername(v: String) { _username.value = v; _pingStatus.value = null }
    fun setPassword(v: String) { _password.value = v; _pingStatus.value = null }

    fun save() {
        serverUrlProvider.update(_url.value.trim(), _username.value.trim(), _password.value)
        _pingStatus.value = "connecting…"
        viewModelScope.launch {
            val ok = repo.ping().getOrNull() ?: false
            _pingStatus.value = if (ok) "✓ connected" else "✗ connection failed"
        }
    }
}
