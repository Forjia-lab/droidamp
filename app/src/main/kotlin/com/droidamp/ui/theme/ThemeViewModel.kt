package com.droidamp.ui.theme

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class ThemeViewModel @Inject constructor() : ViewModel() {
    private val _theme = MutableStateFlow(DroidThemes.Catppuccin)
    val theme: StateFlow<DroidTheme> = _theme.asStateFlow()
    fun setTheme(t: DroidTheme) { _theme.value = t }
}
