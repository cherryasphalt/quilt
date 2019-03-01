package computer.lil.batchwork.database

import android.app.Application
import android.os.AsyncTask
import androidx.lifecycle.LiveData

class MessageRepository(application: Application) {
    val db = SSBDatabase.getInstance(application.applicationContext)
    private var messageDao: MessageDao = db.messageDao()
    private var allMessages: LiveData<List<Message>>? = messageDao.getAll()

    fun insert(message: Message) {
        insertAsyncTask(messageDao).execute(message)
    }

    private class insertAsyncTask internal constructor(private val mAsyncTaskDao: MessageDao) :
        AsyncTask<Message, Void, Void>() {

        override fun doInBackground(vararg params: Message): Void? {
            mAsyncTaskDao.insert(params[0])
            return null
        }
    }
}