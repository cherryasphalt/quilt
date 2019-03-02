package computer.lil.batchwork.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
@Entity
data class Message(
    @PrimaryKey @ColumnInfo(name = "id")  var id: String,
    @ColumnInfo(name = "previous") var previous: String,
    @ColumnInfo(name = "author") var author: String,
    @ColumnInfo(name = "sequence") var sequence: Int,
    @ColumnInfo(name = "timestamp") var timestamp: Long,
    @ColumnInfo(name = "hash") var hash: String,
    //@ColumnInfo(name = "content") val content: Map<String, Any>,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "signature") val signature: String
)