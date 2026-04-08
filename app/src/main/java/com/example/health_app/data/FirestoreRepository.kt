package com.example.health_app.data

import com.google.firebase.firestore.FirebaseFirestore
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
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // Keep the app alive even if Firestore listener fails;
                    // downstream UI can continue showing local data or an empty state.
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val items = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    Meritev.fromFirestore(doc.id, doc.data ?: emptyMap())
                }.sortedByDescending { it.datum }
                trySend(items)
            }

        awaitClose { registration.remove() }
    }

    suspend fun fetchAllByUser(userId: String): List<Meritev> {
        val snapshot = meritveCollection
            .whereEqualTo("userId", userId)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            Meritev.fromFirestore(doc.id, doc.data ?: emptyMap())
        }.sortedByDescending { it.datum }
    }
}


