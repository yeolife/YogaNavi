package com.ssafy.yoganavi.data.source.info

import com.ssafy.yoganavi.data.source.YogaDetailResponse
import com.ssafy.yoganavi.data.source.YogaResponse
import com.ssafy.yoganavi.data.source.lecture.LectureData
import com.ssafy.yoganavi.data.source.lecture.LectureDetailData
import com.ssafy.yoganavi.data.source.live.LiveLectureData
import com.ssafy.yoganavi.data.source.notice.NoticeData
import retrofit2.Response

interface InfoDataSource {

    suspend fun getLectureList(): Response<YogaResponse<LectureData>>

    suspend fun createLecture(lecture: LectureDetailData): Response<YogaDetailResponse<Boolean>>

    suspend fun getLecture(recordedId: Long): Response<YogaDetailResponse<LectureDetailData>>

    suspend fun updateLecture(lecture: LectureDetailData): Response<YogaDetailResponse<Boolean>>

    // live
    suspend fun getLiveList(): Response<YogaResponse<LiveLectureData>>

    suspend fun getLive(liveId: Int): Response<YogaDetailResponse<LiveLectureData>>

    suspend fun createLive(): Response<YogaDetailResponse<Unit>>

    suspend fun updateLive(liveId: Int): Response<YogaDetailResponse<Unit>>

    suspend fun deleteLive(liveId: Int): Response<YogaDetailResponse<Unit>>

    suspend fun getNoticeList(): Response<YogaResponse<NoticeData>>
}