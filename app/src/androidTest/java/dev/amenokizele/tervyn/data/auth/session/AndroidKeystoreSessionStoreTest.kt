package dev.amenokizele.tervyn.data.auth.session

import android.content.Context
import android.content.ContextWrapper
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import androidx.test.core.app.ApplicationProvider
import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import java.time.Instant
import java.util.UUID
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class AndroidKeystoreSessionStoreTest {
    private lateinit var context: Context
    private lateinit var sessionFileName: String
    private lateinit var keyAlias: String
    private lateinit var store: AndroidKeystoreSessionStore

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        sessionFileName = "test_secure_session_${UUID.randomUUID()}.bin"
        keyAlias = "tervyn.session.test.${UUID.randomUUID()}"
        store = newStore()
    }

    @After
    fun tearDown() = runTest {
        store.clear()
        store.deleteKeyForTest()
        sessionFile().delete()
        deleteKey(LEGACY_KEY_ALIAS)
        try {
            context.getSharedPreferences(LEGACY_PREFERENCES_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        } catch (_: Exception) {
            // The production legacy cleanup is best-effort; teardown must not hide test outcomes.
        }
    }

    @Test
    fun writeThenReadReturnsEquivalentSessionAndPersistsAcrossInstances() = runTest {
        val session = session()

        assertEquals(AppResult.Success(Unit), store.write(session))

        assertEquals(AppResult.Success(session), store.read())
        assertEquals(AppResult.Success(session), newStore().read())
    }

    @Test
    fun encryptedFileDoesNotContainPlaintextSessionValues() = runTest {
        val session = session()

        assertEquals(AppResult.Success(Unit), store.write(session))
        val raw = sessionFile().readBytes()

        assertFalse(raw.containsText(session.userId))
        assertFalse(raw.containsText(session.accessToken))
        assertFalse(raw.containsText(session.refreshToken))
        assertFalse(raw.containsText("tervyn2026"))
        assertFalse(raw.containsText("amina@tervyn.demo"))
    }

    @Test
    fun atomicFileRollbackKeepsPreviousSessionAfterPartialSessionWrite() = runTest {
        val sessionA = session(userId = "user-a")
        assertEquals(AppResult.Success(Unit), store.write(sessionA))
        val durableA = sessionFile().readBytes()

        val atomicFile = AtomicFile(sessionFile())
        val stream = atomicFile.startWrite()
        stream.write(envelopeBytes(RECORD_SESSION, iv = byteArrayOf(1, 2, 3), ciphertext = byteArrayOf(4, 5)))
        atomicFile.failWrite(stream)

        assertTrue(durableA.contentEquals(sessionFile().readBytes()))
        assertEquals(AppResult.Success(sessionA), store.read())
    }

    @Test
    fun clearWritesDurableEmptyTombstoneAcrossInstances() = runTest {
        assertEquals(AppResult.Success(Unit), store.write(session()))

        assertEquals(AppResult.Success(Unit), store.clear())

        assertEquals(AppResult.Success(null), store.read())
        assertEquals(AppResult.Success(null), newStore().read())
        assertTombstone()
    }

    @Test
    fun clearRollbackKeepsPreviousSessionWhenTombstoneWriteFails() = runTest {
        val sessionA = session(userId = "user-a")
        assertEquals(AppResult.Success(Unit), store.write(sessionA))
        val durableA = sessionFile().readBytes()

        val atomicFile = AtomicFile(sessionFile())
        val stream = atomicFile.startWrite()
        stream.write(envelopeBytes(RECORD_EMPTY))
        atomicFile.failWrite(stream)

        assertTrue(durableA.contentEquals(sessionFile().readBytes()))
        assertEquals(AppResult.Success(sessionA), store.read())
    }

    @Test
    fun tamperedCiphertextFailsClosedAndWritesEmptyTombstone() = runTest {
        assertEquals(AppResult.Success(Unit), store.write(session()))
        val original = readRawEnvelope()
        val tampered = original.ciphertext.copyOf()
        tampered[0] = (tampered[0].toInt() xor 1).toByte()
        assertEquals(1, original.ciphertext.indices.count { original.ciphertext[it] != tampered[it] })
        overwriteEnvelope(envelopeBytes(RECORD_SESSION, iv = original.iv, ciphertext = tampered))

        assertEquals(AppResult.Success(null), store.read())

        assertTombstone()
        assertEquals(AppResult.Success(null), store.read())
    }

    @Test
    fun tamperedIvFailsClosedAndWritesEmptyTombstone() = runTest {
        assertEquals(AppResult.Success(Unit), store.write(session()))
        val original = readRawEnvelope()
        val tampered = original.iv.copyOf()
        tampered[0] = (tampered[0].toInt() xor 1).toByte()
        assertEquals(1, original.iv.indices.count { original.iv[it] != tampered[it] })
        overwriteEnvelope(envelopeBytes(RECORD_SESSION, iv = tampered, ciphertext = original.ciphertext))

        assertEquals(AppResult.Success(null), store.read())

        assertTombstone()
    }

    @Test
    fun truncatedFileFailsClosedWithoutCrash() = runTest {
        overwriteEnvelope(envelopePrefixBytes())

        assertEquals(AppResult.Success(null), store.read())

        assertTombstone()
    }

    @Test
    fun oversizedCiphertextLengthFailsClosedWithoutAllocation() = runTest {
        overwriteEnvelope(
            envelopeBytes(
                recordType = RECORD_SESSION,
                iv = ByteArray(12) { 7 },
                ciphertext = ByteArray(0),
                ciphertextLengthOverride = Int.MAX_VALUE
            )
        )

        assertEquals(AppResult.Success(null), store.read())

        assertTombstone()
    }

    @Test
    fun unknownStorageVersionFailsClosed() = runTest {
        overwriteEnvelope(envelopeBytes(RECORD_EMPTY, version = 999))

        assertEquals(AppResult.Success(null), store.read())

        assertTombstone()
    }

    @Test
    fun unknownRecordTypeFailsClosed() = runTest {
        overwriteEnvelope(envelopeBytes(recordType = 99))

        assertEquals(AppResult.Success(null), store.read())

        assertTombstone()
    }

    @Test
    fun legacySharedPreferencesAreClearedWithoutMigratingSession() = runTest {
        val legacy = FakeSharedPreferences(
            mutableMapOf(
                "storage_schema_version" to 1,
                "iv" to "legacy-iv",
                "ciphertext" to "legacy-ciphertext"
            )
        )
        val legacyStore = storeWithContext(object : ContextWrapper(context) {
            override fun getSharedPreferences(name: String, mode: Int): SharedPreferences {
                return legacy
            }
        })

        assertEquals(AppResult.Success(null), legacyStore.read())

        assertTrue(legacy.all.isEmpty())
        assertFalse(sessionFile().exists())
    }

    @Test
    fun legacyKeyAliasIsDeletedBestEffort() = runTest {
        createLegacyKeyAlias()
        assertTrue(keyStore().containsAlias(LEGACY_KEY_ALIAS))

        assertEquals(AppResult.Success(null), store.read())

        assertFalse(keyStore().containsAlias(LEGACY_KEY_ALIAS))
    }

    @Test
    fun unrecoverableKeyWriteFailsTombstonesAndRemovesUnusableKey() = runTest {
        assertWriteKeyFailureTombstones(UnrecoverableKeyException("unusable key"))
    }

    @Test
    fun permanentlyInvalidatedKeyWriteFailsTombstonesAndRemovesUnusableKey() = runTest {
        assertWriteKeyFailureTombstones(KeyPermanentlyInvalidatedException())
    }

    @Test
    fun keyInvalidationReadFailsClosedAndTombstones() = runTest {
        assertEquals(AppResult.Success(Unit), store.write(session()))
        val failingStore = storeWithKeyProvider { throw UnrecoverableKeyException("unusable key") }

        assertEquals(AppResult.Success(null), failingStore.read())

        assertFalse(keyStore().containsAlias(keyAlias))
        assertTombstone()
    }

    @Test
    fun cancellationIsRethrownByReadWriteAndClear() = runTest {
        val cancellation = CancellationException("cancelled storage access")
        val failingStore = storeWithContext(object : ContextWrapper(context) {
            override fun getFilesDir(): File {
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

    private suspend fun assertWriteKeyFailureTombstones(failure: Exception) {
        assertEquals(AppResult.Success(Unit), store.write(session()))
        val failingStore = storeWithKeyProvider { throw failure }

        assertEquals(
            AppResult.Failure(AppError.Storage("secure_session_write_failed")),
            failingStore.write(session())
        )
        assertFalse(keyStore().containsAlias(keyAlias))
        assertEquals(AppResult.Success(null), store.read())
        assertTombstone()

        val nextSession = session()
        assertEquals(AppResult.Success(Unit), store.write(nextSession))
        assertEquals(AppResult.Success(nextSession), store.read())
    }

    private fun newStore() = AndroidKeystoreSessionStore(
        context = context,
        ioDispatcher = Dispatchers.IO,
        sessionFileName = sessionFileName,
        keyAlias = keyAlias
    )

    private fun storeWithContext(storageContext: Context) = AndroidKeystoreSessionStore(
        context = storageContext,
        ioDispatcher = Dispatchers.IO,
        sessionFileName = sessionFileName,
        keyAlias = keyAlias
    )

    private fun storeWithKeyProvider(keyProvider: () -> SecretKey) = AndroidKeystoreSessionStore(
        context = context,
        ioDispatcher = Dispatchers.IO,
        sessionFileName = sessionFileName,
        keyAlias = keyAlias,
        keyProviderForTest = keyProvider
    )

    private fun sessionFile() = File(File(context.filesDir, "secure"), sessionFileName)

    private fun overwriteEnvelope(bytes: ByteArray) {
        val file = sessionFile()
        requireNotNull(file.parentFile).mkdirs()
        val atomicFile = AtomicFile(file)
        val stream = atomicFile.startWrite()
        try {
            stream.write(bytes)
            atomicFile.finishWrite(stream)
        } catch (exception: Exception) {
            atomicFile.failWrite(stream)
            throw exception
        }
    }

    private fun readRawEnvelope(): RawEnvelope {
        DataInputStream(sessionFile().inputStream().buffered()).use { input ->
            assertEquals(MAGIC, input.readInt())
            assertEquals(STORAGE_FORMAT_VERSION, input.readInt())
            val recordType = input.readInt()
            val iv = ByteArray(input.readInt())
            val ciphertext = ByteArray(input.readInt())
            input.readFully(iv)
            input.readFully(ciphertext)
            assertEquals(-1, input.read())
            return RawEnvelope(recordType, iv, ciphertext)
        }
    }

    private fun assertTombstone() {
        val envelope = readRawEnvelope()
        assertEquals(RECORD_EMPTY, envelope.recordType)
        assertTrue(envelope.iv.isEmpty())
        assertTrue(envelope.ciphertext.isEmpty())
    }

    private fun envelopeBytes(
        recordType: Int,
        version: Int = STORAGE_FORMAT_VERSION,
        iv: ByteArray = ByteArray(0),
        ciphertext: ByteArray = ByteArray(0),
        ciphertextLengthOverride: Int? = null
    ): ByteArray {
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { data ->
            data.writeInt(MAGIC)
            data.writeInt(version)
            data.writeInt(recordType)
            data.writeInt(iv.size)
            data.writeInt(ciphertextLengthOverride ?: ciphertext.size)
            data.write(iv)
            data.write(ciphertext)
        }
        return output.toByteArray()
    }

    private fun envelopePrefixBytes(): ByteArray {
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { data ->
            data.writeInt(MAGIC)
            data.writeInt(STORAGE_FORMAT_VERSION)
            data.writeInt(RECORD_SESSION)
        }
        return output.toByteArray()
    }

    private fun createLegacyKeyAlias() {
        deleteKey(LEGACY_KEY_ALIAS)
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val parameterSpec = KeyGenParameterSpec.Builder(
            LEGACY_KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(parameterSpec)
        keyGenerator.generateKey()
    }

    private fun deleteKey(alias: String) {
        val keyStore = keyStore()
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }
    }

    private fun keyStore(): KeyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun ByteArray.containsText(value: String): Boolean {
        val pattern = value.toByteArray(Charsets.UTF_8)
        return pattern.isNotEmpty() && indices.any { index ->
            index + pattern.size <= size && pattern.indices.all { offset -> this[index + offset] == pattern[offset] }
        }
    }

    private fun session(userId: String = "user-amina") = StoredSession(
        userId = userId,
        accessToken = "local_demo_access_${UUID.randomUUID()}",
        refreshToken = "local_demo_refresh_${UUID.randomUUID()}",
        issuedAt = Instant.parse("2026-09-05T10:00:00Z"),
        accessTokenExpiresAt = Instant.parse("2026-09-05T10:15:00Z"),
        refreshTokenExpiresAt = Instant.parse("2026-09-12T10:00:00Z"),
        schemaVersion = 1
    )

    private data class RawEnvelope(
        val recordType: Int,
        val iv: ByteArray,
        val ciphertext: ByteArray
    )

    private class FakeSharedPreferences(
        private val values: MutableMap<String, Any?>
    ) : SharedPreferences {
        override fun getAll(): MutableMap<String, *> = values.toMutableMap()

        override fun getString(key: String, defValue: String?): String? = values[key] as? String ?: defValue

        override fun getStringSet(key: String, defValues: MutableSet<String>?): MutableSet<String>? = defValues

        override fun getInt(key: String, defValue: Int): Int = values[key] as? Int ?: defValue

        override fun getLong(key: String, defValue: Long): Long = values[key] as? Long ?: defValue

        override fun getFloat(key: String, defValue: Float): Float = values[key] as? Float ?: defValue

        override fun getBoolean(key: String, defValue: Boolean): Boolean = values[key] as? Boolean ?: defValue

        override fun contains(key: String): Boolean = values.containsKey(key)

        override fun edit(): SharedPreferences.Editor = FakeEditor(values)

        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = Unit

        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = Unit
    }

    private class FakeEditor(
        private val values: MutableMap<String, Any?>
    ) : SharedPreferences.Editor {
        private var clearRequested = false
        private val pending = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()

        override fun putString(key: String, value: String?): SharedPreferences.Editor = apply {
            pending[key] = value
        }

        override fun putStringSet(key: String, values: MutableSet<String>?): SharedPreferences.Editor = apply {
            pending[key] = values
        }

        override fun putInt(key: String, value: Int): SharedPreferences.Editor = apply {
            pending[key] = value
        }

        override fun putLong(key: String, value: Long): SharedPreferences.Editor = apply {
            pending[key] = value
        }

        override fun putFloat(key: String, value: Float): SharedPreferences.Editor = apply {
            pending[key] = value
        }

        override fun putBoolean(key: String, value: Boolean): SharedPreferences.Editor = apply {
            pending[key] = value
        }

        override fun remove(key: String): SharedPreferences.Editor = apply {
            removals += key
        }

        override fun clear(): SharedPreferences.Editor = apply {
            clearRequested = true
        }

        override fun commit(): Boolean {
            if (clearRequested) values.clear()
            removals.forEach(values::remove)
            values.putAll(pending)
            return true
        }

        override fun apply() {
            commit()
        }
    }

    private companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val LEGACY_PREFERENCES_NAME = "tervyn_secure_session"
        private const val LEGACY_KEY_ALIAS = "tervyn.session.aes.v1"
        private const val MAGIC = 0x5456534E
        private const val STORAGE_FORMAT_VERSION = 2
        private const val RECORD_EMPTY = 0
        private const val RECORD_SESSION = 1
    }
}
