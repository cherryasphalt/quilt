package computer.lil.quilt.database

import android.app.Application
import androidx.lifecycle.LiveData
import io.reactivex.Observable

class MessageRepository(application: Application) {
    val db = QuiltDatabase.getInstance(application.applicationContext)
    private val messageDao: MessageDao = db.messageDao()
    private val allMessages: LiveData<List<Message>>? = messageDao.getAll()

    fun getRecentMessagesFromDb(limit: Int = 100): Observable<Message> {
        return Observable.fromArray(*messageDao.getAll().value!!.toTypedArray())
    }
}