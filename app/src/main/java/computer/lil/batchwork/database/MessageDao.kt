package computer.lil.batchwork.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MessageDao {
    @Query("SELECT * FROM message")
    fun getAll(): List<Message>

    @Query("SELECT * FROM message WHERE id IN (:messageIds)")
    fun loadAllByIds(messageIds: IntArray): List<Message>

    @Insert
    fun insertAll(vararg messages: Message)

    @Delete
    fun delete(message: Message)
}