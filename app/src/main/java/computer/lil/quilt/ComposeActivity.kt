package computer.lil.quilt

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProviders
import computer.lil.quilt.data.viewmodel.MessagesViewModel
import computer.lil.quilt.identity.IdentityHandler
import computer.lil.quilt.injection.DaggerDataComponent
import computer.lil.quilt.injection.DataModule
import kotlinx.android.synthetic.main.activity_compose.*
import javax.inject.Inject

class ComposeActivity : AppCompatActivity() {
    @Inject lateinit var identityHandler: IdentityHandler
    private lateinit var messagesViewModel: MessagesViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_compose)
        DaggerDataComponent.builder().dataModule(DataModule(this)).build().inject(this)

        messagesViewModel = ViewModelProviders.of(this).get(MessagesViewModel::class.java)
        setSupportActionBar(bottom_bar)

        fab.setOnClickListener {
            val text = edit_post.text.toString()
            messagesViewModel.submitNewPostMessage(text)
            finish()
        }
    }
}
