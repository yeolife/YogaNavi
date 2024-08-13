package com.ssafy.yoganavi.ui.homeUI.myPage.modify

import android.widget.ImageView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.services.s3.AmazonS3Client
import com.amazonaws.services.s3.model.ObjectMetadata
import com.ssafy.yoganavi.data.repository.info.InfoRepository
import com.ssafy.yoganavi.data.repository.response.DetailResponse
import com.ssafy.yoganavi.data.source.dto.mypage.Profile
import com.ssafy.yoganavi.ui.utils.LOGO
import com.ssafy.yoganavi.ui.utils.MINI
import com.ssafy.yoganavi.ui.utils.loadS3Image
import com.ssafy.yoganavi.ui.utils.uploadFile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class ModifyViewModel @Inject constructor(
    private val infoRepository: InfoRepository,
    private val transferUtility: TransferUtility,
    private val s3Client: AmazonS3Client
) : ViewModel() {

    private val _hashtagList: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())
    val hashtagList: StateFlow<Set<String>> = _hashtagList.asStateFlow()
    var profile = Profile()

    fun getProfile(bindData: suspend (Profile) -> Unit) = viewModelScope.launch(Dispatchers.IO) {
        runCatching {
            infoRepository.getProfile()
        }.onSuccess {
            it.data?.let { data ->
                profile = profile.copy(
                    nickname = data.nickname,
                    imageKey = data.imageKey,
                    smallImageKey = data.smallImageKey,
                    teacher = data.teacher,
                    content = data.content
                )
                _hashtagList.emit(data.hashTags?.toSet() ?: emptySet())
                bindData(data)
            }
        }.onFailure {
            it.printStackTrace()
        }
    }

    fun addHashTag(hashTag: String) = viewModelScope.launch(Dispatchers.IO) {
        val newList = hashtagList.value.toMutableSet()
        newList.add(hashTag)
        _hashtagList.emit(newList)
    }

    fun deleteHashTag(index: Int) = viewModelScope.launch(Dispatchers.IO) {
        val newList = hashtagList.value.toMutableList()
        newList.removeAt(index)
        _hashtagList.emit(newList.toSet())
    }

    fun modifyProfile(
        nickname: String,
        password: String,
        content: String,
        isModified: (DetailResponse<Profile>) -> Unit,
        showLoadingView: suspend () -> Unit,
        uploadFail: suspend () -> Unit
    ) = viewModelScope.launch(Dispatchers.IO) {
        showLoadingView()

        profile = profile.copy(
            nickname = nickname,
            password = password,
            content = content,
            hashTags = hashtagList.value.toList()
        )

        if (profile.logoPath.isNotBlank()) {
            val thumbnailFile = File(profile.logoPath)
            val miniFile = File(profile.logoSmallPath)
            val metadata = ObjectMetadata().apply { contentType = "image/webp" }

            val uploadResults = awaitAll(
                async { uploadFile(transferUtility, profile.imageKey, thumbnailFile, metadata) },
                async { uploadFile(transferUtility, profile.smallImageKey, miniFile, metadata) }
            )

            if (uploadResults.any { false }) {
                uploadFail()
                return@launch
            }

        }

        runCatching { infoRepository.updateProfile(profile) }
            .onSuccess { isModified(it) }
            .onFailure { uploadFail() }
    }

    fun setThumbnail(path: String, miniPath: String) = viewModelScope.launch(Dispatchers.IO) {
        val uuid = UUID.randomUUID()
        val logoKey = "$LOGO/$uuid"
        val miniKey = "$LOGO/$MINI/$uuid"

        profile = profile.copy(
            imageKey = logoKey,
            smallImageKey = miniKey,
            logoPath = path,
            logoSmallPath = miniPath
        )
    }

    fun loadS3Image(view: ImageView, key: String) = view.loadS3Image(key, s3Client)
}
