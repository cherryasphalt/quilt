package computer.lil.quilt.model

import android.util.Base64
import androidx.test.runner.AndroidJUnit4
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import computer.lil.quilt.identity.BasicIdentityHandler
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

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
    fun testParsing() {
        val toParse = "{\n" +
                "  \"previous\": \"%LFvIU/dFLvskkukxeBQ3Y7NSo34aPBESNe+IXczWvSI=.sha256\",\n" +
                "  \"author\": \"@EMovhfIrFk4NihAKnRNhrfRaqIhBv1Wj8pTxJNgvCCY=.ed25519\",\n" +
                "  \"sequence\": 3,\n" +
                "  \"timestamp\": 1449201683943,\n" +
                "  \"hash\": \"sha256\",\n" +
                "  \"content\": {\n" +
                "    \"type\": \"pub\",\n" +
                "    \"address\": {\n" +
                "      \"host\": \"188.166.252.233\",\n" +
                "      \"port\": 8008,\n" +
                "      \"key\": \"@uRECWB4KIeKoNMis2UYWyB2aQPvWmS3OePQvBj2zClg=.ed25519\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"signature\": \"S+tNRWv5D57ElrF1X6ZsPH9x4Ga5yiyznVpRukhN1sUk1VM353nohQMe+Axe4BHuUw+W9aP6A5W29EdXqOGfCg==.sig.ed25519\"\n" +
                "}"

        val adapter = moshi.adapter(MessageModel::class.java)
        val message = adapter.fromJson(toParse)

        val identityHandler = BasicIdentityHandler.createWithGeneratedKeys()
        message?.generateSignature(moshi, identityHandler)
        val newJson = adapter.toJson(message)
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
        val message = adapter.fromJson(toParse)

        val identityHandler = BasicIdentityHandler(
            Base64.decode("zgThI3hlrpVWAISAdknwgz2l8KoT1v6V6v3bdZZ11jM=", Base64.DEFAULT),
            Base64.decode("/P+IqmkRzG8N6OsvkhmkRGlzgg0yYWMnp6KqoNQ9qdjOBOEjeGWulVYAhIB2SfCDPaXwqhPW/pXq/dt1lnXWMw==", Base64.DEFAULT))
        message?.generateSignature(moshi, identityHandler)
        Assert.assertEquals(message?.signature, "4RBCI5QEtJiiKy0JU1vFxJhEg8EVwhrbtCp1xTFivA6odNI7XsaP/A3prKnf1+ZbBQpDOjSiMtvLNZxc2VdxDw==.sig.ed25519")
    }
}