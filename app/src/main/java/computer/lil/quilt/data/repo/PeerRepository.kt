package computer.lil.quilt.data.repo

import android.content.Context
import androidx.lifecycle.LiveData
import computer.lil.quilt.database.Message
import computer.lil.quilt.database.Peer
import computer.lil.quilt.database.PeerDao
import computer.lil.quilt.database.QuiltDatabase
import computer.lil.quilt.injection.DaggerDataComponent
import computer.lil.quilt.injection.DataModule
import javax.inject.Inject

class PeerRepository(context: Context) {
    @Inject lateinit var db: QuiltDatabase
    private val peerDao: PeerDao

    init {
        DaggerDataComponent.builder().dataModule(DataModule(context)).build().inject(this)
        peerDao = db.peerDao()
    }

    fun getAllPeers(): LiveData<List<Peer>> {
        return peerDao.getAll()
    }

    fun getPeersFollowing(): LiveData<List<Peer>> {
        return peerDao.getFollowing()
    }

}