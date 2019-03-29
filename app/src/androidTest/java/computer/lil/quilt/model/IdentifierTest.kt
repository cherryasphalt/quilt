package computer.lil.quilt.model

import androidx.test.runner.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class IdentifierTest {
    @Test
    fun testParsing() {
        val from = "@EMovhfIrFk4NihAKnRNhrfRaqIhBv1Wj8pTxJNgvCCY=.ed25519"
        val identifier = Identifier.fromString(from)
        Assert.assertNotNull(identifier)
        identifier?.run {
            Assert.assertEquals(algorithm.algo, "ed25519")
            Assert.assertEquals(keyHash, "EMovhfIrFk4NihAKnRNhrfRaqIhBv1Wj8pTxJNgvCCY=")
            Assert.assertEquals(type, Identifier.IdentityType.IDENTITY)
        }

        val from2 = "@././;dfasfdas.ed25519"
        val identifier2 = Identifier.fromString(from2)
        Assert.assertNull(identifier2)
    }
}