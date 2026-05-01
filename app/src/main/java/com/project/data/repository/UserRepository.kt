package com.project.data.repository

import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.project.data.model.User
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.tasks.await

class UserRepository {

    private val db = FirebaseFirestore.getInstance(FirebaseApp.getInstance())

    private val usersCollection = db.collection("users")

    suspend fun saveUser(user: User) {
        val docRef = if (user.id.isNotBlank()) {
            usersCollection.document(user.id)
        } else {
            usersCollection.document()
        }

        docRef
            .set(user, SetOptions.merge())
            .await()
    }

    suspend fun getUser(id: String): User? {
        if (id.isBlank()) return null

        val doc = usersCollection
            .document(id)
            .get()
            .await()

        return doc.toObject(User::class.java)
    }

    fun getUserName(id: String): Flow<String> = if (id.isBlank()) {
        flowOf("")
    } else {
        callbackFlow {
            val listener = usersCollection.document(id).addSnapshotListener { snapshot, error ->
                if (error != null) {
                    trySend(FirebaseAuth.getInstance().currentUser?.displayName.orEmpty())
                    return@addSnapshotListener
                }

                val userName = snapshot?.getString("name")
                    ?: snapshot?.getString("nome")
                    ?: snapshot?.getString("displayName")
                    ?: snapshot?.getString("fullName")
                    ?: FirebaseAuth.getInstance().currentUser?.displayName
                    ?: ""

                trySend(userName)
            }

            awaitClose { listener.remove() }
        }
    }
}
