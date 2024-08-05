package com.ssafy.yoganavi.ui.homeUI.myPage.managementLive

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.ssafy.yoganavi.data.source.dto.live.LiveLectureData
import com.ssafy.yoganavi.databinding.ListItemLiveBinding
import com.ssafy.yoganavi.ui.utils.UPDATE
import com.ssafy.yoganavi.ui.utils.convertDaysToHangle
import com.ssafy.yoganavi.ui.utils.formatDotDate
import com.ssafy.yoganavi.ui.utils.formatTime
import com.ssafy.yoganavi.ui.utils.startTildeEnd
import com.ssafy.yoganavi.ui.utils.startVerticalEnd

class ManagementLiveAdapter(
    private val navigateToLiveFragment: (Int, Boolean) -> Unit,
    private val navigateToRegisterFragment: (String, Int) -> Unit,
    private val deleteLive: (Int) -> Unit,
) : ListAdapter<LiveLectureData, ManagementLiveAdapter.ViewHolder>(LiveDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent, navigateToLiveFragment, navigateToRegisterFragment, deleteLive)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(currentList[position])
    }

    class ViewHolder(
        private val binding: ListItemLiveBinding,
        private val navigateToLiveFragment: (Int, Boolean) -> Unit,
        private val navigateToRegisterFragment: (String, Int) -> Unit,
        private val deleteLive: (Int) -> Unit
    ) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(item: LiveLectureData) {

            val date = "${formatDotDate(item.startDate)} ~ ${formatDotDate(item.endDate)}"

            with(binding) {
                tvDate.text = date

                tvLectureTitle.text = item.liveTitle

                val weekData = convertDaysToHangle(item.availableDay)

                val timeData = startTildeEnd(formatTime(item.startTime), formatTime(item.endTime))

                tvLectureTime.text = startVerticalEnd(weekData, timeData)

                vEnterBtn.setOnClickListener { navigateToLiveFragment(item.liveId, item.isTeacher) }

                tvEditBtn.setOnClickListener { navigateToRegisterFragment(UPDATE, item.liveId) }

                tvDeleteBtn.setOnClickListener { deleteLive(item.liveId) }
            }
        }

        companion object {
            fun from(parent: ViewGroup,
                     navigateToLiveFragment: (Int, Boolean) -> Unit,
                     navigateToUpdateFragment: (String, Int) -> Unit,
                     deleteLive: (Int) -> Unit): ViewHolder {
                return ViewHolder(
                    ListItemLiveBinding.inflate(LayoutInflater.from(parent.context), parent, false),
                    navigateToLiveFragment = navigateToLiveFragment,
                    navigateToRegisterFragment = navigateToUpdateFragment,
                    deleteLive = deleteLive
                )
            }
        }
    }
}

class LiveDiffCallback : DiffUtil.ItemCallback<LiveLectureData>() {
    override fun areItemsTheSame(oldItem: LiveLectureData, newItem: LiveLectureData): Boolean {
        return oldItem.liveId == newItem.liveId
    }

    override fun areContentsTheSame(oldItem: LiveLectureData, newItem: LiveLectureData): Boolean {
        return oldItem == newItem
    }
}