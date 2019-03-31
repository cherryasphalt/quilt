package computer.lil.quilt.database

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.runner.AndroidJUnit4
import computer.lil.quilt.data.repo.MessageRepository
import computer.lil.quilt.database.content.post.MentionDao
import computer.lil.quilt.model.MessageModel
import computer.lil.quilt.api.Constants
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class QuiltDatabaseTests {
    private lateinit var messageDao: MessageDao
    private lateinit var mentionDao: MentionDao
    private lateinit var db: QuiltDatabase

    val moshi = Constants.getMoshiInstance()

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, QuiltDatabase::class.java).build()
        messageDao = db.messageDao()
        mentionDao = db.mentionDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        db.close()
    }

    @Test
    @Throws(Exception::class)
    fun testMessageInsert() {
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
        val messageId = message!!.createMessageId()
        MessageRepository.insertNetworkMessage(message, messageDao, mentionDao)
        val byId = messageDao.findMessageById(messageId)
        assertEquals(byId.id, messageId)
        assertEquals(byId.author, message.author)
        assertEquals(byId.previous, message.previous)
        assertEquals(byId.content.type, message.content.type)
    }
}