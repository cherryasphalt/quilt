package computer.lil.quilt.api

import computer.lil.quilt.database.Peer
import computer.lil.quilt.model.ExtendedMessage
import computer.lil.quilt.model.Identifier

interface SSBStorage {
    fun getPeers(): List<Peer>
    fun saveMessage(message: ExtendedMessage): Boolean
    fun getMostRecentSequence(identifier: Identifier): Int
}