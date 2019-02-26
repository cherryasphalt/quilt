package computer.lil.batchwork.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Peer(
    @PrimaryKey var id: String
)