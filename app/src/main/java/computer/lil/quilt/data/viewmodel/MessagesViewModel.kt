package computer.lil.quilt.data.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import computer.lil.quilt.data.repo.MessageRepository
import computer.lil.quilt.database.Message

class MessagesViewModel(application: Application): AndroidViewModel(application) {
    private var repository: MessageRepository = MessageRepository(application)
    private var allMessages: LiveData<List<Message>> = repository.getAllMessages()

    fun refresh() {
        allMessages = repository.getAllMessages()
    }

    fun getAllMessages(): LiveData<List<Message>> = allMessages

    fun submitNewPostMessage(text: String) {
        repository.submitNewPostMessage(text)
    }
}