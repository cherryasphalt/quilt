package computer.lil.quilt.util

import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

class BindingHolder<T : ViewDataBinding>
/**
 * Creates a [BindingHolder] using the provided [View]. The resulting
 * [BindingHolder] will contain a binding if the [View] has implemented the
 * databinding layout.
 *
 * @param v Provided [View].
 */
    (
    /**
     * Returns the [View] this holder contains.
     *
     * @return The [View].
     */
    val view: View
) : RecyclerView.ViewHolder(view) {
    /**
     * Returns the [ViewDataBinding] associated with the [View] this holder contains.
     *
     * @return The [ViewDataBinding].
     */
    var binding: T? = null
        private set

    init {
        try {
            binding = DataBindingUtil.bind(view)
        } catch (e: IllegalArgumentException) {
            binding = null
        }

    }
}