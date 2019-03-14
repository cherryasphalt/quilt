package computer.lil.quilt.database

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json

@Entity
data class Blob(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "distance") val distance: Int,
    @ColumnInfo(name = "size") val size: Long,
    @ColumnInfo(name = "location") val location: Uri
)