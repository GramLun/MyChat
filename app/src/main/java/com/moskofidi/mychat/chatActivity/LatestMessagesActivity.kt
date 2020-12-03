package com.moskofidi.mychat.chatActivity

import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import com.moskofidi.mychat.R
import com.moskofidi.mychat.dataClass.Message
import com.moskofidi.mychat.dataClass.User
import com.moskofidi.mychat.receiver.ConnectionListener
import com.moskofidi.mychat.signInActivity.RegisterActivity
import com.xwray.groupie.GroupAdapter
import com.xwray.groupie.Item
import com.xwray.groupie.ViewHolder
import kotlinx.android.synthetic.main.activity_latest_messages.*
import kotlinx.android.synthetic.main.item_chat_in_row.view.*
import kotlinx.coroutines.InternalCoroutinesApi
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap

@InternalCoroutinesApi
@Suppress("NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
class LatestMessagesActivity : AppCompatActivity() {

    private val adapter = GroupAdapter<ViewHolder>()
    private val connectionListener = ConnectionListener(this)
    private val latestMessagesMap = HashMap<String, Message>()

//    private var images = arrayOf<File>()
//    private var names = arrayOf<File>()
//    private var messages = arrayOf<File>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_latest_messages)
        actionBar?.setDisplayShowCustomEnabled(true)
//        supportActionBar?.title = FirebaseAuth.getInstance().currentUser!!.displayName
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

//        loadChatsFromCache()
        adapter.setOnItemClickListener { item, _ ->
            val row = item as ChatInItem
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
//                deleteCache()
                FirebaseAuth.getInstance().signOut()
                startActivity(Intent(this, RegisterActivity::class.java))
                this.finish()
            }
            R.id.btnDeleteAccount -> {
                val dialogClickListener =
                    DialogInterface.OnClickListener { dialog, btn ->
                        when (btn) {
                            DialogInterface.BUTTON_POSITIVE -> {
                                val user = FirebaseAuth.getInstance().currentUser
                                user?.delete()
                                if (FirebaseAuth.getInstance().currentUser == null) {
                                    FirebaseDatabase.getInstance()
                                        .getReference("/users/${user!!.uid}")
                                        .removeValue()
                                    FirebaseDatabase.getInstance()
                                        .getReference("/names/${user.uid}")
                                        .removeValue()
                                    FirebaseStorage.getInstance()
                                        .getReference("profile_pics/${user.uid}")
                                        .delete()
                                }
                                FirebaseAuth.getInstance().signOut()

                                dialog.dismiss()

                                startActivity(Intent(this, RegisterActivity::class.java))
                                this.finish()
                            }
                            DialogInterface.BUTTON_NEGATIVE -> {
                                dialog.dismiss()
                            }
                        }
                    }

                val builder: AlertDialog.Builder = AlertDialog.Builder(applicationContext)
                builder.setTitle("Удаление пользователя").setMessage("Вы уверены?").setPositiveButton("Да", dialogClickListener)
                    .setNegativeButton("Нет", dialogClickListener).show()

//                deleteCache()
//                val user = FirebaseAuth.getInstance().currentUser
//                user?.delete()
//                FirebaseDatabase.getInstance()
//                    .getReference("/users/${user!!.uid}")
//                    .removeValue()
//                FirebaseDatabase.getInstance()
//                    .getReference("/names/${user.uid}")
//                    .removeValue()
//                FirebaseStorage.getInstance()
//                    .getReference("profile_pics/${user.uid}")
//                    .delete()
//                FirebaseAuth.getInstance().signOut()
//                startActivity(Intent(this, RegisterActivity::class.java))
//                this.finish()
            }
        }
        return super.onOptionsItemSelected(item)
    }

//    private fun deleteCache() {
//        val path = externalCacheDir
//
//        val imgPath = File(path, "/profile_pics")
//        if (imgPath.isDirectory) {
//            val children: Array<String> = imgPath.list()
//            for (i in children.indices) {
//                val file = File(imgPath, children[i])
//                file.deleteRecursively()
//            }
//        }
//
//        val userPath = File(path, "/users")
//        if (userPath.isDirectory) {
//            val children: Array<String> = userPath.list()
//            for (i in children.indices) {
//                val file = File(userPath, children[i])
//                file.deleteRecursively()
//            }
//        }
//
//        val latestMsgPath = File(path, "/latest_messages")
//        if (latestMsgPath.isDirectory) {
//            val children: Array<String> = latestMsgPath.list()
//            for (i in children.indices) {
//                val file = File(latestMsgPath, children[i])
//                file.deleteRecursively()
//            }
//        }
//    }

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

    private fun refreshChatView() {
        latest_empty.visibility = View.GONE
        adapter.clear()
        latestMessagesMap.values.forEach {
            adapter.add(ChatInItem(it, externalCacheDir!!))
//               if (it.senderId != user?.uid)
//                    adapter.add(ChatInItem(it))
//              else
//                    adapter.add(ChatOutItem(it))
        }
    }
}

class ChatInItem(
    private val message: Message? = null, private val path: File
) : Item<ViewHolder>() {
    var chatUser: User? = null

    override fun bind(viewHolder: ViewHolder, position: Int) {
        // load from cache
//        viewHolder.itemView.profilePic_chat_row_in.setImageBitmap(null)
//
//        if (getCacheRow()) {
//            val option = BitmapFactory.Options()
//            option.inPreferredConfig = Bitmap.Config.ARGB_8888
//            val bitmapCache = BitmapFactory.decodeFile(images[position].toString(), option)
//            viewHolder.itemView.profilePic_chat_row_in.setImageBitmap(bitmapCache)
//
//            viewHolder.itemView.name_row_in.text =
//                File(names[position].toString()).readText().substringBefore('\n')
//            viewHolder.itemView.latest_message_row_in.text =
//                File(messages[position].toString()).readText().substringBefore('\n')
//
//            val string = File(messages[position].toString()).readText()
//            val time: String = string.substringAfter('\n')
//
//            val patternCache = "HH:mm"
//            val dateTimeCache = SimpleDateFormat(patternCache, Locale.US)
//            viewHolder.itemView.time_row_in.text = dateTimeCache.format(time.toLong()).toString()
//
//            val user = User(
//                File(names[position].toString()).readText().substringAfter('\n'),
//                File(names[position].toString()).readText().substringBefore('\n')
//            )
//            chatUser = user
//        }

        // load from database
        val ref = FirebaseDatabase.getInstance().getReference("/users")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (message != null) {
                    if (message.senderId == FirebaseAuth.getInstance().currentUser?.uid) {
                        chatUser =
                            snapshot.child(message.receiverId).getValue(User::class.java)
                        viewHolder.itemView.name_row_in.text = chatUser?.name.toString()

                        val storageRef = FirebaseStorage.getInstance()
                            .getReference("profile_pics/${message.receiverId}")
                        storageRef.getBytes(4000 * 4000)
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
                        storageRef.getBytes(4000 * 4000)
                            .addOnSuccessListener {
                                val bitmap = BitmapFactory.decodeByteArray(it, 0, it.size)
                                viewHolder.itemView.profilePic_chat_row_in.setImageBitmap(bitmap)
                            }
                    }

                    if (viewHolder.itemView.latest_message_row_in.text != message.text) {
                        val pattern = "HH:mm"
                        val dateTime = SimpleDateFormat(pattern, Locale.US)
                        viewHolder.itemView.time_row_in.text =
                            dateTime.format(message.time).toString()
                        viewHolder.itemView.latest_message_row_in.text = message.text
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    override fun getLayout(): Int {
        return R.layout.item_chat_in_row
    }

//    private fun setCacheRow(chatUser: User, bitmap: Bitmap? = null) {
//        // Profile pic, name and uid
//        val imgPath = File(path, "/profile_pics/")
//        imgPath.mkdirs()
//        val imgFile = File(imgPath.toString(), chatUser.id + ".jpg")
//
//        val userPath = File(path, "/users/")
//        userPath.mkdirs()
//        val userFile = File(userPath.toString(), chatUser.name + ".txt")
//
//        val latestMsgPath = File(path, "/latest_messages/")
//        latestMsgPath.mkdirs()
//
//        val latestMsgFile = File(latestMsgPath.toString(), chatUser.id + ".txt")
//
//        try {
//            if (!imgFile.exists()) {
//                val fOut = FileOutputStream(imgFile)
//                bitmap!!.compress(Bitmap.CompressFormat.JPEG, 85, fOut)
//                fOut.flush()
//                fOut.close()
//            }
//
//            if (!userFile.exists())
//                userFile.writeText(chatUser.name + '\n' + chatUser.id)
//
//            if (message != null) {
//                if (latestMsgFile.exists()) {
//                    latestMsgFile.deleteRecursively()
//                    latestMsgFile.writeText(message.text + '\n' + message.time)
//                } else
//                    latestMsgFile.writeText(message.text + '\n' + message.time)
//            }
//        } catch (e: Exception) {
//            Log.d("cache", "e: Exception")
//        }
//    }
//
//    private fun getCacheRow(): Boolean {
//        val imgPath = File(path, "/profile_pics/")
//        if (imgPath.exists()) {
//            val directory = File(imgPath.toString())
//            images = directory.listFiles()
//            if (images.isEmpty())
//                return false
//        } else {
//            return false
//        }
//
//        val userPath = File(path, "/users/")
//        if (userPath.exists()) {
//            val directory = File(userPath.toString())
//            names = directory.listFiles()
//            if (names.isEmpty())
//                return false
//        } else {
//            return false
//        }
//
//        val latestMsgPath = File(path, "/latest_messages/")
//        if (latestMsgPath.exists()) {
//            val directory = File(latestMsgPath.toString())
//            messages = directory.listFiles()
//            if (messages.isEmpty())
//                return false
//        } else {
//            return false
//        }
//        return true
//    }
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