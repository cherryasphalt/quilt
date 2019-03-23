package computer.lil.quilt.data.repo

import android.content.Context
import android.os.AsyncTask
import androidx.lifecycle.LiveData
import com.squareup.moshi.Moshi
import computer.lil.quilt.database.Message
import computer.lil.quilt.database.MessageDao
import computer.lil.quilt.database.QuiltDatabase
import computer.lil.quilt.database.content.Content
import computer.lil.quilt.database.content.post.Mention
import computer.lil.quilt.database.content.post.MentionDao
import computer.lil.quilt.database.content.post.Post
import computer.lil.quilt.identity.IdentityHandler
import computer.lil.quilt.injection.DaggerDataComponent
import computer.lil.quilt.injection.DataModule
import computer.lil.quilt.model.MessageModel
import io.reactivex.Observable
import java.util.*
import javax.inject.Inject

class MessageRepository(context: Context) {
    @Inject lateinit var db: QuiltDatabase
    @Inject lateinit var identityHandler: IdentityHandler
    @Inject lateinit var moshi: Moshi

    private val messageDao: MessageDao
    private val mentionDao: MentionDao
    private val allMessages: LiveData<List<Message>>

    init {
        DaggerDataComponent.builder().dataModule(DataModule(context)).build().inject(this)
        messageDao = db.messageDao()
        mentionDao = db.mentionDao()
        allMessages = messageDao.getAll()
    }

    fun getAllMessages(): LiveData<List<Message>> {
        return messageDao.getAll()
    }

    fun getRecentMessagesFromDb(limit: Int = 100): Observable<Message> {
        return Observable.fromArray(*messageDao.getAll().value!!.toTypedArray())
    }

    fun submitNewPostMessage(text: String) {
        class InsertPostMessageTask : AsyncTask<Void, Void, Void>() {
            override fun doInBackground(vararg params: Void): Void? {
                val identity = identityHandler.getIdentifier()
                val previousMessage = messageDao.getRecentMessageFromAuthor(identity.toString())
                val previousMessageId = previousMessage?.id
                val newSequence = (previousMessage?.sequence ?: 0) + 1

                val post = computer.lil.quilt.model.Content.Post(text = text)

                val timestamp = Date(System.currentTimeMillis())
                val newMessage = MessageModel(previousMessageId, newSequence, identity, timestamp, content = post, moshi = moshi, identityHandler = identityHandler)
                insertNetworkMessage(newMessage, messageDao, mentionDao)
                return null
            }
        }
        InsertPostMessageTask().execute()
    }

    companion object {
        fun insertNetworkMessage(networkMessage: MessageModel, messageDao: MessageDao, mentionDao: MentionDao) {
            val messageId = networkMessage.createMessageId()

            val content: Content =
                when(networkMessage.content.type) {
                    "post" -> {
                        val postContent = networkMessage.content as computer.lil.quilt.model.Content.Post

                        postContent.mentions?.map {
                            Mention(it.link, it.name, messageId)
                        }?.let {
                            mentionDao.insertAll(*it.toTypedArray())
                        }
                        Content(networkMessage.content.type, Post(postContent.text, postContent.root, postContent.branch, postContent.channel))
                    }
                    else -> Content("private_message", null)
                }

            val message = Message(
                messageId,
                networkMessage.previous,
                networkMessage.author,
                networkMessage.sequence,
                networkMessage.timestamp,
                networkMessage.hash,
                content,
                networkMessage.signature,
                Date(System.currentTimeMillis())
            )
            messageDao.insert(message)
        }
    }
}