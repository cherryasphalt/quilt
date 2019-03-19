package computer.lil.quilt.model

import androidx.test.runner.AndroidJUnit4
import com.squareup.moshi.Moshi
import org.junit.Test
import org.junit.runner.RunWith
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import org.junit.Assert

@RunWith(AndroidJUnit4::class)
class ContentTest {
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

        val moshi = Moshi.Builder()
            .add(Identifier.IdentifierJsonAdapter())
            .add(
                PolymorphicJsonAdapterFactory.of(Content::class.java, "type")
                    .withSubtype(Content.Post::class.java, "post")
                    .withSubtype(Content.Pub::class.java, "pub")
                    .withSubtype(Content.Contact::class.java, "contact")
            ).build()
        val message = moshi.adapter(MessageModel::class.java).fromJson(toParse)
        Assert.assertTrue(message?.content is Content.Post)

        Assert.assertEquals((message?.content as Content.Post).text, "@cel would tabs be an easier way to do this? shift+click on a link to open a tab?")
        Assert.assertEquals((message.content as Content.Post).root, Identifier.fromString("%yAvDwopppOmCXAU5xj5KOuLkuYp+CkUicmEJbgJVrbo=.sha256"))
        Assert.assertEquals((message.content as Content.Post).branch, Identifier.fromString("%LQQ53cFB816iAbayxwuLjVLmuCwt1J2erfMge4chSC4=.sha256"))

        Assert.assertEquals(message.sequence, 35)
        Assert.assertEquals(message.timestamp, 1449202158507)
        Assert.assertEquals(message.hash, "sha256")
        Assert.assertEquals(message.author, Identifier.fromString("@EMovhfIrFk4NihAKnRNhrfRaqIhBv1Wj8pTxJNgvCCY=.ed25519"))
        Assert.assertEquals(message.signature, "Go98D7qqvvtaGkGPPttBcHsIuTF+s3FmV5ChNWAhifpdlN9UkmkUd39GQaqDUgs9T0bkXgZByLsdZ31MH5tMBQ==.sig.ed25519")
    }
}