package computer.lil.quilt.model

import androidx.test.runner.AndroidJUnit4
import computer.lil.quilt.api.Constants
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class ExtendedMessageTest {
    @Test
    fun testParsing() {
        val toParse = "{\n" +
                "  \"key\": \"%YkBqF4ACVQZbk5d3IpuYrtzHbQK5+x6k6fhgAc+c9Dw=.sha256\",\n" +
                "  \"value\": {\n" +
                "    \"previous\": \"%PF8rYIG4jnZnxUxZhFrJLSMiB7HarO3v1Bn0EH8CvYU=.sha256\",\n" +
                "    \"sequence\": 5,\n" +
                "    \"author\": \"@zgThI3hlrpVWAISAdknwgz2l8KoT1v6V6v3bdZZ11jM=.ed25519\",\n" +
                "    \"timestamp\": 1553032880238,\n" +
                "    \"hash\": \"sha256\",\n" +
                "    \"content\": {\n" +
                "      \"type\": \"post\",\n" +
                "      \"text\": \"testing post\"\n" +
                "    },\n" +
                "    \"signature\": \"4RBCI5QEtJiiKy0JU1vFxJhEg8EVwhrbtCp1xTFivA6odNI7XsaP/A3prKnf1+ZbBQpDOjSiMtvLNZxc2VdxDw==.sig.ed25519\"\n" +
                "  },\n" +
                "  \"timestamp\": 1553032880240\n" +
                "}"

        val moshi = Constants.getMoshiInstance()

        val extendedMessage = moshi.adapter(ExtendedMessage::class.java).fromJson(toParse)
        Assert.assertEquals(extendedMessage?.key, Identifier.fromString("%YkBqF4ACVQZbk5d3IpuYrtzHbQK5+x6k6fhgAc+c9Dw=.sha256"))
        Assert.assertEquals(extendedMessage?.timestamp, Date(1553032880240))
        Assert.assertEquals(extendedMessage?.value?.signature, "4RBCI5QEtJiiKy0JU1vFxJhEg8EVwhrbtCp1xTFivA6odNI7XsaP/A3prKnf1+ZbBQpDOjSiMtvLNZxc2VdxDw==.sig.ed25519")
    }
}