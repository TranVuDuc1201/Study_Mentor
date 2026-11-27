package com.example.study_mentor.data

enum class PowerUpType(
    val id: String,
    val title: String,
    val defaultCount: Long
) {
    REMOVE_ONE_WRONG(
        id = "removeOneWrongCount",
        title = "Xóa 1 đáp án sai",
        defaultCount = 2L
    )
    // Sau này chỉ cần thêm vào đây:
    // FREEZE_TIME("freezeTimeCount", "Đóng băng thời gian", 1L),
    // SECOND_CHANCE("secondChanceCount", "Thêm cơ hội", 1L)
}