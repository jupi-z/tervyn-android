package dev.amenokizele.tervyn.data.auth.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.di.IoDispatcher
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.security.KeyStore
import java.security.UnrecoverableKeyException
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class AndroidKeystoreSessionStore @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val codec: StoredSessionCodec,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : SecureSessionStore {
    constructor(
        context: Context,
        ioDispatcher: CoroutineDispatcher,
        sessionFileName: String,
        keyAlias: String
    ) : this(
        context = context,
        codec = StoredSessionCodec(),
        ioDispatcher = ioDispatcher
    ) {
        this.sessionFileName = validatedFileName(sessionFileName)
        this.keyAlias = keyAlias
    }

    internal constructor(
        context: Context,
        ioDispatcher: CoroutineDispatcher,
        sessionFileName: String,
        keyAlias: String,
        keyProviderForTest: () -> SecretKey
    ) : this(
        context = context,
        ioDispatcher = ioDispatcher,
        sessionFileName = sessionFileName,
        keyAlias = keyAlias
    ) {
        this.keyProviderForTest = keyProviderForTest
    }

    private var sessionFileName: String = DEFAULT_SESSION_FILE_NAME
    private var keyAlias: String = DEFAULT_KEY_ALIAS
    private var keyProviderForTest: (() -> SecretKey)? = null
    private var legacyCleanupAttempted = false

    override suspend fun read(): AppResult<StoredSession?> = withContext(ioDispatcher) {
        cleanupLegacyStorageBestEffort()
        try {
            val file = sessionFile()
            val atomicFile = AtomicFile(file)

            when (val envelope = readEnvelope(atomicFile)) {
                is SecureSessionEnvelope.Empty -> AppResult.Success(null)
                is SecureSessionEnvelope.Session -> readSession(envelope)
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: FileNotFoundException) {
            AppResult.Success(null)
        } catch (exception: KeyPermanentlyInvalidatedException) {
            failClosed(deleteKey = true)
        } catch (exception: UnrecoverableKeyException) {
            failClosed(deleteKey = true)
        } catch (exception: AEADBadTagException) {
            failClosed()
        } catch (exception: Exception) {
            failClosed()
        }
    }

    override suspend fun write(session: StoredSession): AppResult<Unit> = withContext(ioDispatcher) {
        cleanupLegacyStorageBestEffort()
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val ciphertext = cipher.doFinal(codec.encode(session))
            writeEnvelopeAtomically(
                envelope = SecureSessionEnvelope.Session(iv = cipher.iv, ciphertext = ciphertext),
                failureCode = "secure_session_write_failed"
            )
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: KeyPermanentlyInvalidatedException) {
            failClosedAfterInvalidKeyWrite()
        } catch (exception: UnrecoverableKeyException) {
            failClosedAfterInvalidKeyWrite()
        } catch (exception: Exception) {
            AppResult.Failure(AppError.Storage("secure_session_write_failed"))
        }
    }

    override suspend fun clear(): AppResult<Unit> = withContext(ioDispatcher) {
        cleanupLegacyStorageBestEffort()
        writeEnvelopeAtomically(
            envelope = SecureSessionEnvelope.Empty,
            failureCode = "secure_session_clear_failed"
        )
    }

    internal fun deleteKeyForTest() {
        deleteKey()
    }

    private fun readSession(envelope: SecureSessionEnvelope.Session): AppResult<StoredSession?> {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(GCM_TAG_BITS, envelope.iv)
        )
        val session = codec.decode(cipher.doFinal(envelope.ciphertext))
        return if (session.schemaVersion == StoredSession.SCHEMA_VERSION) {
            AppResult.Success(session)
        } else {
            failClosed()
        }
    }

    private fun readEnvelope(atomicFile: AtomicFile): SecureSessionEnvelope {
        val stream = atomicFile.openRead()
        if (stream.channel.size() > MAX_FILE_BYTES) {
            stream.close()
            throw IOException("secure session file too large")
        }

        DataInputStream(stream.buffered()).use { input ->
            if (input.readInt() != MAGIC) throw IOException("invalid secure session magic")
            if (input.readInt() != STORAGE_FORMAT_VERSION) throw IOException("unsupported secure session version")

            val recordType = input.readInt()
            val ivLength = input.readInt()
            val ciphertextLength = input.readInt()
            if (ivLength < 0 || ivLength > MAX_IV_BYTES) throw IOException("invalid secure session IV length")
            if (ciphertextLength < 0 || ciphertextLength > MAX_CIPHERTEXT_BYTES) {
                throw IOException("invalid secure session ciphertext length")
            }

            return when (recordType) {
                RECORD_EMPTY -> {
                    if (ivLength != 0 || ciphertextLength != 0) throw IOException("invalid empty secure session")
                    if (input.read() != -1) throw IOException("trailing secure session data")
                    SecureSessionEnvelope.Empty
                }

                RECORD_SESSION -> {
                    if (ivLength == 0 || ciphertextLength == 0) throw IOException("empty encrypted secure session")
                    val iv = ByteArray(ivLength)
                    val ciphertext = ByteArray(ciphertextLength)
                    input.readFully(iv)
                    input.readFully(ciphertext)
                    if (input.read() != -1) throw IOException("trailing secure session data")
                    SecureSessionEnvelope.Session(iv = iv, ciphertext = ciphertext)
                }

                else -> throw IOException("unknown secure session record type")
            }
        }
    }

    private fun writeEnvelopeAtomically(
        envelope: SecureSessionEnvelope,
        failureCode: String
    ): AppResult<Unit> {
        var atomicFile: AtomicFile? = null
        var stream: FileOutputStream? = null
        return try {
            atomicFile = AtomicFile(sessionFile())
            stream = atomicFile.startWrite()
            val output = DataOutputStream(stream)
            writeEnvelope(output, envelope)
            output.flush()
            atomicFile.finishWrite(stream)
            stream = null
            AppResult.Success(Unit)
        } catch (exception: CancellationException) {
            if (atomicFile != null) {
                stream?.let { failWriteBestEffort(atomicFile, it) }
            }
            throw exception
        } catch (exception: Exception) {
            if (atomicFile != null) {
                stream?.let { failWriteBestEffort(atomicFile, it) }
            }
            AppResult.Failure(AppError.Storage(failureCode))
        }
    }

    private fun writeEnvelope(output: DataOutputStream, envelope: SecureSessionEnvelope) {
        output.writeInt(MAGIC)
        output.writeInt(STORAGE_FORMAT_VERSION)
        when (envelope) {
            SecureSessionEnvelope.Empty -> {
                output.writeInt(RECORD_EMPTY)
                output.writeInt(0)
                output.writeInt(0)
            }

            is SecureSessionEnvelope.Session -> {
                require(envelope.iv.isNotEmpty() && envelope.iv.size <= MAX_IV_BYTES)
                require(envelope.ciphertext.isNotEmpty() && envelope.ciphertext.size <= MAX_CIPHERTEXT_BYTES)
                output.writeInt(RECORD_SESSION)
                output.writeInt(envelope.iv.size)
                output.writeInt(envelope.ciphertext.size)
                output.write(envelope.iv)
                output.write(envelope.ciphertext)
            }
        }
    }

    private fun failWriteBestEffort(atomicFile: AtomicFile, stream: FileOutputStream) {
        try {
            atomicFile.failWrite(stream)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // Preserve the original write failure result.
        }
    }

    private fun failClosed(deleteKey: Boolean = false): AppResult.Success<Nothing?> {
        if (deleteKey) {
            deleteKeyBestEffort()
        }
        writeEmptyTombstoneBestEffort()
        return AppResult.Success(null)
    }

    private fun failClosedAfterInvalidKeyWrite(): AppResult.Failure {
        deleteKeyBestEffort()
        writeEmptyTombstoneBestEffort()
        return AppResult.Failure(AppError.Storage("secure_session_write_failed"))
    }

    private fun writeEmptyTombstoneBestEffort() {
        try {
            writeEnvelopeAtomically(SecureSessionEnvelope.Empty, "secure_session_clear_failed")
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // Recovery must stay fail-closed even if cleanup cannot persist.
        }
    }

    private fun cleanupLegacyStorageBestEffort() {
        if (legacyCleanupAttempted) return
        legacyCleanupAttempted = true
        try {
            context.getSharedPreferences(LEGACY_PREFERENCES_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // Legacy encrypted demo-session cleanup must not block the v2 backend.
        }

        try {
            deleteKey(LEGACY_KEY_ALIAS)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // The legacy key is best-effort cleanup only.
        }
    }

    private fun deleteKeyBestEffort() {
        try {
            deleteKey()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // An unavailable Keystore must not escape from security recovery.
        }
    }

    private fun sessionFile(): File {
        val directory = File(context.filesDir, SESSION_DIRECTORY)
        if (!directory.exists() && !directory.mkdirs()) {
            throw IOException("secure session directory unavailable")
        }
        if (!directory.isDirectory) {
            throw IOException("secure session directory invalid")
        }
        return File(directory, sessionFileName)
    }

    private fun getOrCreateKey(): SecretKey {
        keyProviderForTest?.let { return it() }
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        (keyStore.getKey(keyAlias, null) as? SecretKey)?.let { return it }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val parameterSpec = KeyGenParameterSpec.Builder(
            keyAlias,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(parameterSpec)
        return keyGenerator.generateKey()
    }

    private fun deleteKey() {
        deleteKey(keyAlias)
    }

    private fun deleteKey(alias: String) {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }
    }

    private sealed interface SecureSessionEnvelope {
        data object Empty : SecureSessionEnvelope
        data class Session(val iv: ByteArray, val ciphertext: ByteArray) : SecureSessionEnvelope
    }

    companion object {
        const val DEFAULT_KEY_ALIAS = "tervyn.session.aes.v2"
        private const val DEFAULT_SESSION_FILE_NAME = "tervyn_secure_session_v2.bin"
        private const val SESSION_DIRECTORY = "secure"
        private const val LEGACY_PREFERENCES_NAME = "tervyn_secure_session"
        private const val LEGACY_KEY_ALIAS = "tervyn.session.aes.v1"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val GCM_TAG_BITS = 128
        private const val MAGIC = 0x5456534E
        private const val STORAGE_FORMAT_VERSION = 2
        private const val RECORD_EMPTY = 0
        private const val RECORD_SESSION = 1
        private const val MAX_FILE_BYTES = 64 * 1024L
        private const val MAX_IV_BYTES = 64
        private const val MAX_CIPHERTEXT_BYTES = 60 * 1024

        private fun validatedFileName(name: String): String {
            require(name.isNotBlank()) { "session file name must not be blank" }
            require(name == File(name).name && !name.contains('/') && !name.contains('\\')) {
                "session file name must not contain path separators"
            }
            return name
        }
    }
}
