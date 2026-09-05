package dev.amenokizele.tervyn.data.auth.session

import java.time.Instant
import javax.inject.Inject
import org.json.JSONObject

class StoredSessionCodec @Inject constructor() {
    fun encode(session: StoredSession): ByteArray {
        val json = JSONObject()
            .put("schemaVersion", session.schemaVersion)
            .put("userId", session.userId)
            .put("accessToken", session.accessToken)
            .put("refreshToken", session.refreshToken)
            .put("issuedAt", session.issuedAt.toString())
            .put("accessTokenExpiresAt", session.accessTokenExpiresAt.toString())
            .put("refreshTokenExpiresAt", session.refreshTokenExpiresAt.toString())

        return json.toString().toByteArray(Charsets.UTF_8)
    }

    fun decode(payload: ByteArray): StoredSession {
        val json = JSONObject(payload.toString(Charsets.UTF_8))
        return StoredSession(
            userId = json.getString("userId"),
            accessToken = json.getString("accessToken"),
            refreshToken = json.getString("refreshToken"),
            issuedAt = Instant.parse(json.getString("issuedAt")),
            accessTokenExpiresAt = Instant.parse(json.getString("accessTokenExpiresAt")),
            refreshTokenExpiresAt = Instant.parse(json.getString("refreshTokenExpiresAt")),
            schemaVersion = json.getInt("schemaVersion")
        )
    }
}
