package dev.amenokizele.tervyn.data.auth.local

import dev.amenokizele.tervyn.data.local.dao.UserDao
import dev.amenokizele.tervyn.data.local.mapper.toDomain
import dev.amenokizele.tervyn.di.IoDispatcher
import dev.amenokizele.tervyn.domain.model.User
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class RoomLocalUserDataSource @Inject constructor(
    private val userDao: UserDao,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : LocalUserDataSource {
    override suspend fun getUser(userId: String): User? = withContext(ioDispatcher) {
        userDao.getById(userId)?.toDomain()
    }
}
