package computer.lil.batchwork

import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import computer.lil.batchwork.protocol.RPCProtocol
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class ScuttlebuttInstrumentedTest {
    @Test
    fun useAppContext() {
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("computer.lil.batchwork", appContext.packageName)
    }

    @Test
    fun connectScuttlebutt() {
        val client = RPCProtocol()

    }
}
