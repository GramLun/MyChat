package com.moskofidi.mychat.room

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.internal.synchronized

@InternalCoroutinesApi
@Database(entities = [UserEntity::class], version = 1)
abstract class UserDatabase : RoomDatabase() {

//    @Volatile
//    private var INSTANCE: UserDatabase? = null

//    fun instance(context: Context): UserDatabase {
//        synchronized(this) {
//            if (INSTANCE == null) {
//                INSTANCE =
//                    Room.databaseBuilder(context, UserDatabase::class.java, "users.db").build()
//            }
//            return INSTANCE as UserDatabase
//        }
//    }

    abstract fun userDao(): UserDao?
}