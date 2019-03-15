package computer.lil.quilt.model

import androidx.test.runner.AndroidJUnit4
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class InviteCodeTest {
    @Test
    fun testParsing() {
        val from = "zie.one:8008:@tgzHDm9HEN0k5wFRLFmNPyGZYNF/M5KpkZqCRhgowVE=.ed25519~/lJfSYpAfISN3iBxoL71Q7gL+rTaYXfcXJ4cOEb2E2E="
        val inviteCode = InviteCode.fromString(from)
        Assert.assertNotNull(inviteCode)
        inviteCode?.run {
            Assert.assertEquals(domain, "zie.one")
            Assert.assertEquals(port, 8008)
            Assert.assertEquals(pubKey.algorithm, "ed25519")
            Assert.assertEquals(pubKey.keyHash, "tgzHDm9HEN0k5wFRLFmNPyGZYNF/M5KpkZqCRhgowVE=")
            Assert.assertEquals(pubKey.type, Identifier.IdentityType.IDENTITY)
            Assert.assertEquals(inviteKey, "/lJfSYpAfISN3iBxoL71Q7gL+rTaYXfcXJ4cOEb2E2E=")
            Assert.assertEquals(getInviteCode(), "zie.one:8008:@tgzHDm9HEN0k5wFRLFmNPyGZYNF/M5KpkZqCRhgowVE=.ed25519~/lJfSYpAfISN3iBxoL71Q7gL+rTaYXfcXJ4cOEb2E2E=")
        }
    }
}