package com.moskofidi.mychat.dataClass

data class Message(
    var id: String = "",
    var senderId: String = "",
    var receiverId: String = "",
    var text: String = "",
    var read: Boolean = false,
    var time: Long = System.currentTimeMillis()
)

enum class TypeMsg {
    MSG_IN_READ, MSG_OUT_READ, MSG_OUT_UNREAD
}

enum class TypeChat {
    MSG_IN_READ, MSG_IN_UNREAD, MSG_OUT_READ, MSG_OUT_UNREAD
}