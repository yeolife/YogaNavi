package com.ssafy.yoganavi.ui.homeUI.myPage.managementVideo.lecture

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.ListAdapter
import com.ssafy.yoganavi.data.source.dto.lecture.LectureData
import com.ssafy.yoganavi.databinding.ListItemLectureThumbnailBinding

class LectureAdapter(
    private val navigateToLectureDetailFragment: ((Long, String) -> Unit)? = null,
    private val navigateToRegisterVideoFragment: ((Long) -> Unit)? = null,
    private val sendLikeLecture: (Long) -> Unit,
    private val loadS3Image: (ImageView, String) -> Unit
) : ListAdapter<LectureData, LectureViewHolder>(LectureItemCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LectureViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemLectureThumbnailBinding.inflate(inflater, parent, false)

        return LectureViewHolder(
            binding = binding,
            navigateToLectureDetailFragment = navigateToLectureDetailFragment,
            navigateToRegisterVideoFragment = navigateToRegisterVideoFragment,
            sendLikeLecture = sendLikeLecture,
            loadS3Image = loadS3Image
        )
    }

    override fun onBindViewHolder(holder: LectureViewHolder, position: Int) {
        holder.bind(currentList[position])
    }
}
