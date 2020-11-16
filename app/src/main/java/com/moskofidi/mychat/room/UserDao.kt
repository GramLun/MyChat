package com.moskofidi.mychat.room

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface UserDao {
    @get:Query("SELECT * FROM users")
    val readAll: List<UserEntity?>

    @Query("SELECT * FROM users WHERE :id = :id")
    fun getById(id: Long): UserEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(employee: UserEntity?)

    @Update
    fun update(employee: UserEntity?)

    @Delete
    fun delete(employee: UserEntity?)
}