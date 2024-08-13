package com.ssafy.yoganavi.ui.homeUI.teacher.teacherReservation.availableList

import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.ssafy.yoganavi.data.source.dto.live.LiveLectureData
import com.ssafy.yoganavi.databinding.ListItemAvailableClassBinding
import com.ssafy.yoganavi.ui.utils.formatTime
import com.ssafy.yoganavi.ui.utils.parseDay
import com.ssafy.yoganavi.ui.utils.startTildeEnd

class AvailableViewHolder(
    private val binding: ListItemAvailableClassBinding
) : ViewHolder(binding.root) {
    fun bind(
        item: LiveLectureData,
        position: Int,
        selectedPosition: Int,
        onItemSelected: (Int) -> Unit,
        visibleCalendar: (Long, Long, Int) -> Unit
    ) {
        val classString =
            parseDay(item.availableDay) + " | " + item.liveTitle + " | " + startTildeEnd(
                formatTime(item.startTime),
                formatTime(item.endTime)
            )
        binding.rvAvailableClass.text = classString

        binding.rvAvailableClass.isChecked = position == selectedPosition
        binding.rvAvailableClass.setOnClickListener {
            visibleCalendar(item.startDate, item.endDate, item.liveId)
            onItemSelected(position)
        }
    }
}
