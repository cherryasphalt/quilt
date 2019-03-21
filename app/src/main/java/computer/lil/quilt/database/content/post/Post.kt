package computer.lil.quilt.database.content.post

import androidx.room.*
import computer.lil.quilt.model.Identifier

data class Post(
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "root") val root: Identifier?,
    @ColumnInfo(name = "branch") val branch: Identifier?,
    @ColumnInfo(name = "channel") val channel: String?
)