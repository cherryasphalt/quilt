package computer.lil.quilt.api

import computer.lil.quilt.database.Peer
import computer.lil.quilt.model.ExtendedMessage
import computer.lil.quilt.model.Identifier

class InMemoryStorage() : SSBStorage {
    val map: MutableMap<Identifier, ExtendedMessage> = mutableMapOf()

    override fun getPeers(): List<Peer> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun saveMessage(message: ExtendedMessage): Boolean {
        val author = message.value.author
        if (map.containsKey(author)) {

        } else {
            map.put(author, message)
        }
        return true
    }

    override fun getMostRecentSequence(identifier: Identifier): Int {
        map.get(identifier)?.let {
            return it.value.sequence
        }
        return -1
    }

}