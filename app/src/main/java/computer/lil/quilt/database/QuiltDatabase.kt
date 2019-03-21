package computer.lil.quilt.database

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import android.os.AsyncTask
import androidx.room.Room
import androidx.room.TypeConverters
import computer.lil.quilt.database.content.post.Mention
import computer.lil.quilt.database.content.post.MentionDao
import computer.lil.quilt.model.Identifier
import computer.lil.quilt.util.SingletonHolder

@Database(entities = [Message::class, Feed::class, Blob::class, Mention::class], version = 1)
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

    private class PopulateDbAsync internal constructor(context: Context) : AsyncTask<Void, Void, Void>() {
        private val mDao: MessageDao
        //private val helloMessage: Message

        init {
            mDao = Companion.getInstance(context).messageDao()
        }

        override fun doInBackground(vararg params: Void): Void? {
            //mDao.deleteAll()
            //mDao.insertAll(helloMessage, helloMessage)
            return null
        }
    }
}