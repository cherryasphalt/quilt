package computer.lil.quilt.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import computer.lil.quilt.model.Identifier

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages")
    fun getAll(): LiveData<List<Message>>

    @Query("SELECT * FROM messages WHERE id IN (:messageIds)")
    fun loadAllByIds(messageIds: IntArray): LiveData<List<Message>>

    @Query("SELECT * FROM messages WHERE author = :author ORDER BY sequence DESC LIMIT 1")
    fun getRecentMessageFromAuthor(author: String): LiveData<Message>

    @Query("SELECT * FROM messages WHERE id = :id")
    fun findMessageById(id: Identifier): Message

    @Query("SELECT * FROM messages WHERE id = :id")
    fun findMessageById(id: String): LiveData<Message>

    @Insert
    fun insertAll(vararg messages: Message)

    @Insert
    fun insert(message: Message)

    @Query("DELETE FROM messages")
    fun deleteAll()

    @Delete
    fun delete(message: Message)
}