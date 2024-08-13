package com.ssafy.yoganavi.ui.homeUI.teacher.teacherList.teacher

import androidx.recyclerview.widget.DiffUtil
import com.ssafy.yoganavi.data.source.dto.teacher.TeacherData

class TeacherItemCallback : DiffUtil.ItemCallback<TeacherData>() {
    override fun areItemsTheSame(oldItem: TeacherData, newItem: TeacherData): Boolean {
        return oldItem.teacherId == newItem.teacherId
    }

    override fun areContentsTheSame(oldItem: TeacherData, newItem: TeacherData): Boolean {
        return oldItem == newItem
    }

}