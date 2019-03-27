package computer.lil.quilt.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface PeerDao {
    @Query("SELECT * FROM peers")
    fun getAll(): LiveData<List<Peer>>

    @Query("SELECT * FROM peers WHERE following = 1")
    fun getFollowing(): LiveData<List<Peer>>

    @Insert
    fun insertPeers(vararg peers: Peer)

    @Insert
    fun insertPeer(peer: Peer)

    @Query("DELETE FROM peers")
    fun deleteAll()
}