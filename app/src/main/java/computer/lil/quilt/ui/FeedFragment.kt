package computer.lil.quilt.ui

import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager

import computer.lil.quilt.R
import computer.lil.quilt.adapter.FeedAdapter
import computer.lil.quilt.data.viewmodel.MessagesViewModel
import computer.lil.quilt.database.Message
import kotlinx.android.synthetic.main.fragment_feed.view.*

class FeedFragment : Fragment() {
    private lateinit var messagesViewModel: MessagesViewModel
    val adapter = FeedAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_feed, container, false)

        view.recycler_messages.layoutManager = LinearLayoutManager(context)
        view.recycler_messages.adapter = adapter

        messagesViewModel = ViewModelProviders.of(this).get(MessagesViewModel::class.java)
        messagesViewModel.getAllMessages().observe(this,
            Observer<List<Message>> {
                messages -> adapter.setMessages(messages)
            })
        return view
    }
}
