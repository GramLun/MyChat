package com.moskofidi.mychat.chatActivity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.moskofidi.mychat.R
import com.moskofidi.mychat.dataClass.Message
import com.moskofidi.mychat.dataClass.User
import com.moskofidi.mychat.receiver.ConnectionListener
import com.r0adkll.slidr.Slidr
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.item_incoming.view.*
import kotlinx.android.synthetic.main.item_incoming.view.text_message_in
import kotlinx.android.synthetic.main.item_outcoming.view.*
import kotlinx.coroutines.InternalCoroutinesApi
import java.text.SimpleDateFormat
import java.util.*

@InternalCoroutinesApi
class ChatActivity : AppCompatActivity() {
    private val adapter = GroupAdapter<ViewHolder>()
    private val connectionListener = ConnectionListener(this)
    private var user: User? = null

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        user = intent.getParcelableExtra(NewMessageActivity.USER_KEY)

        connectionListener.result = { isAvailable ->
            runOnUiThread {
                when (isAvailable) {
                    true -> {
                        supportActionBar?.title = user?.name
                    }
                    false -> {
                        supportActionBar?.title = "Ожидание сети..."
                    }
                }
            }
        }

        Slidr.attach(this)
        list_of_messages.adapter = adapter

        if (user != null) {
            fetchMessages(user!!)
            btnSend.setOnClickListener {
                sendMessage(user!!)
            }
        }
//        message_input.setOnFocusChangeListener
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
        menuInflater.inflate(R.menu.nav_chat, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.btnDeleteChat -> {
                val me = FirebaseAuth.getInstance().currentUser
                val ref = FirebaseDatabase.getInstance()
                if (user == null) {
                    ref.getReference("/messages/${me!!.uid}/${me.uid}").removeValue()
                    ref.getReference("latest_messages/${me.uid}/${me.uid}").removeValue()
                } else {
                    ref.getReference("/messages/${me!!.uid}/${user!!.id}").removeValue()
                    ref.getReference("/messages/${user!!.id}/${me.uid}").removeValue()

                    ref.getReference("latest_messages/${me.uid}/${user!!.id}").removeValue()
                    ref.getReference("latest_messages/${user!!.id}/${me.uid}").removeValue()
                }

                startActivity(Intent(this, LatestMessagesActivity::class.java))
                this.finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun sendMessage(user: User) {
        val me = FirebaseAuth.getInstance().currentUser

        if (message_input.text.toString() != "") {
            if (user.id != FirebaseAuth.getInstance().currentUser?.uid) {
                val ref = FirebaseDatabase.getInstance()
                    .getReference("/messages/${me!!.uid}/${user.id}")
                    .push()
                val toRef = FirebaseDatabase.getInstance()
                    .getReference("/messages/${user.id}/${me.uid}")
                    .push()

                val message = Message(
                    ref.key!!,
                    me.uid,
                    message_input.text.toString().trim(),
                    user.id,
                    false,
                    System.currentTimeMillis()
                )
                val toMessage = Message(
                    toRef.key!!,
                    me.uid,
                    message_input.text.toString().trim(),
                    user.id,
                    false,
                    System.currentTimeMillis()
                )

                ref.setValue(message)
                toRef.setValue(toMessage)

                val latestRef = FirebaseDatabase.getInstance()
                    .getReference("/latest_messages/${me.uid}/${user.id}")
                latestRef.setValue(message)

                val latestToRef = FirebaseDatabase.getInstance()
                    .getReference("/latest_messages/${user.id}/${me.uid}")
                latestToRef.setValue(toMessage)
            } else {
                val ref = FirebaseDatabase.getInstance()
                    .getReference("/messages/${me!!.uid}/${user.id}")
                    .push()
                val message = Message(
                    ref.key!!,
                    me.uid,
                    message_input.text.toString().trim(),
                    user.id,
                    false,
                    System.currentTimeMillis()
                )
                ref.setValue(message)

                val latestRef = FirebaseDatabase.getInstance()
                    .getReference("/latest_messages/${me.uid}/${user.id}")
                latestRef.setValue(message)
            }
            message_input.setText("")
        }
    }

    private fun fetchMessages(user: User) {
        val me = FirebaseAuth.getInstance().currentUser

        val ref = FirebaseDatabase.getInstance().getReference("/messages/${me!!.uid}/${user.id}")
        ref.addChildEventListener(object : ChildEventListener {

            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                chat_empty.visibility = View.GONE
                val message = snapshot.getValue(Message::class.java)
                if (message != null) {
                    if (message.senderId == FirebaseAuth.getInstance().currentUser?.uid)
                        adapter.add(MessageOutItem(message))
                    else {
                        adapter.add(MessageInItem(message))
                    }
                }
                list_of_messages.scrollToPosition(adapter.itemCount - 1)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
                TODO("Not yet implemented")
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
                TODO("Not yet implemented")
            }

            override fun onCancelled(databaseError: DatabaseError) {
                println("The read failed: " + databaseError.code)
            }
        })
    }
}

class MessageInItem(private val message: Message) : Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.text_message_in.text = message.text

        val pattern = "HH:mm"
        val dateTime = SimpleDateFormat(pattern, Locale.US)
        val time = dateTime.format(message.time)

        viewHolder.itemView.time_message_in.text = time.toString()
    }

    override fun getLayout(): Int {
        return R.layout.item_incoming
    }
}

class MessageOutItem(private val message: Message) : Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.text_message_out.text = message.text

        val pattern = "HH:mm"
        val dateTime = SimpleDateFormat(pattern, Locale.getDefault())
        val time = dateTime.format(message.time)

        viewHolder.itemView.time_message_out.text = time.toString()
    }

    override fun getLayout(): Int {
        return R.layout.item_outcoming
    }
}