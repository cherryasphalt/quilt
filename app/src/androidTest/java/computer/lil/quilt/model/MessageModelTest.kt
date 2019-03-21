package computer.lil.quilt.model

import android.util.Base64
import androidx.test.runner.AndroidJUnit4
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import computer.lil.quilt.identity.BasicIdentityHandler
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@RunWith(AndroidJUnit4::class)
class MessageModelTest {
    val moshi = Moshi.Builder()
        .add(Identifier.IdentifierJsonAdapter())
        .add(Adapters.DataTypeAdapter())
        .add(
            PolymorphicJsonAdapterFactory.of(Content::class.java, "type")
                .withSubtype(Content.Post::class.java, "post")
                .withSubtype(Content.Pub::class.java, "pub")
                .withSubtype(Content.Contact::class.java, "contact")
        ).build()

    @Test
    fun testContentParsing() {
        val toParse = "{\n" +
                "    \"previous\": \"%O3I3w1pUZqCh1/DxoO/fGYdpn2nYnth+OqXHwdOodUg=.sha256\",\n" +
                "    \"author\": \"@EMovhfIrFk4NihAKnRNhrfRaqIhBv1Wj8pTxJNgvCCY=.ed25519\",\n" +
                "    \"sequence\": 35,\n" +
                "    \"timestamp\": 1449202158507,\n" +
                "    \"hash\": \"sha256\",\n" +
                "    \"content\": {\n" +
                "      \"type\": \"post\",\n" +
                "      \"text\": \"@cel would tabs be an easier way to do this? shift+click on a link to open a tab?\",\n" +
                "      \"root\": \"%yAvDwopppOmCXAU5xj5KOuLkuYp+CkUicmEJbgJVrbo=.sha256\",\n" +
                "      \"branch\": \"%LQQ53cFB816iAbayxwuLjVLmuCwt1J2erfMge4chSC4=.sha256\",\n" +
                "      \"mentions\": [\n" +
                "        {\n" +
                "          \"link\": \"@f/6sQ6d2CMxRUhLpspgGIulDxDCwYD7DzFzPNr7u5AU=.ed25519\",\n" +
                "          \"name\": \"cel\"\n" +
                "        }\n" +
                "      ]\n" +
                "    },\n" +
                "    \"signature\": \"Go98D7qqvvtaGkGPPttBcHsIuTF+s3FmV5ChNWAhifpdlN9UkmkUd39GQaqDUgs9T0bkXgZByLsdZ31MH5tMBQ==.sig.ed25519\"\n" +
                "  }"

        val message = moshi.adapter(MessageModel::class.java).fromJson(toParse)
        Assert.assertTrue(message?.content is Content.Post)

        Assert.assertEquals((message?.content as Content.Post).text, "@cel would tabs be an easier way to do this? shift+click on a link to open a tab?")
        Assert.assertEquals(message.content.type, "post")
        Assert.assertEquals((message.content as Content.Post).root, Identifier.fromString("%yAvDwopppOmCXAU5xj5KOuLkuYp+CkUicmEJbgJVrbo=.sha256"))
        Assert.assertEquals((message.content as Content.Post).branch, Identifier.fromString("%LQQ53cFB816iAbayxwuLjVLmuCwt1J2erfMge4chSC4=.sha256"))

        Assert.assertEquals(message.sequence, 35)
        Assert.assertEquals(message.timestamp, Date(1449202158507))
        Assert.assertEquals(message.hash, "sha256")
        Assert.assertEquals(message.author, Identifier.fromString("@EMovhfIrFk4NihAKnRNhrfRaqIhBv1Wj8pTxJNgvCCY=.ed25519"))
        Assert.assertEquals(message.signature, "Go98D7qqvvtaGkGPPttBcHsIuTF+s3FmV5ChNWAhifpdlN9UkmkUd39GQaqDUgs9T0bkXgZByLsdZ31MH5tMBQ==.sig.ed25519")
    }

    @Test
    fun testSignatureGeneration() {
        val toParse = "{\n" +
                "  \"previous\": \"%PF8rYIG4jnZnxUxZhFrJLSMiB7HarO3v1Bn0EH8CvYU=.sha256\",\n" +
                "  \"sequence\": 5,\n" +
                "  \"author\": \"@zgThI3hlrpVWAISAdknwgz2l8KoT1v6V6v3bdZZ11jM=.ed25519\",\n" +
                "  \"timestamp\": 1553032880238,\n" +
                "  \"hash\": \"sha256\",\n" +
                "  \"content\": {\n" +
                "    \"type\": \"post\",\n" +
                "    \"text\": \"testing post\"\n" +
                "  },\n" +
                "  \"signature\": \"4RBCI5QEtJiiKy0JU1vFxJhEg8EVwhrbtCp1xTFivA6odNI7XsaP/A3prKnf1+ZbBQpDOjSiMtvLNZxc2VdxDw==.sig.ed25519\"\n" +
                "}"

        val adapter = moshi.adapter(MessageModel::class.java)
        val newMessage = adapter.fromJson(toParse)?.let { message ->
            val identityHandler = BasicIdentityHandler(
                Base64.decode("zgThI3hlrpVWAISAdknwgz2l8KoT1v6V6v3bdZZ11jM=", Base64.DEFAULT),
                Base64.decode("/P+IqmkRzG8N6OsvkhmkRGlzgg0yYWMnp6KqoNQ9qdjOBOEjeGWulVYAhIB2SfCDPaXwqhPW/pXq/dt1lnXWMw==", Base64.DEFAULT))
            MessageModel(
                message.previous, message.sequence, message.author, message.timestamp, message.hash, message.content,
                moshi, identityHandler)
        }

        Assert.assertEquals(newMessage?.signature, "4RBCI5QEtJiiKy0JU1vFxJhEg8EVwhrbtCp1xTFivA6odNI7XsaP/A3prKnf1+ZbBQpDOjSiMtvLNZxc2VdxDw==.sig.ed25519")
    }
}