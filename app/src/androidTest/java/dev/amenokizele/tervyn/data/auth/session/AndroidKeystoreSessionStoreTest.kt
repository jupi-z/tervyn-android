package dev.amenokizele.tervyn.data.auth.session

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import dev.amenokizele.tervyn.core.result.AppResult
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
        store.write(session())
        val preferences = context.getSharedPreferences(storageName, Context.MODE_PRIVATE)
        preferences.edit().putString("ciphertext", "tampered").commit()

        val read = store.read()

        assertTrue(read is AppResult.Success<*>)
        assertEquals(null, (read as AppResult.Success<*>).data)
        assertTrue(preferences.all.isEmpty())
    }

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
