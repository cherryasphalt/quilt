package computer.lil.quilt.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import computer.lil.quilt.model.Identifier

@Entity
data class Feed(
    @PrimaryKey @ColumnInfo(name = "id") val id: Identifier,
    @ColumnInfo(name = "sequence") val sequence: Int
)