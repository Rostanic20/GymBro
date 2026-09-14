package hr.rostanic20.gymbro.data.local

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import kotlinx.serialization.json.encodeToStream
import java.io.InputStream
import java.io.OutputStream

@OptIn(ExperimentalSerializationApi::class)
object UserProfileSerializer : Serializer<UserProfile> {
    private val json = Json { ignoreUnknownKeys = true }

    override val defaultValue: UserProfile
        get() = UserProfile()

    override suspend fun readFrom(input: InputStream): UserProfile {
        return try {
            json.decodeFromStream(stream = input)
        } catch (e: SerializationException) {
            throw CorruptionException("Failed to deserialize UserProfile", e)
        }
    }

    override suspend fun writeTo(
        t: UserProfile,
        output: OutputStream,
    ) {
        json.encodeToStream(value = t, stream = output)
    }
}
