package computer.lil.quilt.database

import androidx.room.*
import computer.lil.quilt.database.content.Content
import computer.lil.quilt.database.content.post.Mention
import computer.lil.quilt.model.Identifier
import java.util.*

@Entity(tableName = "messages")
data class Message(
    @PrimaryKey @ColumnInfo(name = "id")  var id: Identifier,
    @ColumnInfo(name = "previous") var previous: Identifier?,
    @ColumnInfo(name = "author") var author: Identifier,
    @ColumnInfo(name = "sequence") var sequence: Int,
    @ColumnInfo(name = "timestamp") var timestamp: Date,
    @ColumnInfo(name = "hash") var hash: String,
    @Embedded val content: Content,
    @ColumnInfo(name = "signature") val signature: String?,
    @ColumnInfo(name = "received_timestamp") val receivedTimestamp: Date
)

data class MessageAndAllMentions(
    @Embedded
    val message: Message,
    @Relation(parentColumn = "id", entityColumn = "messageId", entity = Mention::class)
    val mentions: List<Mention>
)