package computer.lil.quilt.database

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import android.os.AsyncTask
import androidx.room.Room
import androidx.room.TypeConverters
import computer.lil.quilt.model.ProtocolModel
import computer.lil.quilt.util.SingletonHolder

@Database(entities = [Message::class, Feed::class, Blob::class], version = 1)
@TypeConverters(DBTypeConverter::class)
abstract class SSBDatabase: RoomDatabase() {
    companion object: SingletonHolder<SSBDatabase, Context>({
        Room.databaseBuilder(
            it,
            SSBDatabase::class.java,
            "ssb_database"
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

    private class PopulateDbAsync internal constructor(context: Context) : AsyncTask<Void, Void, Void>() {
        private val mDao: MessageDao
        private val helloMessage: Message

        init {
            mDao = Companion.getInstance(context).messageDao()
            helloMessage = ProtocolModel.createMessage(
                context,
                mapOf("type" to "post", "text" to "hello")
            )
        }

        override fun doInBackground(vararg params: Void): Void? {
            //mDao.deleteAll()
            mDao.insertAll(helloMessage, helloMessage)
            return null
        }
    }
}