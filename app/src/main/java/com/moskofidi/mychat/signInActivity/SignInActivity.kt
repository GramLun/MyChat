package com.moskofidi.mychat.signInActivity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.moskofidi.mychat.R
import com.moskofidi.mychat.chatActivity.LatestMessagesActivity
import kotlinx.android.synthetic.main.activity_sign_in.*

class SignInActivity : AppCompatActivity() {

    val auth: FirebaseAuth = Firebase.auth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)

        btnSignIn.setOnClickListener {
            val email = email_input.text.toString()
            val password = password_input.text.toString()

            if (email.isEmpty() || password.isEmpty())
                Toast.makeText(this, "Введите email/пароль", Toast.LENGTH_SHORT).show()
            else
                emailSignIn(email, password)
        }
    }

    private fun emailSignIn(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Toast.makeText(this, "Sign in:success", Toast.LENGTH_SHORT).show()
                    val user = auth.currentUser
                    startActivity(Intent(this, LatestMessagesActivity::class.java))
                    this.finish()
                } else {
                    Toast.makeText(this, "Wrong email/password", Toast.LENGTH_SHORT).show()
                }
            }
    }
}