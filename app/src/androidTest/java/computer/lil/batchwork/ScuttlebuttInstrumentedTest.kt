package computer.lil.batchwork

import android.os.AsyncTask
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4
import android.util.Log
import computer.lil.batchwork.handshake.SSBClientHandshake
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.net.Socket


@RunWith(AndroidJUnit4::class)
class ScuttlebuttInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("computer.lil.batchwork", appContext.packageName)
    }

    fun getHexString(array: ByteArray): String {
        val sb = StringBuilder()
        for (b in array) {
            sb.append(String.format("%02X", b))
        }
        return sb.toString()
    }

    fun sendToServer(text: ByteArray?) {
        val socket = Socket("10.0.2.2", 8888)
        val out = BufferedOutputStream(socket.outputStream)

        val buffer = ByteArray(1024)
        val inputStream = socket.inputStream

        val responseTask = object : AsyncTask<Void, Void, String>() {

            val byteArrayOutputStream = ByteArrayOutputStream(1024)
            var response = ""
            override fun doInBackground(vararg arg: Void): String {
                try {
                    var bytesRead = inputStream.read(buffer)
                    while (bytesRead != -1) {
                        byteArrayOutputStream.write(buffer, 0, bytesRead)
                        response += byteArrayOutputStream.toString("UTF-8")
                        Log.d("rolling response", response)
                        bytesRead = inputStream.read(buffer)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Log.d("response", response)
                return response
            }

            override fun onPostExecute(result: String) {
                Log.d("server response", result)
            }
        }

        out.write(text)
        responseTask.execute()
        out.flush()
        //out.close()
        //socket.close()
    }

    @Test
    fun connectScuttlebutt() {
        val clientHandshake = SSBClientHandshake()

        sendToServer(clientHandshake.createHello())
        Thread.sleep(3000)
        assert(true)
    }
}
