package com.moskofidi.mychat.diffUtil

import androidx.recyclerview.widget.DiffUtil
import com.moskofidi.mychat.room.UserEntity

class mDiffUtil(private val oldList: List<UserEntity>, private val newList: List<UserEntity>): DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldList.size
    }

    override fun getNewListSize(): Int {
        return newList.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }

}