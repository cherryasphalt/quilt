package computer.lil.quilt.database.content.post

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy

@Dao
interface MentionDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insertAll(vararg mentions: Mention)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun insert(mention: Mention)
}