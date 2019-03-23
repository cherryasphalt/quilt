package computer.lil.quilt.util

import android.content.Context
import computer.lil.quilt.identity.AndroidKeyStoreIdentityHandler
import computer.lil.quilt.identity.IdentityHandler
import computer.lil.quilt.model.Identifier

class SharedPrefUtil {
    companion object {
        const val PREF_USER_NAME = "PREF_USER_NAME"

        @JvmStatic
        fun getUserIdentifier(context: Context): Identifier {
            val pref = context.getSharedPreferences(AndroidKeyStoreIdentityHandler.SHARED_PREF_NAME, Context.MODE_PRIVATE)
            pref.getString(AndroidKeyStoreIdentityHandler.PREF_IDENTITY_PUBLIC_KEY, null)?.let { publicKeyString ->
                return Identifier(publicKeyString, Identifier.AlgoType.ED25519, Identifier.IdentityType.IDENTITY)
            }
            throw IdentityHandler.IdentityException("Identity not found.")
        }

        @JvmStatic
        fun getUserName(context: Context): String? {
            val pref = context.getSharedPreferences(AndroidKeyStoreIdentityHandler.SHARED_PREF_NAME, Context.MODE_PRIVATE)
            return pref.getString(PREF_USER_NAME, null)
        }

        @JvmStatic
        fun setUserName(context: Context, name: String) {
            val editor = context.getSharedPreferences(AndroidKeyStoreIdentityHandler.SHARED_PREF_NAME, Context.MODE_PRIVATE).edit()
            editor.putString(PREF_USER_NAME, name)
            editor.apply()
        }
    }
}