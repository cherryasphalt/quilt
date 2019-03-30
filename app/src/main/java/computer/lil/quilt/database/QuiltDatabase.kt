package computer.lil.quilt.database

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import android.os.AsyncTask
import androidx.room.Room
import androidx.room.TypeConverters
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.PolymorphicJsonAdapterFactory
import computer.lil.quilt.data.repo.MessageRepository
import computer.lil.quilt.database.content.post.Mention
import computer.lil.quilt.database.content.post.MentionDao
import computer.lil.quilt.model.Adapters
import computer.lil.quilt.model.Content
import computer.lil.quilt.model.Identifier
import computer.lil.quilt.model.MessageModel
import computer.lil.quilt.protocol.Constants
import computer.lil.quilt.util.SingletonHolder

@Database(entities = [Message::class, Peer::class, Pub::class, Blob::class, Mention::class], version = 1)
@TypeConverters(DBTypeConverter::class, Identifier::class)
abstract class QuiltDatabase: RoomDatabase() {
    companion object: SingletonHolder<QuiltDatabase, Context>({
        Room.databaseBuilder(
            it,
            QuiltDatabase::class.java,
            "quilt_database"
        )
        .addCallback(object : RoomDatabase.Callback() {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                PopulateDbAsync(it).execute()
            }
        })
        .build()
    })

    abstract fun messageDao(): MessageDao
    abstract fun mentionDao(): MentionDao
    abstract fun peerDao(): PeerDao

    private class PopulateDbAsync internal constructor(context: Context) : AsyncTask<Void, Void, Void>() {
        private val messageDao: MessageDao = Companion.getInstance(context).messageDao()
        private val mentionDao: MentionDao = Companion.getInstance(context).mentionDao()
        private val peerDao: PeerDao = Companion.getInstance(context).peerDao()

        override fun doInBackground(vararg params: Void): Void? {
            messageDao.deleteAll()
            insertMessages()
            peerDao.deleteAll()
            insertPeers()
            return null
        }

        fun insertPeers() {
            val peerList = listOf(
                "@Z2rNu9oinC9LzYPcaaOjEELE7pImbQnKu2mbCzBmsN4=.ed25519",
                "@EMovhfIrFk4NihAKnRNhrfRaqIhBv1Wj8pTxJNgvCCY=.ed25519")
                .map {
                    Identifier.fromString(it)
                }
                .map {
                    Peer(it!!, 0, null, true, false)
                }
            peerDao.insertPeers(*peerList.toTypedArray())
        }

        fun insertMessages() {
            val moshi = Constants.getMoshiInstance()

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
            moshi.adapter(MessageModel::class.java).fromJson(toParse)?.let {
                    MessageRepository.insertNetworkMessage(it, messageDao, mentionDao)
            }
        }
    }
}