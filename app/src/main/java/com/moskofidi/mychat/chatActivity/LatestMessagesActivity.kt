package com.moskofidi.mychat.chatActivity

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.moskofidi.mychat.R
import com.moskofidi.mychat.chatActivity.ChatInItem
import com.moskofidi.mychat.dataClass.Message
import com.moskofidi.mychat.dataClass.User
import com.moskofidi.mychat.receiver.ConnectionListener
import com.moskofidi.mychat.signInActivity.RegisterActivity
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_latest_messages.*
import kotlinx.android.synthetic.main.item_chat_in_row.view.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

class LatestMessagesActivity : AppCompatActivity() {

    private val adapter = GroupAdapter<ViewHolder>()
    private val connectionListener = ConnectionListener(this)
    private val latestMessagesMap = HashMap<String, Message>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_latest_messages)
        actionBar?.setDisplayShowCustomEnabled(true)
//        supportActionBar?.title = FirebaseAuth.getInstance().currentUser!!.displayName

        connectionListener.result = { isAvailable ->
            runOnUiThread {
                when (isAvailable) {
                    true -> {
                        supportActionBar?.title = "MyChat"
                    }
                    false -> {
                        supportActionBar?.title = "Ожидание сети..."
                    }
                }
            }
        }

        list_of_chats.adapter = adapter
        adapter.setOnItemClickListener { item, button ->
            val row = item as ChatInItem
//            button.isClickable = false
            startActivity(
                Intent(
                    this,
                    ChatActivity::class.java
                ).putExtra(NewMessageActivity.USER_KEY, row.chatUser)
            )
        }

        fetchChats()
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

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.nav_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.btnNewMessage -> {
                startActivity(Intent(this, NewMessageActivity::class.java))
            }
            R.id.btnSignOut -> {
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, RegisterActivity::class.java))
                this.finish()
            }
            R.id.btnDeleteAccount -> {
                val user = FirebaseAuth.getInstance().currentUser
                user?.delete()
                FirebaseDatabase.getInstance()
                    .getReference("/users/${user!!.uid}")
                    .removeValue()
                FirebaseDatabase.getInstance()
                    .getReference("/names/${user.uid}")
                    .removeValue()
                FirebaseStorage.getInstance()
                    .getReference("profile_pics/${user.uid}")
                    .delete()
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, RegisterActivity::class.java))
                this.finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun refreshChatView() {
        latest_empty.visibility = View.GONE
        adapter.clear()
        val user = FirebaseAuth.getInstance().currentUser
        latestMessagesMap.values.forEach {
            adapter.add(ChatInItem(it))
//            if (it.senderId != user?.uid)
//                adapter.add(ChatInItem(it))
//            else
//                adapter.add(ChatOutItem(it))
        }
    }

    private fun fetchChats() {
        val user = FirebaseAuth.getInstance().currentUser

        val ref = FirebaseDatabase.getInstance().getReference("/latest_messages/${user!!.uid}")
        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java) ?: return
                latestMessagesMap[snapshot.key!!] = message
                refreshChatView()
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java) ?: return
                latestMessagesMap[snapshot.key!!] = message
                refreshChatView()
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                TODO("Not yet implemented")
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(
                    this@LatestMessagesActivity,
                    "Loading the chat list: failure",
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}

class ChatInItem(private val message: Message) : Item<ViewHolder>() {
    var chatUser: User? = null

    override fun bind(viewHolder: ViewHolder, position: Int) {
        val ref = FirebaseDatabase.getInstance().getReference("/users")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (message.senderId == FirebaseAuth.getInstance().currentUser?.uid) {
                    chatUser =
                        snapshot.child(message.receiverId).getValue(User::class.java)
                    viewHolder.itemView.name_row_in.text = chatUser?.name.toString()

                    val storageRef = FirebaseStorage.getInstance()
                        .getReference("profile_pics/${message.receiverId}")
                    storageRef.getBytes(1920 * 1920)
                        .addOnSuccessListener {
                            val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                            viewHolder.itemView.profilePic_chat_row_in.setImageBitmap(bitmap)
                        }
                } else {
                    chatUser =
                        snapshot.child(message.senderId).getValue(User::class.java)
                    viewHolder.itemView.name_row_in.text = chatUser?.name.toString()

                    val storageRef = FirebaseStorage.getInstance()
                        .getReference("profile_pics/${message.senderId}")
                    storageRef.getBytes(1920 * 1920)
                        .addOnSuccessListener {
                            val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                            viewHolder.itemView.profilePic_chat_row_in.setImageBitmap(bitmap)
                        }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })

        val pattern = "HH:mm"
        val dateTime = SimpleDateFormat(pattern, Locale.US)
        viewHolder.itemView.time_row_in.text = dateTime.format(message.time).toString()
        viewHolder.itemView.latest_message_row_in.text = message.text
    }

    override fun getLayout(): Int {
        return R.layout.item_chat_in_row
    }
}

//class ChatOutItem(private val message: Message) : Item<ViewHolder>() {
//    var chatUser: User? = null
//
//    override fun bind(viewHolder: ViewHolder, position: Int) {
//        val ref = FirebaseDatabase.getInstance().getReference("/users")
//        ref.addListenerForSingleValueEvent(object : ValueEventListener {
//            override fun onDataChange(snapshot: DataSnapshot) {
//                if (message.senderId == FirebaseAuth.getInstance().currentUser?.uid) {
//                    chatUser =
//                        snapshot.child(message.receiverId).getValue(User::class.java)
//                    viewHolder.itemView.name_row_out.text = chatUser?.name.toString()
//                } else {
//                    chatUser =
//                        snapshot.child(message.senderId).getValue(User::class.java)
//                    viewHolder.itemView.name_row_out.text = chatUser?.name.toString()
//                }
//            }
//
//            override fun onCancelled(error: DatabaseError) {
//                TODO("Not yet implemented")
//            }
//        })
//
//        val pattern = "HH:mm"
//        val dateTime = SimpleDateFormat(pattern, Locale.US)
//        viewHolder.itemView.time_row_out.text = dateTime.format(message.time).toString()
//        viewHolder.itemView.latest_message_row_out.text = message.text
//    }
//
//    override fun getLayout(): Int {
//        return R.layout.item_chat_out_row
//    }
//}