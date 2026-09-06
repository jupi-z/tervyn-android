package dev.amenokizele.tervyn.data.auth.session

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.util.Base64
import androidx.test.core.app.ApplicationProvider
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.core.result.AppError
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class AndroidKeystoreSessionStoreTest {
    private lateinit var context: Context
    private lateinit var storageName: String
    private lateinit var keyAlias: String
    private lateinit var store: AndroidKeystoreSessionStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        storageName = "test_secure_session_${UUID.randomUUID()}"
        keyAlias = "tervyn.session.test.${UUID.randomUUID()}"
        store = AndroidKeystoreSessionStore(
            context = context,
            ioDispatcher = Dispatchers.IO,
            preferencesName = storageName,
            keyAlias = keyAlias
        )
    }

    @After
    fun tearDown() = runTest {
        store.clear()
        store.deleteKeyForTest()
    }

    @Test
    fun writeThenReadReturnsEquivalentSession() = runTest {
        val session = session()

        assertEquals(AppResult.Success(Unit), store.write(session))
        val read = store.read()

        assertEquals(AppResult.Success(session), read)
    }

    @Test
    fun encryptedStorageDoesNotContainPlaintextSessionValues() = runTest {
        val session = session()

        store.write(session)
        val raw = context.getSharedPreferences(storageName, Context.MODE_PRIVATE).all.toString()

        assertFalse(raw.contains(session.accessToken))
        assertFalse(raw.contains(session.refreshToken))
        assertFalse(raw.contains(session.userId))
        assertFalse(raw.contains("tervyn2026"))
    }

    @Test
    fun tamperedCiphertextFailsClosedAndClearsStorage() = runTest {
        assertEquals(AppResult.Success(Unit), store.write(session()))
        val preferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE)
        val original = Base64.decode(requireNotNull(preferences.getString("ciphertext", null)), Base64.NO_WRAP)
        val tampered = original.copyOf()
        tampered[0] = (tampered[0].toInt() xor 1).toByte()
        assertEquals(1, original.indices.count { original[it] != tampered[it] })
        val encoded = Base64.encodeToString(tampered, Base64.NO_WRAP)
        assertTrue(tampered.contentEquals(Base64.decode(encoded, Base64.NO_WRAP)))
        assertTrue(preferences.edit().putString("ciphertext", encoded).commit())

        val read = store.read()

        assertTrue(read is AppResult.Success<*>)
        assertEquals(null, (read as AppResult.Success<*>).data)
        assertTrue(preferences.all.isEmpty())
    }

    @Test
    fun corruptReadReturnsNoSessionEvenWhenCleanupThrows() = runTest {
        val failingStore = storeWithContext(object : ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
                throw IllegalStateException("storage unavailable including cleanup")
            }
        })

        assertEquals(AppResult.Success(null), failingStore.read())
    }

    @Test
    fun incompletePayloadFailsClosedAndClearsStorage() = runTest {
        val preferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE)
        assertTrue(preferences.edit().putString("iv", "orphaned").commit())

        assertEquals(AppResult.Success(null), store.read())
        assertTrue(preferences.all.isEmpty())
    }

    @Test
    fun unrecoverableKeyWriteFailsAndRemovesUnusableKeyAndPayload() = runTest {
        assertWriteKeyFailureCleansUp(UnrecoverableKeyException("unusable key"))
    }

    @Test
    fun permanentlyInvalidatedKeyWriteFailsAndRemovesUnusableKeyAndPayload() = runTest {
        assertWriteKeyFailureCleansUp(KeyPermanentlyInvalidatedException())
    }

    @Test
    fun keyWriteFailureDoesNotThrowWhenPayloadCleanupAlsoFails() = runTest {
        val failingStore = storeWithContext(object : ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
                throw UnrecoverableKeyException("storage and cleanup unavailable")
            }
        })

        assertEquals(
            AppResult.Failure(AppError.Storage("secure_session_write_failed")),
            failingStore.write(session())
        )
        assertFalse(KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.containsAlias(keyAlias))
    }

    @Test
    fun cancellationIsRethrownByReadWriteAndClear() = runTest {
        val cancellation = CancellationException("cancelled storage access")
        val failingStore = storeWithContext(object : ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
                throw cancellation
            }
        })
        val operations: List<suspend () -> Any> = listOf(
            { failingStore.read() }, { failingStore.write(session()) }, { failingStore.clear() }
        )
        for (operation in operations) {
            try {
                operation()
                fail("Cancellation must propagate")
            } catch (exception: CancellationException) {
                assertSame(cancellation, exception)
            }
        }
    }

    private suspend fun assertWriteKeyFailureCleansUp(failure: Exception) {
        assertEquals(AppResult.Success(Unit), store.write(session()))
        var failNextAccess = true
        val failingStore = storeWithContext(object : ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
                if (failNextAccess) {
                    failNextAccess = false
                    throw failure
                }
                return super.getSharedPreferences(name, mode)
            }
        })

        assertEquals(
            AppResult.Failure(AppError.Storage("secure_session_write_failed")),
            failingStore.write(session())
        )
        assertTrue(context.getSharedPreferences(storageName, Context.MODE_PRIVATE).all.isEmpty())
        assertFalse(KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.containsAlias(keyAlias))
        assertEquals(AppResult.Success(null), store.read())
        val nextSession = session()
        assertEquals(AppResult.Success(Unit), store.write(nextSession))
        assertEquals(AppResult.Success(nextSession), store.read())
    }

    private fun storeWithContext(storageContext: Context) = AndroidKeystoreSessionStore(
        context = storageContext,
        ioDispatcher = Dispatchers.IO,
        preferencesName = storageName,
        keyAlias = keyAlias
    )

    @Test
    fun clearRemovesSessionAndRawStorage() = runTest {
        store.write(session())

        assertEquals(AppResult.Success(Unit), store.clear())
        val read = store.read()

        assertEquals(AppResult.Success(null), read)
        assertTrue(context.getSharedPreferences(storageName, Context.MODE_PRIVATE).all.isEmpty())
    }

    private fun session() = StoredSession(
        userId = "user-amina",
        accessToken = "local_demo_access_${UUID.randomUUID()}",
        refreshToken = "local_demo_refresh_${UUID.randomUUID()}",
        issuedAt = Instant.parse("2026-09-05T10:00:00Z"),
        accessTokenExpiresAt = Instant.parse("2026-09-05T10:15:00Z"),
        refreshTokenExpiresAt = Instant.parse("2026-09-12T10:00:00Z"),
        schemaVersion = 1
    )
}
