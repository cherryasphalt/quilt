package computer.lil.quilt.database.content

import androidx.room.ColumnInfo
import androidx.room.Embedded
import computer.lil.quilt.database.content.post.Post

data class Content(
    @ColumnInfo(name = "type") var type: String,
    @Embedded(prefix = "post_") val post: Post?
)
