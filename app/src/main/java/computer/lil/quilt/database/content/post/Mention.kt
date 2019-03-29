package computer.lil.quilt.database.content.post

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import computer.lil.quilt.model.Identifier

@Entity
data class Mention(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "link") val link: String,
    @ColumnInfo(name = "name") val name: String?,
    @ColumnInfo(name = "message_id") val messageId: Identifier
)