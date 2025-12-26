package com.project.data.repository

import com.google.firebase.firestore.SetOptions
import com.project.data.model.User
import com.project.data.remote.FirestoreService
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val usersCollection =
        FirestoreService.db.collection("users")
            .document()

    suspend fun saveUser(user: User) {
        usersCollection
            .set(user, SetOptions.merge())
            .await()
    }

    suspend fun getUser(id: String): User? {
        val doc = usersCollection
            .get()
            .await()

        return doc.toObject(User::class.java)
    }
}
