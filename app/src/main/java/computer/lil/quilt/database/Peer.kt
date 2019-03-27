package computer.lil.quilt.database

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import computer.lil.quilt.model.Identifier

@Entity(tableName = "peers")
data class Peer(
    @PrimaryKey @ColumnInfo(name = "id") val id: Identifier,
    @ColumnInfo(name = "sequence") val sequence: Int,
    @ColumnInfo(name = "avatar") val avatar: Uri?,
    @ColumnInfo(name = "following") val following: Boolean,
    @ColumnInfo(name = "follower") val follower: Boolean
)