package computer.lil.batchwork.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.CASCADE
import androidx.room.PrimaryKey

@Entity(foreignKeys = arrayOf(ForeignKey(
    entity = Peer::class,
    parentColumns = arrayOf("id"),
    childColumns = arrayOf("author"),
    onDelete = CASCADE)
))
data class Message(
    @PrimaryKey var id: Int,
    @ColumnInfo(name = "previous") var previous: String?,
    @ColumnInfo(name = "author") var author: String?,
    @ColumnInfo(name = "sequence") var sequence: Int?,
    @ColumnInfo(name = "timestamp") var timestamp: Long?,
    @ColumnInfo(name = "hash") var hash: String?

)