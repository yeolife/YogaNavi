package com.ssafy.yoganavi.data.source.dto.teacher

import com.google.gson.annotations.SerializedName

data class TeacherData(

    @SerializedName("nickname")
    val teacherName: String,
    @SerializedName("profileImageUrl")
    val profileKey: String? = "",
    @SerializedName("profileImageUrlSmall")
    val smallProfileKey: String? = "",
    @SerializedName("id")
    val teacherId: Int,
    @SerializedName("hashtags")
    val hashtags: List<String>,
    val content: String?,
    val liked: Boolean,
    @SerializedName("likeCount")
    val likes: Int,
)