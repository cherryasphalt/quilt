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
import com.squareup.moshi.Moshi
import computer.lil.quilt.databinding.ActivityMainBinding
import computer.lil.quilt.identity.IdentityHandler
import computer.lil.quilt.injection.DaggerDataComponent
import computer.lil.quilt.injection.DataModule
import computer.lil.quilt.ui.fragment.BottomNavigationDrawerFragment
import computer.lil.quilt.ui.fragment.FeedFragment
import computer.lil.quilt.ui.fragment.MentionsFragment
import computer.lil.quilt.ui.fragment.ProfileFragment
import io.reactivex.disposables.CompositeDisposable
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

class MainActivity : AppCompatActivity() {
    @Inject lateinit var identityHandler: IdentityHandler
    @Inject lateinit var moshi: Moshi

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = DataBindingUtil.setContentView<ActivityMainBinding>(this, R.layout.activity_main)
        DaggerDataComponent.builder().dataModule(DataModule(this)).build().inject(this)

        Log.d("current id", identityHandler.getIdentityString())

        setSupportActionBar(bottom_bar)
        pager.adapter = TabsAdapter(supportFragmentManager)

        fab.setOnClickListener {
            val intent = Intent(this, ComposeActivity::class.java)
            startActivity(intent)
        }
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
