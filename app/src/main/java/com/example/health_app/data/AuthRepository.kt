package com.example.health_app.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

class AuthRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {
    val currentUser: FirebaseUser?
        get() = auth.currentUser

    suspend fun login(email: String, password: String): FirebaseUser? {
        auth.signInWithEmailAndPassword(email, password).await()
        return auth.currentUser
    }

    suspend fun register(email: String, password: String): FirebaseUser? {
        auth.createUserWithEmailAndPassword(email, password).await()
        return auth.currentUser
    }

    suspend fun signInWithGoogle(idToken: String): FirebaseUser? {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).await()
        return auth.currentUser
    }

    fun logout() {
        auth.signOut()
    }
}

