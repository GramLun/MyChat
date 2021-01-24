package com.moskofidi.mychat.chat

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
import com.moskofidi.mychat.dataClass.TypeMsg
import com.moskofidi.mychat.dataClass.User
import com.moskofidi.mychat.listener.ConnectionListener
import com.r0adkll.slidr.Slidr
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_chat.*
import kotlinx.android.synthetic.main.item_incoming.view.*
import kotlinx.android.synthetic.main.item_incoming.view.text_message_in
import kotlinx.android.synthetic.main.item_outcoming_read.view.*
import kotlinx.android.synthetic.main.item_outcoming_unread.view.*
import kotlinx.coroutines.InternalCoroutinesApi
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

@InternalCoroutinesApi
class ChatActivity : AppCompatActivity() {
    private val adapter = GroupAdapter<ViewHolder>()
    private val connectionListener = ConnectionListener(this)
    private var user: User? = null

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
                        supportActionBar?.title = getString(R.string.waiting_for_network)
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
                // send from me
                val ref = FirebaseDatabase.getInstance()
                    .getReference("/messages/${me!!.uid}/${user.id}")
                    .push()
                val toRef = FirebaseDatabase.getInstance()
                    .getReference("/messages/${user.id}/${me.uid}")
                    .push()

                val message = Message(
                    ref.key!!,
                    me.uid,
                    user.id,
                    message_input.text.toString().trim(),
                    false,
                    System.currentTimeMillis()
                )
                val toMessage = Message(
                    toRef.key!!,
                    me.uid,
                    user.id,
                    message_input.text.toString().trim(),
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
                // send to me (dialog with myself)
                val ref = FirebaseDatabase.getInstance()
                    .getReference("/messages/${me!!.uid}/${user.id}")
                    .push()
                val message = Message(
                    ref.key!!,
                    me.uid,
                    user.id,
                    message_input.text.toString().trim(),
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
        val uRef = FirebaseDatabase.getInstance().getReference("/messages/${user.id}/${me.uid}")
        val latRef = FirebaseDatabase.getInstance().getReference("/latest_messages/${me.uid}")
        val uLatRef = FirebaseDatabase.getInstance().getReference("/latest_messages/${user.id}")
        ref.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                chat_empty.visibility = View.GONE
                val message = snapshot.getValue(Message::class.java)
                if (message != null) {
                    setRead(message, snapshot)

                    if (message.senderId == me.uid)
                        if (!message.read) {
                            adapter.add(MessageItem(message, TypeMsg.MSG_OUT_UNREAD))
                        }
                        else {
                            adapter.add(MessageItem(message, TypeMsg.MSG_OUT_READ))
                        }
                    else {
                        adapter.add(MessageItem(message, TypeMsg.MSG_IN_READ))
                    }
                }
                list_of_messages.scrollToPosition(adapter.itemCount - 1)
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                chat_empty.visibility = View.GONE
                val message = snapshot.getValue(Message::class.java)
                if (message != null) {
                    setRead(message, snapshot)
//                    if (message.senderId == me.uid)
//                        if (!message.read)
//                            adapter.add(MessageItem(message, TypeMsg.MSG_OUT_UNREAD))
//                        else
//                            adapter.add(MessageItem(message, TypeMsg.MSG_OUT_READ))
//                    else {
//                        adapter.add(MessageItem(message, TypeMsg.MSG_IN_READ))
//                    }
                }
                list_of_messages.scrollToPosition(adapter.itemCount - 1)
            }

            override fun onChildRemoved(snapshot: DataSnapshot) { }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) { }

            override fun onCancelled(databaseError: DatabaseError) {
                println("The read failed: " + databaseError.code)
            }
        })
        uRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java)
                if (message != null) {
                    setRead(message, snapshot)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java)
                if (message != null) {
                    setRead(message, snapshot)
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) { }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) { }

            override fun onCancelled(error: DatabaseError) { }
        })

        latRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java)
                if (message != null) {
                    setRead(message, snapshot)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java)
                if (message != null) {
                    setRead(message, snapshot)
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
        uLatRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java)
                if (message != null) {
                    setRead(message, snapshot)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {
                val message = snapshot.getValue(Message::class.java)
                if (message != null) {
                    setRead(message, snapshot)
                }
            }

            override fun onChildRemoved(snapshot: DataSnapshot) {
            }

            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {
            }

            override fun onCancelled(error: DatabaseError) {
            }

        })
    }

    private fun setRead(message: Message, snapshot: DataSnapshot) {
        if (message.senderId == user!!.id) {
            message.read = true
            val hashMap = HashMap<String, Any>()
            hashMap["read"] = true
            snapshot.ref.updateChildren(hashMap)
        }
    }
}

class MessageItem(private val message: Message, private val type: TypeMsg) : Item<ViewHolder>() {
    override fun bind(viewHolder: ViewHolder, position: Int) {

        val pattern = "HH:mm"
        val dateTime = SimpleDateFormat(pattern, Locale.US)
        val time = dateTime.format(message.time)

        when (type) {
            TypeMsg.MSG_IN_READ -> {
                viewHolder.itemView.text_message_in.text = message.text
                viewHolder.itemView.time_message_in.text = time.toString()
            }
            TypeMsg.MSG_OUT_READ -> {
                viewHolder.itemView.text_message_out_read.text = message.text
                viewHolder.itemView.time_message_out_read.text = time.toString()
            }
            TypeMsg.MSG_OUT_UNREAD -> {
                viewHolder.itemView.text_message_out_unread.text = message.text
                viewHolder.itemView.time_message_out_unread.text = time.toString()
            }
        }
    }

    override fun getLayout(): Int {
        return when (type) {
            TypeMsg.MSG_IN_READ -> {
                R.layout.item_incoming
            }
            TypeMsg.MSG_OUT_READ -> {
                R.layout.item_outcoming_read
            }
            TypeMsg.MSG_OUT_UNREAD -> {
                R.layout.item_outcoming_unread
            }
        }
    }
}