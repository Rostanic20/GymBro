package hr.rostanic20.gymbro

import android.app.Application
import hr.rostanic20.gymbro.di.appModule
import hr.rostanic20.gymbro.di.dataStoreModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class GymBroApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@GymBroApplication)
            modules(dataStoreModule, appModule)
        }
    }
}
