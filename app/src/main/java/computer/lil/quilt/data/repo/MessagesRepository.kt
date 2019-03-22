package computer.lil.quilt.data.repo

import android.app.Application
import androidx.lifecycle.LiveData
import computer.lil.quilt.database.Message
import computer.lil.quilt.database.QuiltDatabase
import computer.lil.quilt.injection.DaggerDataComponent
import computer.lil.quilt.injection.DataModule
import javax.inject.Inject

class MessagesRepository(application: Application) {
    @Inject lateinit var db: QuiltDatabase
    private var messages: LiveData<List<Message>>

    init {
        DaggerDataComponent.builder().dataModule(DataModule(application)).build().inject(this)
        messages = db.messageDao().getAll()
    }

    fun getAllMessages(): LiveData<List<Message>> {
        return messages
    }
}