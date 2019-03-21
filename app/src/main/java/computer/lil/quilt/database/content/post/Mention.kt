package computer.lil.quilt.database.content.post

import androidx.room.ColumnInfo
import androidx.room.Entity
import computer.lil.quilt.model.Identifier

@Entity(primaryKeys = ["link", "name"])
data class Mention(
    @ColumnInfo(name = "link") val link: Identifier,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "message_id") val messageId: Identifier
)