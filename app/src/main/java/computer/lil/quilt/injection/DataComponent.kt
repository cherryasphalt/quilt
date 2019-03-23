package computer.lil.quilt.injection

import computer.lil.quilt.ComposeActivity
import computer.lil.quilt.MainActivity
import computer.lil.quilt.data.repo.MessageRepository
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [DataModule::class])
interface DataComponent {
    fun inject(activity: MainActivity)
    fun inject(repo: MessageRepository)
    fun inject(activity: ComposeActivity)

    @Component.Builder
    interface Builder {
        fun build(): DataComponent
        fun dataModule(dataModule: DataModule): Builder
    }
}