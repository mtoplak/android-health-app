package com.example.health_app.data

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val meritveCollection = firestore.collection("meritve")

    suspend fun insertMeritev(meritev: Meritev): String {
        val docRef = meritveCollection.add(meritev.toFirestoreMap()).await()
        return docRef.id
    }

    suspend fun deleteMeritev(firestoreId: String) {
        if (firestoreId.isBlank()) return
        meritveCollection.document(firestoreId).delete().await()
    }

    fun getMeritveByUser(userId: String): Flow<List<Meritev>> = callbackFlow {
        val registration = meritveCollection
            .whereEqualTo("userId", userId)
            .orderBy("datum", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val items = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    Meritev.fromFirestore(doc.id, doc.data ?: emptyMap())
                }
                trySend(items)
            }

        awaitClose { registration.remove() }
    }

    suspend fun fetchAllByUser(userId: String): List<Meritev> {
        val snapshot = meritveCollection
            .whereEqualTo("userId", userId)
            .orderBy("datum", Query.Direction.DESCENDING)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            Meritev.fromFirestore(doc.id, doc.data ?: emptyMap())
        }
    }
}

