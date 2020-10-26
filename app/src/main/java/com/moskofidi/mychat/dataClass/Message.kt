package com.moskofidi.mychat.dataClass

import java.util.*

data class Message(
    var id: String = "",
    var senderId: String = "",
    var text: String = "",
    var receiverId: String = "",
    var isRead: Boolean = false,
    var time: Long = System.currentTimeMillis()
)