package computer.lil.batchwork.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MessageDao {
    @Query("SELECT * FROM message")
    fun getAll(): LiveData<List<Message>>

    @Query("SELECT * FROM message WHERE id IN (:messageIds)")
    fun loadAllByIds(messageIds: IntArray): LiveData<List<Message>>

    @Query("SELECT * FROM message WHERE author = :author ORDER BY sequence DESC LIMIT 1")
    fun getRecentMessageFromAuthor(author: String): LiveData<Message>

    @Insert
    fun insertAll(vararg messages: Message)

    @Insert
    fun insert(message: Message)

    @Query("DELETE FROM message")
    fun deleteAll()

    @Delete
    fun delete(message: Message)
}