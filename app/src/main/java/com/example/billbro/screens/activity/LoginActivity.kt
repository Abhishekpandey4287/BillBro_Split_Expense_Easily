package com.example.billbro.screens.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.billbro.databinding.ActivityLoginBinding
import com.example.billbro.screens.dialog.LoadingDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var loading: LoadingDialog
    private lateinit var auth: FirebaseAuth

    override fun onStart() {
        super.onStart()
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            navigateToGroup()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        loading = LoadingDialog(this)
        auth = FirebaseAuth.getInstance()
        binding.btnLogin.setOnClickListener {
            loading.show()
            login()
        }

//        binding.btnRegister.setOnClickListener {
//            register()
//        }
    }
    private fun login() {
        val email = binding.etEmail.text.toString()
        val pass = binding.etPassword.text.toString()
        Firebase.auth.signInAnonymously().addOnCompleteListener { task ->
            loading.dismiss()
            if (task.isSuccessful) {
                startActivity(Intent(this, GroupActivity::class.java))
                finish()
            }else {
                Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
    private fun register() {
        val email = binding.etEmail.text.toString().trim()
        val pass = binding.etPassword.text.toString().trim()

        if (email.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Email & Password required", Toast.LENGTH_SHORT).show()
            return
        }

        loading.show()

        FirebaseAuth.getInstance()
            .signInAnonymously()
            .addOnSuccessListener {
                loading.dismiss()
                navigateToGroup()
            }
            .addOnFailureListener {
                loading.dismiss()
                Toast.makeText(this, it.message, Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToGroup() {
        startActivity(Intent(this, GroupActivity::class.java))
        finish()
    }

}