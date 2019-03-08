package computer.lil.quilt.database

import android.app.Application
import android.os.AsyncTask
import androidx.lifecycle.LiveData
import io.reactivex.Observable

class MessageRepository(application: Application) {
    val db = SSBDatabase.getInstance(application.applicationContext)
    private val messageDao: MessageDao = db.messageDao()
    private val allMessages: LiveData<List<Message>>? = messageDao.getAll()

    fun getRecentMessagesFromDb(limit: Int = 100): Observable<Message> {
        return Observable.fromArray(*messageDao.getAll().value!!.toTypedArray())
    }
}