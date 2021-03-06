package computer.lil.quilt.ui.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import computer.lil.quilt.ConnectionsActivity
import computer.lil.quilt.R
import computer.lil.quilt.databinding.FragmentBottomNavigationDrawerBinding

class BottomNavigationDrawerFragment : BottomSheetDialogFragment() {
    companion object {
        const val TAG = "bottom_navigation_drawer_fragment"
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = DataBindingUtil.inflate<FragmentBottomNavigationDrawerBinding>(inflater, R.layout.fragment_bottom_navigation_drawer, container, false)

        binding.navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_connections -> {
                    startActivity(Intent(activity, ConnectionsActivity::class.java))
                }
            }
            dismiss()
            true
        }

        return binding.root
    }
}