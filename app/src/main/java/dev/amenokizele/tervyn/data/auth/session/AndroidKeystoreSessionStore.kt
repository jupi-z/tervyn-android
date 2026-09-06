package dev.amenokizele.tervyn.data.auth.session

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import android.util.Base64
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.amenokizele.tervyn.core.result.AppError
import dev.amenokizele.tervyn.core.result.AppResult
import dev.amenokizele.tervyn.di.IoDispatcher
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
        preferencesName: String,
        keyAlias: String
    ) : this(
        context = context,
        codec = StoredSessionCodec(),
        ioDispatcher = ioDispatcher
    ) {
        this.preferencesName = preferencesName
        this.keyAlias = keyAlias
    }

    private var preferencesName: String = DEFAULT_PREFERENCES_NAME
    private var keyAlias: String = DEFAULT_KEY_ALIAS

    override suspend fun read(): AppResult<StoredSession?> = withContext(ioDispatcher) {
        try {
            val ciphertext = preferences().getString(KEY_CIPHERTEXT, null)
            val iv = preferences().getString(KEY_IV, null)
            if (ciphertext.isNullOrBlank() || iv.isNullOrBlank()) {
                return@withContext if (preferences().all.isEmpty()) AppResult.Success(null) else failClosed()
            }

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(GCM_TAG_BITS, decode(iv)))
            val session = codec.decode(cipher.doFinal(decode(ciphertext)))
            if (session.schemaVersion == StoredSession.SCHEMA_VERSION) {
                AppResult.Success(session)
            } else {
                failClosed()
            }
        } catch (exception: CancellationException) {
            throw exception
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
        try {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
            val ciphertext = cipher.doFinal(codec.encode(session))
            val committed = preferences().edit()
                .putInt(KEY_STORAGE_SCHEMA_VERSION, STORAGE_SCHEMA_VERSION)
                .putString(KEY_IV, encode(cipher.iv))
                .putString(KEY_CIPHERTEXT, encode(ciphertext))
                .commit()

            if (committed) {
                AppResult.Success(Unit)
            } else {
                AppResult.Failure(AppError.Storage("secure_session_write_failed"))
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: KeyPermanentlyInvalidatedException) {
            failClosed(deleteKey = true)
            AppResult.Failure(AppError.Storage("secure_session_write_failed"))
        } catch (exception: UnrecoverableKeyException) {
            failClosed(deleteKey = true)
            AppResult.Failure(AppError.Storage("secure_session_write_failed"))
        } catch (exception: Exception) {
            AppResult.Failure(AppError.Storage("secure_session_write_failed"))
        }
    }

    override suspend fun clear(): AppResult<Unit> = withContext(ioDispatcher) {
        try {
            if (clearStoredPayload()) {
                AppResult.Success(Unit)
            } else {
                AppResult.Failure(AppError.Storage("secure_session_clear_failed"))
            }
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: Exception) {
            AppResult.Failure(AppError.Storage("secure_session_clear_failed"))
        }
    }

    internal fun deleteKeyForTest() {
        deleteKey()
    }

    private fun preferences() = context.getSharedPreferences(preferencesName, Context.MODE_PRIVATE)

    private fun clearStoredPayload(): Boolean = preferences().edit().clear().commit()

    private fun failClosed(deleteKey: Boolean = false): AppResult.Success<Nothing?> {
        clearStoredPayloadBestEffort()
        if (deleteKey) {
            deleteKeyBestEffort()
        }
        return AppResult.Success(null)
    }

    private fun clearStoredPayloadBestEffort() {
        try {
            clearStoredPayload()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // Recovery must remain fail-closed even when storage is unavailable.
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

    private fun getOrCreateKey(): SecretKey {
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
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(keyAlias)) {
            keyStore.deleteEntry(keyAlias)
        }
    }

    private fun encode(bytes: ByteArray): String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    private fun decode(value: String): ByteArray = Base64.decode(value, Base64.NO_WRAP)

    companion object {
        const val DEFAULT_KEY_ALIAS = "tervyn.session.aes.v1"
        private const val DEFAULT_PREFERENCES_NAME = "tervyn_secure_session"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val GCM_TAG_BITS = 128
        private const val STORAGE_SCHEMA_VERSION = 1
        private const val KEY_STORAGE_SCHEMA_VERSION = "storage_schema_version"
        private const val KEY_IV = "iv"
        private const val KEY_CIPHERTEXT = "ciphertext"
    }
}
