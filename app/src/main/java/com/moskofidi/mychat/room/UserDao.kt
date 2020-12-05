package com.moskofidi.mychat.room

import androidx.lifecycle.LiveData
import androidx.room.*
import java.util.concurrent.Flow
import com.moskofidi.mychat.room.UserEntity as UserEntity

@Dao
interface UserDao {
    @Query("SELECT * FROM users")
    fun getAll() : Flow<List<UserEntity?>>

    @Query("SELECT * FROM users WHERE :id = :id")
    fun getById(id: Long): UserEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(employee: UserEntity?)

    @Update
    fun update(employee: UserEntity?)

    @Delete
    fun delete(employee: UserEntity?)
}