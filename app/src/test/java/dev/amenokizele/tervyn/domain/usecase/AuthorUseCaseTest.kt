package dev.amenokizele.tervyn.domain.usecase

import dev.amenokizele.tervyn.FakeTervynClock
import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.data.inmemory.InMemoryJobRepository
import dev.amenokizele.tervyn.data.inmemory.InMemoryStore
import dev.amenokizele.tervyn.domain.model.AddAttachmentRequest
import dev.amenokizele.tervyn.domain.model.AttachmentType
import dev.amenokizele.tervyn.domain.model.AuthState
import dev.amenokizele.tervyn.domain.model.User
import dev.amenokizele.tervyn.domain.repository.AuthRepository
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthorUseCaseTest {
    @Test
    fun addNote_usesAuthenticatedUserIdAsAuthor() = runTest {
        val repository = InMemoryJobRepository(InMemoryStore(), FakeTervynClock())
        val useCase = AddNoteUseCase(repository, authRepository(user("user-X")))

        val result = useCase("job-003", "Note avec auteur courant")

        assertTrue(result is AppResult.Success)
        assertEquals("user-X", (result as AppResult.Success).data.authorUserId)
    }

    @Test
    fun addAttachment_usesAuthenticatedUserIdAsAuthor() = runTest {
        val repository = InMemoryJobRepository(InMemoryStore(), FakeTervynClock())
        val useCase = AddAttachmentUseCase(repository, authRepository(user("user-Y")))

        val result = useCase("job-003", photoRequest())

        assertTrue(result is AppResult.Success)
        assertEquals("user-Y", (result as AppResult.Success).data.authorUserId)
    }

    @Test
    fun addNote_withoutAuthenticatedUserFails() = runTest {
        val repository = InMemoryJobRepository(InMemoryStore(), FakeTervynClock())
        val useCase = AddNoteUseCase(repository, authRepository(null))

        val result = useCase("job-003", "Note refusée")

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AppError.Authentication)
    }

    @Test
    fun addAttachment_withoutAuthenticatedUserFails() = runTest {
        val repository = InMemoryJobRepository(InMemoryStore(), FakeTervynClock())
        val useCase = AddAttachmentUseCase(repository, authRepository(null))

        val result = useCase("job-003", photoRequest())

        assertTrue(result is AppResult.Failure)
        assertTrue((result as AppResult.Failure).error is AppError.Authentication)
    }

    private fun authRepository(user: User?) = object : AuthRepository {
        override val authState: Flow<AuthState> = flowOf(
            if (user == null) AuthState.Unauthenticated else AuthState.Authenticated(user)
        )

        override suspend fun currentAuthenticatedUser(): User? = user

        override suspend fun login(email: String, password: String): AppResult<User> {
            return user?.let { AppResult.Success(it) }
                ?: AppResult.Failure(AppError.Authentication("not_authenticated"))
        }

        override suspend fun logout(): AppResult<Unit> = AppResult.Success(Unit)
    }

    private fun user(id: String) = User(
        id = id,
        email = "$id@tervyn.demo",
        firstName = id,
        lastName = "Tester",
        jobTitle = null,
        avatarUrl = null,
        createdAt = Instant.parse("2026-09-03T10:00:00Z"),
        updatedAt = Instant.parse("2026-09-03T10:00:00Z"),
        lastSyncedAt = null
    )

    private fun photoRequest() = AddAttachmentRequest(
        type = AttachmentType.PHOTO,
        localUri = "tervyn://test/photo/AUTHOR",
        mimeType = "image/jpeg",
        fileName = "Photo author test",
        sizeBytes = 0,
        checksumSha256 = null
    )
}
