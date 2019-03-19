package computer.lil.quilt.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass
import computer.lil.quilt.model.Identifier

@JsonClass(generateAdapter = true)
@Entity
data class Message(
    @PrimaryKey @ColumnInfo(name = "id")  var id: Identifier,
    @ColumnInfo(name = "previous") var previous: Identifier,
    @ColumnInfo(name = "author") var author: Identifier,
    @ColumnInfo(name = "sequence") var sequence: Int,
    @ColumnInfo(name = "timestamp") var timestamp: Long,
    @ColumnInfo(name = "hash") var hash: String,
    //@ColumnInfo(name = "content") val content: Content,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "signature") val signature: String
)