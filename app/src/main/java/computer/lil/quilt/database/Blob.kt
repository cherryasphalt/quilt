package computer.lil.quilt.database

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import computer.lil.quilt.model.Identifier

@Entity
data class Blob(
    @PrimaryKey @ColumnInfo(name = "id") val id: Identifier,
    @ColumnInfo(name = "distance") val distance: Int,
    @ColumnInfo(name = "size") val size: Long,
    @ColumnInfo(name = "location") val location: Uri
)