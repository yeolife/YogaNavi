package com.ssafy.yoganavi.data.source.info

import com.ssafy.yoganavi.data.source.YogaDetailResponse
import com.ssafy.yoganavi.data.source.YogaResponse
import com.ssafy.yoganavi.data.source.lecture.LectureData
import com.ssafy.yoganavi.data.source.lecture.LectureDetailData
import com.ssafy.yoganavi.data.source.live.LiveLectureData
import retrofit2.Response

interface InfoDataSource {

    suspend fun getLectureList(): Response<YogaResponse<LectureData>>

    suspend fun getLecture(recordedId: Int): Response<YogaDetailResponse<LectureDetailData>>

    suspend fun getLiveList(): Response<YogaResponse<LiveLectureData>>

    suspend fun getLive(liveId: Int): Response<YogaDetailResponse<LiveLectureData>>
}