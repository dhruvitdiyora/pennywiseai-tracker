package com.pennywiseai.tracker.presentation.panels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pennywiseai.tracker.data.preferences.HomePanel
import com.pennywiseai.tracker.data.preferences.HomePanelLayout
import com.pennywiseai.tracker.data.preferences.HomePanelState
import com.pennywiseai.tracker.data.preferences.UserPreferencesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomePanelsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    val panels: StateFlow<List<HomePanelState>> = userPreferencesRepository.homePanelLayout
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomePanelLayout.DEFAULT)

    fun setEnabled(panel: HomePanel, enabled: Boolean) = mutate {
        HomePanelLayout.setEnabled(it, panel, enabled)
    }

    fun move(from: Int, to: Int) = mutate { HomePanelLayout.reorder(it, from, to) }

    fun resetToDefault() {
        viewModelScope.launch { userPreferencesRepository.resetHomePanelLayout() }
    }

    /**
     * Reads the persisted layout before transforming it, rather than the cached [panels]
     * value: two edits made in quick succession would otherwise both start from the same
     * stale snapshot and the first would be lost.
     */
    private fun mutate(transform: (List<HomePanelState>) -> List<HomePanelState>) {
        viewModelScope.launch {
            val current = userPreferencesRepository.homePanelLayout.first()
            userPreferencesRepository.updateHomePanelLayout(transform(current))
        }
    }
}
