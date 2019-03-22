package computer.lil.quilt.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import computer.lil.quilt.R
import computer.lil.quilt.database.Message
import computer.lil.quilt.databinding.ItemMessageBinding
import computer.lil.quilt.util.BindingHolder

class FeedAdapter: RecyclerView.Adapter<BindingHolder<ItemMessageBinding>>() {
    private var messages: List<Message> = ArrayList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BindingHolder<ItemMessageBinding> {
        val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_message, parent, false)
        return BindingHolder(itemView)
    }

    override fun onBindViewHolder(holder: BindingHolder<ItemMessageBinding>, position: Int) {
        val message = messages[position]
        val binding = holder.binding as ItemMessageBinding
        binding.message = message
    }

    override fun getItemCount(): Int = messages.size

    fun setMessages(messages: List<Message>) {
        this.messages = messages
        notifyDataSetChanged()
    }
}