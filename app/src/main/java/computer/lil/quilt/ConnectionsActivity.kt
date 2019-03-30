package computer.lil.quilt

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.squareup.moshi.Moshi
import computer.lil.quilt.identity.IdentityHandler
import computer.lil.quilt.injection.DaggerDataComponent
import computer.lil.quilt.injection.DataModule
import computer.lil.quilt.network.SyncManager
import kotlinx.android.synthetic.main.activity_connections.*
import javax.inject.Inject

class ConnectionsActivity : AppCompatActivity() {
    @Inject lateinit var identityHandler: IdentityHandler
    @Inject lateinit var moshi: Moshi
    private lateinit var syncManager: SyncManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        DaggerDataComponent.builder().dataModule(DataModule(this)).build().inject(this)
        setContentView(R.layout.activity_connections)
        syncManager = SyncManager(this, identityHandler)
        btn_retry.setOnClickListener {
            syncManager.startSync(this, this)
        }
    }

    override fun onStop() {
        syncManager.cancelSync()
        super.onStop()
    }
}
