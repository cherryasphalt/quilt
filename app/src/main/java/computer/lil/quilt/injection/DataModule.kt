package computer.lil.quilt.injection

import android.content.Context
import com.goterl.lazycode.lazysodium.LazySodiumAndroid
import com.goterl.lazycode.lazysodium.SodiumAndroid
import com.squareup.moshi.Moshi
import computer.lil.quilt.database.QuiltDatabase
import computer.lil.quilt.identity.AndroidKeyStoreIdentityHandler
import computer.lil.quilt.identity.IdentityHandler
import computer.lil.quilt.api.Constants
import dagger.Module
import dagger.Provides
import java.nio.charset.StandardCharsets
import javax.inject.Singleton

@Module
class DataModule(private val context: Context) {
    @Provides @Singleton fun provideContext() = context

    @Provides @Singleton
    fun provideMoshi(): Moshi = Constants.getMoshiInstance()

    @Provides @Singleton
    fun provideLazySodium(): LazySodiumAndroid = LazySodiumAndroid(SodiumAndroid(), StandardCharsets.UTF_8)

    @Provides @Singleton
    fun provideIdentityHandler(context: Context): IdentityHandler = AndroidKeyStoreIdentityHandler.getInstance(context)

    @Provides @Singleton
    fun provideDatabase(context: Context): QuiltDatabase = QuiltDatabase.getInstance(context)

}