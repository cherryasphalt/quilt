package computer.lil.quilt.database

import computer.lil.quilt.database.content.Content
import computer.lil.quilt.database.content.post.Mention
import computer.lil.quilt.database.content.post.MentionDao
import computer.lil.quilt.database.content.post.Post
import computer.lil.quilt.model.MessageModel
import java.util.*

class MessageConverter() {
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