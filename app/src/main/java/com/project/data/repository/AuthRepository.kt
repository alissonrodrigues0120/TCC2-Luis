package com.project.data.repository

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.SetOptions
import com.project.data.model.User
import com.project.data.remote.FirestoreService
import kotlinx.coroutines.tasks.await



class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val users = FirestoreService.db.collection("users")

    suspend fun register(
        email: String,
        password: String,
        name: String
    ) {
            val result = auth
            .createUserWithEmailAndPassword(email, password)
            .await()

        val uid = result.user!!.uid

       val user = User(
            name = name,
            email = email,
            senha = password 
        )

        users.document(uid)
            .set(user, SetOptions.merge())
            .await()
    }

    suspend fun resetPassword(email: String) {
        auth
            .sendPasswordResetEmail(email)
            .await()
    }
}
