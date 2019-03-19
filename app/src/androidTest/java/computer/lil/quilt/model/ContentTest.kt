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
                "  \"previous\": null,\n" +
                "  \"author\": \"@FCX/tsDLpubCPKKfIrw4gc+SQkHcaD17s7GI6i/ziWY=.ed25519\",\n" +
                "  \"sequence\": 1,\n" +
                "  \"timestamp\": 1514517067954,\n" +
                "  \"hash\": \"sha256\",\n" +
                "  \"content\": {\n" +
                "    \"type\": \"post\",\n" +
                "    \"text\": \"This is the first post!\"\n" +
                "  },\n" +
                "  \"signature\": \"QYOR/zU9dxE1aKBaxc3C0DJ4gRyZtlMfPLt+CGJcY73sv5abKKKxr1SqhOvnm8TY784VHE8kZHCD8RdzFl1tBA==.sig.ed25519\"\n" +
                "}"


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
        Assert.assertEquals((message?.content as Content.Post).text, "This is the first post!")
        Assert.assertEquals(message.sequence, 1)
        Assert.assertEquals(message.timestamp, 1514517067954)
        Assert.assertEquals(message.hash, "sha256")
        Assert.assertEquals(message.author, Identifier.fromString("@FCX/tsDLpubCPKKfIrw4gc+SQkHcaD17s7GI6i/ziWY=.ed25519"))
        Assert.assertEquals(message.signature, "QYOR/zU9dxE1aKBaxc3C0DJ4gRyZtlMfPLt+CGJcY73sv5abKKKxr1SqhOvnm8TY784VHE8kZHCD8RdzFl1tBA==.sig.ed25519")
    }
}