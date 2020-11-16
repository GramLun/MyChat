package com.moskofidi.mychat.room

import android.graphics.Bitmap
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "users")
class UserEntity(id: String, name: String, email: String, profilePic: Bitmap) {
    @PrimaryKey
    var id: String? = null
    var name: String? = null
    var email: String? = null

    @ColumnInfo(typeAffinity = ColumnInfo.BLOB)
    var profilePic: ByteArray? = null
}