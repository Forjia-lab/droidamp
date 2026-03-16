package com.droidamp.ui.settings

import androidx.lifecycle.ViewModel
import com.droidamp.data.api.ServerUrlProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val serverUrlProvider: ServerUrlProvider,
) : ViewModel() {

    private val _url      = MutableStateFlow(serverUrlProvider.baseUrl())
    private val _username = MutableStateFlow(serverUrlProvider.username())
    private val _password = MutableStateFlow(serverUrlProvider.password())

    val url      = _url.asStateFlow()
    val username = _username.asStateFlow()
    val password = _password.asStateFlow()

    fun setUrl(v: String)      { _url.value = v }
    fun setUsername(v: String) { _username.value = v }
    fun setPassword(v: String) { _password.value = v }

    fun save() {
        serverUrlProvider.update(_url.value.trim(), _username.value.trim(), _password.value)
    }
}
