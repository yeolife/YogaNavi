package com.ssafy.yoganavi.data.repository.ai

import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import com.ssafy.yoganavi.data.source.ai.KeyPoint
import com.ssafy.yoganavi.data.source.ai.PoseDataSource
import com.ssafy.yoganavi.di.DefaultDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PoseRepositoryImpl @Inject constructor(
    private val poseDataSource: PoseDataSource,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : PoseRepository {

    override suspend fun infer(image: ImageProxy, width: Int, height: Int): List<List<KeyPoint>> =
        withContext(defaultDispatcher) { poseDataSource.infer(image, width, height) }

    override suspend fun infer(bitmap: Bitmap, width: Int, height: Int): List<List<KeyPoint>> =
        withContext(defaultDispatcher) { poseDataSource.infer(bitmap, width, height) }

}