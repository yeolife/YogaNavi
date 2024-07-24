package com.ssafy.yoganavi.data.repository

import com.ssafy.yoganavi.data.repository.response.DetailResponse
import com.ssafy.yoganavi.data.repository.response.ListResponse
import com.ssafy.yoganavi.data.repository.response.toDetailResponse
import com.ssafy.yoganavi.data.repository.response.toListResponse
import com.ssafy.yoganavi.data.source.info.InfoDataSource
import com.ssafy.yoganavi.data.source.lecture.LectureData
import com.ssafy.yoganavi.data.source.lecture.LectureDetailData
import com.ssafy.yoganavi.data.source.live.LiveLectureData
import com.ssafy.yoganavi.data.source.live.RegisterLiveRequest
import com.ssafy.yoganavi.data.source.notice.NoticeData
import com.ssafy.yoganavi.data.source.notice.RegisterNoticeRequest
import com.ssafy.yoganavi.di.IoDispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InfoRepositoryImpl @Inject constructor(
    private val infoDataSource: InfoDataSource,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : InfoRepository {

    override suspend fun getLectureList(): ListResponse<LectureData> {
        val response = withContext(ioDispatcher) { infoDataSource.getLectureList() }
        return response.toListResponse()
    }

    override suspend fun createLecture(lecture: LectureDetailData): DetailResponse<Boolean> {
        val response = withContext(ioDispatcher) { infoDataSource.createLecture(lecture) }
        return response.toDetailResponse()
    }

    override suspend fun getLecture(recordId: Long): DetailResponse<LectureDetailData> {
        val response = withContext(ioDispatcher) { infoDataSource.getLecture(recordId) }
        return response.toDetailResponse()
    }


    override suspend fun updateLecture(lecture: LectureDetailData): DetailResponse<Boolean> {
        val response = withContext(ioDispatcher) { infoDataSource.updateLecture(lecture) }
        return response.toDetailResponse()
    }

    override suspend fun deleteLectures(recordIdList: List<Long>): DetailResponse<Boolean> {
        val response = withContext(ioDispatcher) { infoDataSource.deleteLectures(recordIdList) }
        return response.toDetailResponse()
    }

    override suspend fun likeLecture(recordedId: Long): DetailResponse<Boolean> {
        val response = withContext(ioDispatcher) { infoDataSource.likeLecture(recordedId) }

    // Live 시작
    override suspend fun getLiveList(): ListResponse<LiveLectureData> {
        val response = withContext(ioDispatcher) { infoDataSource.getLiveList() }
        return response.toListResponse()
    }

    override suspend fun createLive(registerLiveRequest: RegisterLiveRequest): DetailResponse<Unit> {
        val response = withContext(ioDispatcher) { infoDataSource.createLive(registerLiveRequest) }
        return response.toDetailResponse()
    }
        override suspend fun getLive(liveId: Int): DetailResponse<LiveLectureData> {
            val response = withContext(ioDispatcher) { infoDataSource.getLive(liveId) }
            return response.toDetailResponse()
        }

        override suspend fun updateLive(registerLiveRequest: RegisterLiveRequest, liveId: Int): DetailResponse<Unit> {
        val response = withContext(ioDispatcher) { infoDataSource.updateLive(registerLiveRequest, liveId) }
        return response.toDetailResponse()
    }

        override suspend fun deleteLive(liveId: Int): DetailResponse<Unit> {
        val response = withContext(ioDispatcher) { infoDataSource.deleteLive(liveId) }
        return response.toDetailResponse()
    }

    // Live 끝


    override suspend fun getNoticeList(): ListResponse<NoticeData> {
        val response = withContext(ioDispatcher) { infoDataSource.getNoticeList() }
        return response.toListResponse()
    }

    override suspend fun getNotice(articleId: Int): DetailResponse<NoticeData> {
        val response = withContext(ioDispatcher) { infoDataSource.getNotice(articleId) }
        return response.toDetailResponse()
    }

    override suspend fun insertNotice(registerNoticeRequest: RegisterNoticeRequest): DetailResponse<Unit> {
        val response =
            withContext(ioDispatcher) { infoDataSource.insertNotice(registerNoticeRequest) }
        return response.toDetailResponse()
    }

    override suspend fun updateNotice(
        registerNoticeRequest: RegisterNoticeRequest,
        articleId: Int
    ): DetailResponse<Unit> {
        val response = withContext(ioDispatcher) {
            infoDataSource.updateNotice(registerNoticeRequest, articleId)
        }
        return response.toDetailResponse()
    }

    override suspend fun deleteNotice(articleId: Int): DetailResponse<Unit> {
        val response = withContext(ioDispatcher) { infoDataSource.deleteNotice(articleId) }
        return response.toDetailResponse()
    }
}