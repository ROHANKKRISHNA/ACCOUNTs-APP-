package com.example.rohan_project

import android.content.Intent
import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.transition.TransitionManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.UserProfileChangeRequest

class MainActivity : AppCompatActivity() {

    private var isSignup = false

    private val auth by lazy { FirebaseAuth.getInstance() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val cardContent = findViewById<ViewGroup>(R.id.cardContent)
        val tabLogin = findViewById<TextView>(R.id.tabLogin)
        val tabSignup = findViewById<TextView>(R.id.tabSignup)
        val nameField = findViewById<EditText>(R.id.nameField)
        val emailField = findViewById<EditText>(R.id.emailField)
        val passwordField = findViewById<EditText>(R.id.passwordField)
        val confirmField = findViewById<EditText>(R.id.confirmField)
        val actionButton = findViewById<Button>(R.id.actionButton)

        fun clearAllFields() {
            listOf(nameField, emailField, passwordField, confirmField).forEach {
                it.text.clear()
                it.error = null
            }
            emailField.clearFocus()
            passwordField.clearFocus()
        }

        fun showMode(signup: Boolean, animate: Boolean = true) {
            isSignup = signup
            if (animate) TransitionManager.beginDelayedTransition(cardContent)

            clearAllFields()

            nameField.visibility = if (signup) View.VISIBLE else View.GONE
            confirmField.visibility = if (signup) View.VISIBLE else View.GONE
            actionButton.text = if (signup) "Signup" else "Login"

            tabSignup.setBackgroundResource(if (signup) R.drawable.bg_gradient_pill else 0)
            tabLogin.setBackgroundResource(if (signup) 0 else R.drawable.bg_gradient_pill)
            tabSignup.setTextColor(if (signup) 0xFFFFFFFF.toInt() else 0xFF000000.toInt())
            tabLogin.setTextColor(if (signup) 0xFF000000.toInt() else 0xFFFFFFFF.toInt())
        }

        fun handleSignup() {
            val name = nameField.text.toString().trim()
            val email = emailField.text.toString().trim().lowercase()
            val password = passwordField.text.toString()
            val confirm = confirmField.text.toString()

            when {
                name.isEmpty() -> nameField.error = "Enter your name"
                !Patterns.EMAIL_ADDRESS.matcher(email).matches() -> emailField.error = "Enter a valid email"
                password.length < 6 -> passwordField.error = "At least 6 characters"
                password != confirm -> confirmField.error = "Passwords don't match"
                else -> {
                    actionButton.isEnabled = false
                    auth.createUserWithEmailAndPassword(email, password)
                        .addOnSuccessListener { result ->
                            val user = result.user
                            val profileUpdates = UserProfileChangeRequest.Builder()
                                .setDisplayName(name)
                                .build()

                            user?.updateProfile(profileUpdates)?.addOnCompleteListener {
                                actionButton.isEnabled = true
                                // Immediately take user to ProfileActivity with their name
                                val intent = Intent(this, ProfileActivity::class.java)
                                intent.putExtra("name", name)
                                intent.putExtra("email", user.email ?: email)
                                startActivity(intent)
                                finish()
                            }
                        }
                        .addOnFailureListener { e ->
                            actionButton.isEnabled = true
                            if (e is FirebaseAuthUserCollisionException) {
                                Toast.makeText(this, "Account already exists. Please log in.", Toast.LENGTH_SHORT).show()
                                showMode(false)
                            } else {
                                Toast.makeText(this, e.message ?: "Signup failed", Toast.LENGTH_LONG).show()
                            }
                        }
                }
            }
        }

        fun handleLogin() {
            val email = emailField.text.toString().trim().lowercase()
            val password = passwordField.text.toString()

            when {
                email.isEmpty() -> emailField.error = "Enter your email"
                password.isEmpty() -> passwordField.error = "Enter your password"
                else -> {
                    actionButton.isEnabled = false
                    auth.signInWithEmailAndPassword(email, password)
                        .addOnSuccessListener { result ->
                            val user = result.user
                            val intent = Intent(this, ProfileActivity::class.java)
                            intent.putExtra("name", user?.displayName ?: "")
                            intent.putExtra("email", user?.email ?: email)
                            startActivity(intent)
                            finish()
                        }
                        .addOnFailureListener { e ->
                            actionButton.isEnabled = true
                            // If the account does not exist, automatically switch to Signup mode
                            if (e is FirebaseAuthInvalidUserException) {
                                Toast.makeText(this, "Account not found. Please sign up!", Toast.LENGTH_SHORT).show()
                                showMode(true)
                                emailField.requestFocus()
                            } else {
                                Toast.makeText(
                                    this,
                                    "Incorrect password. Please try again.",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                }
            }
        }

        // Always start on Login
        showMode(signup = false, animate = false)

        tabLogin.setOnClickListener { if (isSignup) showMode(false) }
        tabSignup.setOnClickListener { if (!isSignup) showMode(true) }
        actionButton.setOnClickListener { if (isSignup) handleSignup() else handleLogin() }
    }
}