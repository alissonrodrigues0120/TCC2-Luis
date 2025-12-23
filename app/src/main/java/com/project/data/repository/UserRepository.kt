package com.project.data.repository

import com.project.data.model.User
import com.project.data.remote.FirestoreService
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val usersCollection =
        FirestoreService.db.collection("users")

    suspend fun saveUser(user: User) {
        usersCollection
            .document(user.id)
            .set(user)
            .await()
    }

    suspend fun getUser(id: String): User? {
        val doc = usersCollection
            .document(id)
            .get()
            .await()

        return doc.toObject(User::class.java)
    }
}
