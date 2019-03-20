package computer.lil.quilt.injection

import android.app.Application
import computer.lil.quilt.MainActivity
import dagger.Component
import javax.inject.Singleton

@Singleton
@Component(modules = [DataModule::class])
interface DataComponent {
    fun inject(activity: MainActivity)

    @Component.Builder
    interface Builder {
        fun build(): DataComponent
        fun dataModule(dataModule: DataModule): Builder
    }
}