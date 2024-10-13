package com.ssafy.yoganavi.ui.homeUI.myPage.managementLive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.yoganavi.data.repository.info.InfoRepository
import com.ssafy.yoganavi.data.source.dto.live.LiveLectureData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManagementLiveViewModel @Inject constructor(
    private val infoRepository: InfoRepository
) : ViewModel() {
    private val _liveList = MutableStateFlow<List<LiveLectureData>>(emptyList())
    val liveList = _liveList.asStateFlow()

    fun getLiveList() = viewModelScope.launch(Dispatchers.IO) {
        runCatching { infoRepository.getLiveList() }
            .onSuccess { _liveList.emit(it.data.toMutableList()) }
            .onFailure { it.printStackTrace() }
    }

    fun deleteLive(
        liveId: Int,
        onDeleteComplete: suspend () -> Unit,
    ) = viewModelScope.launch(Dispatchers.IO) {
        runCatching { infoRepository.deleteLive(liveId) }
            .onSuccess { onDeleteComplete() }
            .onFailure { it.printStackTrace() }
    }
}
