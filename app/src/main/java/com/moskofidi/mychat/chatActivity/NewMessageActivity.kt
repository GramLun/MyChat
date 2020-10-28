package com.moskofidi.mychat.chatActivity

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.storage.FirebaseStorage
import com.moskofidi.mychat.R
import com.moskofidi.mychat.dataClass.User
import com.moskofidi.mychat.receiver.ConnectionListener
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_new_message.*
import kotlinx.android.synthetic.main.item_chat_in_row.view.*
import kotlinx.android.synthetic.main.item_user_row.view.*

class NewMessageActivity : AppCompatActivity() {

    private val connectionListener = ConnectionListener(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_new_message)

        connectionListener.result = { isAvailable ->
            runOnUiThread {
                when (isAvailable) {
                    true -> {
                        supportActionBar?.title = "Выберите контакт"
                    }
                    false -> {
                        supportActionBar?.title = "Ожидание сети..."
                    }
                }
            }
        }

        fetchUsers()
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onResume() {
        super.onResume()
        connectionListener.registerConnectionListener()
    }

    override fun onStop() {
        super.onStop()
        connectionListener.unregisterConnectionListener()
    }

    private fun fetchUsers() {
        val ref = FirebaseDatabase.getInstance().getReference("/users")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                new_message_progress_bar.visibility = View.GONE
                val adapter = GroupAdapter<ViewHolder>()

                snapshot.children.forEach {
                    val user = it.getValue(User::class.java)
                    if (user!!.id != FirebaseAuth.getInstance().currentUser!!.uid)
                        adapter.add(UserItem(user))
                }
                adapter.setOnItemClickListener { item, _ ->
                    val userItem = item as UserItem

                    startActivity(
                        Intent(
                            this@NewMessageActivity,
                            ChatActivity::class.java
                        ).putExtra(USER_KEY, userItem.user)
                    )
                    this@NewMessageActivity.finish()
                }

                list_of_users.adapter = adapter
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@NewMessageActivity,
                    "Loading the user list: failure",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }

    companion object {
        const val USER_KEY = "USER_KEY"
    }
}

class UserItem(val user: User) : Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.user_row.text = user.name

        val storageRef = FirebaseStorage.getInstance()
            .getReference("profile_pics/${user.id}")
        storageRef.getBytes(4000 * 4000)
            .addOnSuccessListener {
                val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                viewHolder.itemView.profilePic_user_row.setImageBitmap(bitmap)
            }
    }

    override fun getLayout(): Int {
        return R.layout.item_user_row
    }
}