package computer.lil.batchwork.database

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import android.os.AsyncTask
import androidx.room.Room
import computer.lil.batchwork.model.SSBClient
import computer.lil.batchwork.util.SingletonHolder

@Database(entities = [Message::class], version = 1)
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
                PopulateDbAsync(Companion.getInstance(it)).execute()
            }
        })
        .build()
    })

    abstract fun messageDao(): MessageDao

    private class PopulateDbAsync internal constructor(db: SSBDatabase) : AsyncTask<Void, Void, Void>() {
        private val mDao: MessageDao
        init {
            mDao = db.messageDao()
        }

        override fun doInBackground(vararg params: Void): Void? {
            //mDao.deleteAll()
            /*val messageHello = SSBClient.createMessage(
                mapOf()
            )
            val messageWorld = Message("World")
            mDao.insertAll(messageHello, messageWorld)*/
            return null
        }
    }
}