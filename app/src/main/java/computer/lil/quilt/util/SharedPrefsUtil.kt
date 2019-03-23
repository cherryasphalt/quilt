package computer.lil.quilt.util

import android.content.Context
import computer.lil.quilt.identity.AndroidKeyStoreIdentityHandler
import computer.lil.quilt.identity.IdentityHandler
import computer.lil.quilt.model.Identifier

class SharedPrefsUtil {
    companion object {
        fun getUserIdentifier(context: Context): Identifier {
            val pref = context.getSharedPreferences(AndroidKeyStoreIdentityHandler.SHARED_PREF_NAME, Context.MODE_PRIVATE)
            pref.getString(AndroidKeyStoreIdentityHandler.PREF_IDENTITY_PUBLIC_KEY, null)?.let { publicKeyString ->
                return Identifier(publicKeyString, Identifier.AlgoType.ED25519, Identifier.IdentityType.IDENTITY)
            }
            throw IdentityHandler.IdentityException("Identity not found.")
        }
    }
}