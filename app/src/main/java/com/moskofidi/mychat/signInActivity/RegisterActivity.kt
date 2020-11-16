package com.moskofidi.mychat.signInActivity

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.firebase.ui.auth.ui.AcquireEmailHelper.RC_SIGN_IN
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.moskofidi.mychat.R
import com.moskofidi.mychat.chatActivity.LatestMessagesActivity
import com.moskofidi.mychat.dataClass.User
import com.moskofidi.mychat.room.UserDatabase
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.coroutines.InternalCoroutinesApi
import java.io.File
import java.io.FileOutputStream

@InternalCoroutinesApi
@RequiresApi(Build.VERSION_CODES.M)
class RegisterActivity : AppCompatActivity() {

    private val auth: FirebaseAuth = Firebase.auth
    private val SELECT_IMAGE = 1
    private val REQUEST_PERMISSIONS_CODE = 2
    private lateinit var uri: Uri
    private var nameList: MutableList<String> = mutableListOf()

    override fun onStart() {
        super.onStart()

        if (FirebaseAuth.getInstance().currentUser != null) {
            startActivity(Intent(this, LatestMessagesActivity::class.java))
            this.finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        setContentView(R.layout.activity_register)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        btnGoogleSignIn.setOnClickListener {
            signIn(mGoogleSignInClient)
        }

        btnRegister.setOnClickListener {
            btnRegister.isClickable = false
            fetchNames()

            val email = email_input_reg.text.toString()
            val password = password_input_reg.text.toString()

            if (nameList.contains(name_input_reg.text.toString())) {
                btnRegister.isClickable = true
                Toast.makeText(this, "Имя пользователя занято", Toast.LENGTH_SHORT).show()
            } else {
                if (email.isEmpty() || password.isEmpty()) {
                    btnRegister.isClickable = true
                    Toast.makeText(this, "Введите email/пароль", Toast.LENGTH_SHORT).show()
                } else if (profilePic_reg.drawable == null) {
                    btnRegister.isClickable = true
                    Toast.makeText(this, "Выберите фото профиля", Toast.LENGTH_SHORT).show()
                } else {
                    emailRegister(email, password)
                }
            }
        }

        btnHaveAccount.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }

        profilePic_reg.setOnClickListener {
            checkPermissions()
        }
    }

    private fun signIn(mGoogleSignInClient: GoogleSignInClient) {
        val signInIntent: Intent = mGoogleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                Toast.makeText(this, "Success google sign in:" + account.id, Toast.LENGTH_SHORT)
                    .show()
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(
                    this,
                    "Google sign in failed: code " + e.statusCode,
                    Toast.LENGTH_SHORT
                ).show()
            }
        } else if (requestCode == SELECT_IMAGE) {
            if (resultCode == Activity.RESULT_OK && data != null) {
                uri = data.data!!
                profilePic_reg.setImageURI(uri)
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Create user(google): success", Toast.LENGTH_SHORT).show()

                    addUser(auth.currentUser!!)

                    startActivity(Intent(this, LatestMessagesActivity::class.java))
                    this.finish()
                } else {
                    Toast.makeText(this, "Create user(google): failure", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun emailRegister(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Create user(email): success", Toast.LENGTH_SHORT)
                        .show()

                    val user = auth.currentUser
                    val update = UserProfileChangeRequest.Builder()
                        .setDisplayName(name_input_reg.text.toString()).build()
                    user?.updateProfile(update)?.addOnCompleteListener {
                        addUser(auth.currentUser!!)
                        addUserToCache(auth.currentUser!!)
                    }
                    uploadImg(user!!)

                    startActivity(Intent(this, LatestMessagesActivity::class.java))
                    this.finish()
                } else {
                    Toast.makeText(this, "Create user(email): failure", Toast.LENGTH_SHORT)
                        .show()
                }
            }
    }

    private fun addUser(user: FirebaseUser) {
        val uid = user.uid
        val name = user.displayName ?: ""
        val email = user.email ?: ""
        val userRef = FirebaseDatabase.getInstance().getReference("/users/$uid")
        val nameRef = FirebaseDatabase.getInstance().getReference("/names/$uid")

        val mUser = User(uid, name, email)
        userRef.setValue(mUser)
        nameRef.setValue(name)
    }

    private fun pickImg() {
        val intentPickFromGallery = Intent(
            Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        )
        startActivityForResult(intentPickFromGallery, SELECT_IMAGE)
    }

    private fun uploadImg(user: FirebaseUser) {
        val storageRef = FirebaseStorage.getInstance().getReference("profile_pics/${user.uid}")
        storageRef.putFile(uri)
            .addOnSuccessListener {
                Toast.makeText(this, "Profile pic has been loaded", Toast.LENGTH_SHORT)
                    .show()
            }.addOnFailureListener {
                Toast.makeText(this, "FAILURE", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun addUserToCache(user: FirebaseUser) {
        // Without profile pic
        val userPath = File(externalCacheDir, "/me")
        userPath.mkdirs()
        val userFile = File(userPath.toString(), user.displayName.toString() + ".txt")

        try {
            userFile.writeText(user.displayName.toString())
        } catch (e: Exception) {
            Toast.makeText(this, "e: Exception", Toast.LENGTH_LONG).show()
        }
    }

    private fun fetchNames() {
        val ref = FirebaseDatabase.getInstance().getReference("/names")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    val name = it.getValue(String::class.java)
                    Toast.makeText(this@RegisterActivity, name, Toast.LENGTH_SHORT).show()
                    nameList.plusAssign(listOf(name!!.toString()))
                }
            }

            override fun onCancelled(error: DatabaseError) {
                TODO("Not yet implemented")
            }
        })
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            + ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            pickImg()
        } else {
            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                AlertDialog.Builder(this)
                    .setTitle("Нужно разрешение")
                    .setCancelable(true)
                    .setMessage("Это разрешение нужно для загрузки изображений")
                    .setPositiveButton("Да") { _, _ ->
                        ActivityCompat.requestPermissions(
                            this@RegisterActivity,
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                            REQUEST_PERMISSIONS_CODE
                        )
                    }
                    .setNegativeButton("Нет") { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create().show()
            } else {
                ActivityCompat.requestPermissions(
                    this@RegisterActivity,
                    arrayOf(
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    REQUEST_PERMISSIONS_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_PERMISSIONS_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                pickImg()
            } else {
                Toast.makeText(this, "Permission DENIED", Toast.LENGTH_LONG).show()
            }
            return
        }
    }
}
