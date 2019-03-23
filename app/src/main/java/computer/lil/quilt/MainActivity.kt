package computer.lil.quilt

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentStatePagerAdapter
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.squareup.moshi.Moshi
import computer.lil.quilt.database.QuiltDatabase
import computer.lil.quilt.databinding.ActivityMainBinding
import computer.lil.quilt.identity.IdentityHandler
import computer.lil.quilt.injection.DaggerDataComponent
import computer.lil.quilt.injection.DataModule
import computer.lil.quilt.network.PeerConnection
import computer.lil.quilt.ui.fragment.BottomNavigationDrawerFragment
import computer.lil.quilt.ui.fragment.FeedFragment
import computer.lil.quilt.ui.fragment.MentionsFragment
import computer.lil.quilt.ui.fragment.ProfileFragment
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*
import okio.ByteString.Companion.decodeHex
import javax.inject.Inject

class MainActivity : AppCompatActivity() {
    var clientSubs = CompositeDisposable()

    @Inject lateinit var identityHandler: IdentityHandler
    @Inject lateinit var db: QuiltDatabase
    @Inject lateinit var moshi: Moshi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        DaggerDataComponent.builder().dataModule(DataModule(this)).build().inject(this)

        val connection = PeerConnection(
            identityHandler,
            "d4a1cb88a66f02f8db635ce26441cc5dac1b08420ceaac230839b755845a9ffb".decodeHex().toByteArray(),
            "676acdbbda229c2f4bcd83dc69a3a31042c4ee92266d09cabb699b0b3066b0de".decodeHex().toByteArray(),
            moshi
        )

        Log.d("current id", identityHandler.getIdentityString())

        setSupportActionBar(bottom_bar)
        pager.adapter = TabsAdapter(supportFragmentManager)

        fab.setOnClickListener {
            val intent = Intent(this, ComposeActivity::class.java)
            startActivity(intent)
        }


        /*btn_retry.setOnClickListener {
            clientSubs.add(connection.connectToPeer("10.0.2.2", 8008)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe {
                    Log.d("success", it.toString())
                    val requestQueue = RequestQueue(moshi)
                    if (it) {
                        clientSubs.add(
                            connection.listenToPeer()
                                .subscribeOn(Schedulers.io())
                                .observeOn(Schedulers.io())
                                .subscribeBy (
                                    onNext = {
                                        Log.d("server response", it.toString())
                                        if (it.requestNumber > 0) {
                                            requestQueue.add(it)
                                            requestQueue.processRequest(connection)
                                        } else {
                                            Log.d("not request", ByteString.copyFrom(it.body).toStringUtf8())
                                        }
                                    },
                                    onError = {
                                        it.printStackTrace()
                                    }
                                )
                        )
                    }
                })
        }*/
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            android.R.id.home -> {
                val bottomNavDrawerFragment = BottomNavigationDrawerFragment()
                bottomNavDrawerFragment.show(supportFragmentManager, BottomNavigationDrawerFragment.TAG)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onStop() {
        clientSubs.dispose()
        super.onStop()
    }
}

class TabsAdapter(fm: FragmentManager) : FragmentStatePagerAdapter(fm) {
    override fun getCount(): Int {
        return 3
    }

    override fun getItem(position: Int): Fragment {
        return when (position) {
            0 -> FeedFragment()
            1 -> MentionsFragment()
            2 -> ProfileFragment()
            else -> FeedFragment()
        }
    }
}
