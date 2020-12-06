package com.moskofidi.mychat.chat

import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.provider.Settings.Global.getString
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
import com.moskofidi.mychat.dataClass.Message
import com.moskofidi.mychat.dataClass.User
import com.moskofidi.mychat.listener.ConnectionListener
import com.moskofidi.mychat.signIn.RegisterActivity
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_latest_messages.*
import kotlinx.android.synthetic.main.item_chat_in_row.view.*
import kotlinx.android.synthetic.main.item_chat_out_read_row.view.*
import kotlinx.android.synthetic.main.item_chat_out_unread_row.view.*
import kotlinx.coroutines.InternalCoroutinesApi
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

@InternalCoroutinesApi
@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class LatestMessagesActivity : AppCompatActivity() {

    private val adapter = GroupAdapter<ViewHolder>()
    private val connectionListener = ConnectionListener(this)
    private val latestMessagesMap = HashMap<String, Message>()

    private val user = FirebaseAuth.getInstance().currentUser

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_latest_messages)
        actionBar?.setDisplayShowCustomEnabled(true)
        list_of_chats.adapter = adapter

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

        adapter.setOnItemClickListener { item, _ ->
            val row = item as ChatItem
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

                //Delete cache
            }
            R.id.btnDeleteAccount -> {
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

                //Delete Cache
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun fetchChats() {
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

            override fun onChildRemoved(snapshot: DataSnapshot) = Toast.makeText(
                this@LatestMessagesActivity,
                getString(R.string.dialog_removed),
                Toast.LENGTH_SHORT
            ).show()

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) { }

            override fun onCancelled(error: DatabaseError) = Toast.makeText(
                this@LatestMessagesActivity,
                getString(R.string.load_chats_error),
                Toast.LENGTH_SHORT
            ).show()
        })
    }

    private fun refreshChatView() {
        latest_empty.visibility = View.GONE
        adapter.clear()
        latestMessagesMap.values.forEach {
               if (it.senderId != user?.uid)
                    adapter.add(ChatItem(it, Type.MSG_IN))
              else
                    adapter.add(ChatItem(it, Type.MSG_OUT_UNREAD))
        }
    }
}

class ChatItem(
    private val message: Message? = null, private val type: Type
) : Item<ViewHolder>() {
    var chatUser: User? = null

    override fun bind(viewHolder: ViewHolder, position: Int) {
        // load from cache

        // load from database
        val ref = FirebaseDatabase.getInstance().getReference("/users")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (message != null) {
                    if (message.senderId == FirebaseAuth.getInstance().currentUser?.uid) {
                        chatUser =
                            snapshot.child(message.receiverId).getValue(User::class.java)
                        when (type) {
                            Type.MSG_IN -> {
                                viewHolder.itemView.name_row_in.text = chatUser?.name.toString()
                            }
                            Type.MSG_OUT_READ -> {
                                viewHolder.itemView.name_row_out_read.text = chatUser?.name.toString()
                            }
                            Type.MSG_OUT_UNREAD -> {
                                viewHolder.itemView.name_row_out_unread.text = chatUser?.name.toString()
                            }
                        }

                        val storageRef = FirebaseStorage.getInstance()
                            .getReference("profile_pics/${message.receiverId}")
                        storageRef.getBytes(4000 * 4000)
                            .addOnSuccessListener {
                                val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                                when (type) {
                                    Type.MSG_IN -> {
                                        viewHolder.itemView.profilePic_chat_row_in.setImageBitmap(bitmap)
                                    }
                                    Type.MSG_OUT_READ -> {
                                        viewHolder.itemView.profilePic_chat_row_out_read.setImageBitmap(bitmap)
                                    }
                                    Type.MSG_OUT_UNREAD -> {
                                        viewHolder.itemView.profilePic_chat_row_out_unread.setImageBitmap(bitmap)
                                    }
                                }
                            }
                    } else {
                        chatUser =
                            snapshot.child(message.senderId).getValue(User::class.java)
                        when (type) {
                            Type.MSG_IN -> {
                                viewHolder.itemView.name_row_in.text = chatUser?.name.toString()
                            }
                            Type.MSG_OUT_READ -> {
                                viewHolder.itemView.name_row_out_read.text = chatUser?.name.toString()
                            }
                            Type.MSG_OUT_UNREAD -> {
                                viewHolder.itemView.name_row_out_unread.text = chatUser?.name.toString()
                            }
                        }

                        val storageRef = FirebaseStorage.getInstance()
                            .getReference("profile_pics/${message.senderId}")
                        storageRef.getBytes(4000 * 4000)
                            .addOnSuccessListener {
                                val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                                when (type) {
                                    Type.MSG_IN -> {
                                        viewHolder.itemView.profilePic_chat_row_in.setImageBitmap(bitmap)
                                    }
                                    Type.MSG_OUT_READ -> {
                                        viewHolder.itemView.profilePic_chat_row_out_read.setImageBitmap(bitmap)
                                    }
                                    Type.MSG_OUT_UNREAD -> {
                                        viewHolder.itemView.profilePic_chat_row_out_unread.setImageBitmap(bitmap)
                                    }
                                }
                            }
                    }
                    val pattern = "HH:mm"
                    val dateTime = SimpleDateFormat(pattern, Locale.US)
                    when (type) {
                        Type.MSG_IN -> {
                            viewHolder.itemView.time_row_in.text = dateTime.format(message.time).toString()
                            viewHolder.itemView.latest_message_row_in.text = message.text
                        }
                        Type.MSG_OUT_READ -> {
                            viewHolder.itemView.time_row_out_read.text = dateTime.format(message.time).toString()
                            viewHolder.itemView.latest_message_row_out_read.text = message.text
                        }
                        Type.MSG_OUT_UNREAD -> {
                            viewHolder.itemView.time_row_out_unread.text = dateTime.format(message.time).toString()
                            viewHolder.itemView.latest_message_row_out_unread.text = message.text
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) { }
        })
    }

    override fun getLayout(): Int {
        return when (type) {
            Type.MSG_IN -> {
                R.layout.item_chat_in_row
            }
            Type.MSG_OUT_READ -> {
                R.layout.item_chat_out_read_row
            }
            Type.MSG_OUT_UNREAD -> {
                R.layout.item_chat_out_unread_row
            }
        }
    }
}
