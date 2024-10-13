package com.ssafy.yoganavi.ui.homeUI.teacher.teacherDetail

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.ssafy.yoganavi.data.source.dto.teacher.TeacherData
import com.ssafy.yoganavi.data.source.dto.teacher.TeacherDetailData
import com.ssafy.yoganavi.databinding.FragmentTeacherDetailBinding
import com.ssafy.yoganavi.ui.core.BaseFragment
import com.ssafy.yoganavi.ui.homeUI.teacher.teacherDetail.teacherDetail.TeacherDetailAdapter
import com.ssafy.yoganavi.ui.utils.TEACHER_DETAIL
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class TeacherDetailFragment : BaseFragment<FragmentTeacherDetailBinding>(
    FragmentTeacherDetailBinding::inflate
) {

    private val viewModel: TeacherDetailViewModel by viewModels()
    private val args by navArgs<TeacherDetailFragmentArgs>()
    private val teacherDetailAdapter by lazy {
        TeacherDetailAdapter(
            goReserve = ::goReserve,
            navigateToLectureDetailFragment = ::navigateToLectureDetailFragment,
            sendLikeLecture = ::sendLikeLecture,
            loadS3Image = ::loadS3Image,
            loadS3ImageSequentially = ::loadS3ImageSequentially
        )
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.rvTeacherDetail.adapter = teacherDetailAdapter
        getTeacherDetail()
    }

    override fun onStart() {
        super.onStart()
        setToolbar(false, TEACHER_DETAIL, true)
    }

    private fun getTeacherDetail() = viewModel.getTeacherDetail(args.userId, ::bindData)
    private suspend fun bindData(data: TeacherDetailData) = withContext(Dispatchers.Main) {
        val itemList: MutableList<TeacherDetailItem> = mutableListOf()
        val header = TeacherData(
            teacherName = data.teacherName,
            teacherId = data.teacherId,
            content = data.content ?: "",
            profileKey = data.profileKey ?: "",
            smallProfileKey = data.smallProfileKey ?: "",
            hashtags = data.hashtags,
            liked = data.liked,
            likes = data.likes
        )
        itemList.add(TeacherDetailItem.Header(header))
        //강의 추가
        if (data.teacherRecorded.isNotEmpty())
            itemList.add(TeacherDetailItem.LectureItem(data.teacherRecorded))
        if (data.teacherNotice.isNotEmpty()) {
            val header2 = TeacherData(
                teacherName = "공지사항",
                teacherId = -1,
                content = "",
                profileKey = "",
                smallProfileKey = "",
                hashtags = arrayListOf(),
                liked = false,
                likes = 0
            )
            itemList.add(TeacherDetailItem.Header(header2))
        }
        data.teacherNotice.forEach {
            itemList.add(TeacherDetailItem.NoticeItem(it))
        }
        teacherDetailAdapter.submitList(itemList)
    }

    private fun goReserve(
        teacherId: Int,
        teacherName: String,
        hashtags: String,
        teacherSmallProfile: String
    ) {
        val directions =
            TeacherDetailFragmentDirections.actionTeacherDetailFragmentToTeacherReservationFragment(
                teacherId,
                teacherName,
                hashtags,
                teacherSmallProfile
            )
        findNavController().navigate(directions)
    }

    private fun navigateToLectureDetailFragment(recordedId: Long = -1, teacherName: String) {
        val directions =
            TeacherDetailFragmentDirections.actionTeacherDetailFragmentToLectureDetailFragment(
                recordedId = recordedId,
                teacher = teacherName
            )
        findNavController().navigate(directions)
    }

    private fun sendLikeLecture(lectureId: Long) {
        viewModel.likeLecture(lectureId)
    }

    private fun loadS3Image(view: ImageView, key: String) = viewModel.loadS3Image(view, key)

    private fun loadS3ImageSequentially(view: ImageView, smallKey: String, largeKey: String) =
        viewModel.loadS3ImageSequentially(view, smallKey, largeKey)
}