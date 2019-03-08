package computer.lil.quilt.database

import android.net.Uri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json

@Entity
data class Blob(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "hashAlgo") val hashAlgo: HashAlgo = HashAlgo.SHA256,
    @ColumnInfo(name = "distance") val distance: Int,
    @ColumnInfo(name = "size") val size: Long,
    @ColumnInfo(name = "location") val location: Uri
)

enum class HashAlgo(val value: String) {
    @Json(name="sha256") SHA256("sha256")
}