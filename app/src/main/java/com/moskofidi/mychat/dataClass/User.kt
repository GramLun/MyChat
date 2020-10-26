package com.moskofidi.mychat.dataClass

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
class User(
    var id: String = "",
    var name: String = "",
    var email: String = "",
    var profilePic: String = ""
) : Parcelable