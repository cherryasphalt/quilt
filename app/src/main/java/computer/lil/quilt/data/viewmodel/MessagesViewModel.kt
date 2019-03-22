package computer.lil.quilt.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import computer.lil.quilt.data.repo.MessagesRepository
import computer.lil.quilt.database.Message

class MessagesViewModel(application: Application): AndroidViewModel(application) {
    private var repository: MessagesRepository = MessagesRepository(application)
    private var allMessages: LiveData<List<Message>> = repository.getAllMessages()

    fun refresh() {

    }

    fun getAllMessages(): LiveData<List<Message>> = allMessages
}