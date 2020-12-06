package com.moskofidi.mychat.signIn

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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
import com.moskofidi.mychat.chat.LatestMessagesActivity
import com.moskofidi.mychat.dataClass.User
import kotlinx.android.synthetic.main.activity_register.*
import kotlinx.android.synthetic.main.permission_dialog_custom.*
import kotlinx.coroutines.InternalCoroutinesApi

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

    @SuppressLint("ResourceAsColor")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(R.style.AppTheme)
        setContentView(R.layout.activity_register)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        fetchNames()

        btnGoogleSignIn.setOnClickListener {
            signIn(mGoogleSignInClient)
        }

        btnRegister.setOnClickListener {
//            val profilePic = findViewById<View>(R.id.profilePic_reg)
//            profilePic.setBackgroundColor(ContextCompat.getColor(this, R.color.mainBackground))
            val email = email_input_reg.text.toString()
            val password = password_input_reg.text.toString()
            val name = name_input_reg.text.toString()

            if (nameList.contains(name)) {
                Toast.makeText(this, getString(R.string.name_is_already_taken), Toast.LENGTH_SHORT).show()
            } else {
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, getString(R.string.enter_email), Toast.LENGTH_SHORT).show()
                } else if (profilePic_reg.drawable == null) {
                    Toast.makeText(this, getString(R.string.pick_profile_photo), Toast.LENGTH_SHORT).show()
                } else {
                    btnRegister.isClickable = false
                    emailRegister(email, password)
                }
            }
        }

        btnHaveAccount.setOnClickListener {
            startActivity(Intent(this, SignInActivity::class.java))
        }

        profilePic_reg.setOnClickListener {
            checkStoragePermissions()
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
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.e("Google register", e.statusCode.toString())
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
                    addUser(auth.currentUser!!)
                    Toast.makeText(this, getString(R.string.google_sign_in_success), Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, LatestMessagesActivity::class.java))
                    this.finish()
                } else {
                    Toast.makeText(this, getString(R.string.google_sign_in_error), Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun emailRegister(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, getString(R.string.register_success), Toast.LENGTH_SHORT)
                        .show()

                    val user = auth.currentUser
                    val update = UserProfileChangeRequest.Builder()
                        .setDisplayName(name_input_reg.text.toString()).build()
                    user?.updateProfile(update)?.addOnCompleteListener {
                        addUser(auth.currentUser!!)
                    }
                    uploadImg(user!!)

                    startActivity(Intent(this, LatestMessagesActivity::class.java))
                    this.finish()
                } else {
                    btnRegister.isClickable = true
                    Toast.makeText(this, getString(R.string.register_error), Toast.LENGTH_SHORT)
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
                Toast.makeText(this, getString(R.string.profile_pic_load_success), Toast.LENGTH_SHORT)
                    .show()
            }.addOnFailureListener {
                Toast.makeText(this, getString(R.string.profile_pic_load_error), Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun fetchNames() {
        val ref = FirebaseDatabase.getInstance().getReference("/names")
        ref.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                snapshot.children.forEach {
                    val name = it.getValue(String::class.java)
//                    Toast.makeText(this@RegisterActivity, name, Toast.LENGTH_SHORT).show()
//                    nameList.plusAssign(listOf(name!!.toString()))
                    if (name != null) {
                        nameList.add(name)
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) =
                Toast.makeText(this@RegisterActivity, getString(R.string.fetch_names_error), Toast.LENGTH_SHORT).show()
        })
    }

    private fun checkStoragePermissions() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            )
            + ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this@RegisterActivity,
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                REQUEST_PERMISSIONS_CODE
            )
        } else {
            val customView = layoutInflater.inflate(R.layout.permission_dialog_custom, null)

            if (shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                val dialog = AlertDialog.Builder(this)
                    .setTitle(getString(R.string.storage_permission_title))
                    .setCancelable(true)
                    .setView(customView)
                    .setPositiveButton(getString(R.string.storage_permission_on)) { _, _ ->
                        ActivityCompat.requestPermissions(
                            this@RegisterActivity,
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                            REQUEST_PERMISSIONS_CODE
                        )
                    }
                    .setNegativeButton(getString(R.string.storage_permission_off)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .create()
                dialog.show()
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
                Toast.makeText(this, getString(R.string.no_permission), Toast.LENGTH_LONG).show()
            }
            return
        }
    }
}
