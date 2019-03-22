package computer.lil.quilt.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import computer.lil.quilt.R
import computer.lil.quilt.databinding.FragmentProfileBinding
import computer.lil.quilt.identity.AndroidKeyStoreIdentityHandler

class ProfileFragment: Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        val binding: FragmentProfileBinding? = DataBindingUtil.bind(view)
        val identityHandler = AndroidKeyStoreIdentityHandler.getInstance(context!!)

        binding?.identifier = identityHandler.getIdentifier()
        return view
    }
}