package com.ssafy.yoganavi.ui.homeUI.myPage.registerLive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ssafy.yoganavi.data.repository.InfoRepository
import com.ssafy.yoganavi.data.source.live.LiveLectureData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class RegisterLiveViewModel @Inject constructor(
    private val infoRepository: InfoRepository
) : ViewModel() {

    var liveLectureData = LiveLectureData()

    fun getLive(liveId: Int, onSuccess: () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        runCatching { infoRepository.getLive(liveId) }
            .onSuccess {
                it.data?.let { data ->
                    liveLectureData = data
                    onSuccess()
                }
            }
            .onFailure { it.printStackTrace() }
    }

    fun createLive(popBack: suspend () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            infoRepository.createLive(liveLectureData) }
            .onSuccess { popBack() }
            .onFailure { it.printStackTrace() }
    }

    fun updateLive(popBack: suspend () -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            infoRepository.updateLive(liveLectureData, liveLectureData.liveId) }
            .onSuccess { popBack() }
            .onFailure { it.printStackTrace() }
    }
}
