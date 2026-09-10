package dev.amenokizele.tervyn.data.auth.local

import android.database.SQLException
import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.data.local.dao.UserDao
import dev.amenokizele.tervyn.data.local.mapper.toEntity
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

    override suspend fun upsertUser(user: User): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            userDao.upsert(user.toEntity())
            AppResult.Success(Unit)
        } catch (_: SQLException) {
            AppResult.Failure(AppError.Storage("local_database_error"))
        }
    }
}
