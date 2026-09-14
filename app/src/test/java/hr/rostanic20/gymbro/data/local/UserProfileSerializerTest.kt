package hr.rostanic20.gymbro.data.local

import androidx.datastore.core.CorruptionException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream

class UserProfileSerializerTest {

    private suspend fun encode(profile: UserProfile): String =
        ByteArrayOutputStream().also { UserProfileSerializer.writeTo(profile, it) }.toString(Charsets.UTF_8)

    private suspend fun decode(json: String): UserProfile =
        UserProfileSerializer.readFrom(json.byteInputStream())

    @Test
    fun `default values are written so a later code change cannot move saved targets`() = runTest {
        val json = encode(UserProfile(programStartEpochDay = 20_710))

        listOf("maintenanceKcal", "surplusKcal", "kcalAdjustment", "proteinG", "fatG").forEach {
            assertTrue("$it missing from $json", "\"$it\"" in json)
        }
    }

    @Test
    fun `round trip keeps every field`() = runTest {
        val profile = UserProfile(
            programStartEpochDay = 20_710,
            maintenanceKcal = 2500,
            surplusKcal = 300,
            kcalAdjustment = -150,
            proteinG = 150,
            fatG = 80,
        )

        assertEquals(profile, decode(encode(profile)))
    }

    @Test
    fun `unknown keys are skipped instead of wiping the profile`() = runTest {
        val profile = decode("""{"maintenanceKcal":2600,"removedSetting":true}""")

        assertEquals(UserProfile(maintenanceKcal = 2600), profile)
    }

    @Test(expected = CorruptionException::class)
    fun `malformed file is reported as corruption`() = runTest {
        decode("not json")
    }
}
