package computer.lil.quilt.database.content.post

import androidx.room.Dao
import androidx.room.Insert

@Dao
interface MentionDao {
    @Insert
    fun insertAll(vararg mentions: Mention)

    @Insert
    fun insert(mention: Mention)
}