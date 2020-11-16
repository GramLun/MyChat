package com.moskofidi.mychat.room

import android.app.Application
import androidx.room.Room
import com.moskofidi.mychat.dataClass.User
import kotlinx.coroutines.InternalCoroutinesApi

@InternalCoroutinesApi
class App : Application() {

    var instance: App? = null
    var database: UserDatabase? = null

    override fun onCreate() {
        super.onCreate()
        instance = this
        database = Room.databaseBuilder(this, UserDatabase::class.java, "users.db").build()
    }

    fun instance(): App? {
        return instance
    }

    fun database(): UserDatabase? {
        return database
    }
}