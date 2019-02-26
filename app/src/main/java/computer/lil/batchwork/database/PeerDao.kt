package computer.lil.batchwork.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PeerDao {
    @Query("SELECT * FROM peer")
    fun getAll(): List<Message>

    @Query("SELECT * FROM peer WHERE id IN (:peerIds)")
    fun loadAllByIds(peerIds: Array<String>): List<Peer>

    @Insert
    fun insertAll(vararg peer: Peer)

    @Delete
    fun delete(peer: Peer)
}