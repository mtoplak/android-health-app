package com.example.health_app.data

import kotlinx.coroutines.flow.Flow

class MeritevRepository(private val meritevDao: MeritevDao) {

    val vseMeritve: Flow<List<Meritev>> = meritevDao.getAll()

    fun getMeritevById(id: Int): Flow<Meritev?> = meritevDao.getById(id)

    fun getMeritveByUser(userId: String): Flow<List<Meritev>> = meritevDao.getAllByUser(userId)

    suspend fun vstavi(meritev: Meritev): Long {
        return meritevDao.insert(meritev)
    }

    suspend fun posodobi(meritev: Meritev) {
        meritevDao.update(meritev)
    }

    suspend fun izbrisi(meritev: Meritev) {
        meritevDao.delete(meritev)
    }

    suspend fun deleteAllByUser(userId: String) {
        meritevDao.deleteAllByUser(userId)
    }
}

