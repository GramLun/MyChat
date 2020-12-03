package com.moskofidi.mychat.dataClass

import java.util.*

data class Message(
    var id: String = "",
    var senderId: String = "",
    var receiverId: String = "",
    var text: String = "",
    var read: Boolean = false,
    var time: Long = System.currentTimeMillis()
)