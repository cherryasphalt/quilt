package computer.lil.quilt

import androidx.test.InstrumentationRegistry
import androidx.test.runner.AndroidJUnit4
import computer.lil.quilt.protocol.RPCProtocol
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
