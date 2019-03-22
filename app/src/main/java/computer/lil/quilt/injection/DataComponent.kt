package computer.lil.quilt.injection

import android.app.Application
import computer.lil.quilt.MainActivity
import computer.lil.quilt.data.repo.MessagesRepository
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [DataModule::class])
interface DataComponent {
    fun inject(activity: MainActivity)
    fun inject(repo: MessagesRepository)

    @Component.Builder
    interface Builder {
        fun build(): DataComponent
        fun dataModule(dataModule: DataModule): Builder
    }
}