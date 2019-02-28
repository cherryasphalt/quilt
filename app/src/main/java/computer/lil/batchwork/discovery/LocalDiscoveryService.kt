package computer.lil.batchwork.discovery

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import kotlinx.coroutines.*
import java.io.Closeable
import java.net.DatagramPacket
import java.net.DatagramSocket
import kotlin.coroutines.resume

class LocalDiscoveryService: Service() {
    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private val socket = DatagramSocket(8008)
    private var dataJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        val datagramPacket = DatagramPacket(ByteArray(1024), 1024)
        dataJob = CoroutineScope(Dispatchers.IO).launch {
            runBlocking(Dispatchers.IO) {
                socket.useCancellably { it.receive(datagramPacket) }
            }
            withContext(Dispatchers.Main) {
                Log.d("socket-receive", datagramPacket.toString())
            }
        }
    }

    override fun onDestroy() {
        socket.close()
        super.onDestroy()
    }

    suspend inline fun <T : Closeable?, R> T.useCancellably(
        crossinline block: (T) -> R
    ): R = suspendCancellableCoroutine { cont ->
        cont.invokeOnCancellation { this?.close() }
        cont.resume(use(block))
    }
}