package computer.lil.quilt.network

import computer.lil.quilt.identity.IdentityHandler
import computer.lil.quilt.protocol.Util
import okio.ByteString
import java.util.concurrent.Executor
import java.util.concurrent.SynchronousQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

class ConnectionPool(
    val identityHandler: IdentityHandler,
    val networkId: ByteString,
    val maxConnections: Int = 5,
    val keepAliveDuration: Long = 60L,
    val timeUnit: TimeUnit = TimeUnit.SECONDS
) {
    val keepAliveDurationNs = timeUnit.toNanos(keepAliveDuration)


    companion object {
        @JvmField
        val executor: Executor = ThreadPoolExecutor(0, Int.MAX_VALUE, 60L, TimeUnit.SECONDS, SynchronousQueue(), Util.threadFactory("SSB Connection Pool", true))


    }

    val pool: MutableList<PeerConnection> = ArrayList()

    private fun add(connection: PeerConnection) {
        pool.add(connection)
    }

    fun connect(remoteKey: ByteString, host: String, port: Int) {
        val connection = PeerConnection(
            identityHandler,
            networkId,
            remoteKey
        )
        add(connection)
        connection.connectToPeer(host, port)
    }

    fun closeAll() {
        for (connection in pool) {
            connection.close()
        }
    }
}