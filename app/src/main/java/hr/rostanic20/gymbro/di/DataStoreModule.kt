package hr.rostanic20.gymbro.di

import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.dataStoreFile
import hr.rostanic20.gymbro.data.local.UserProfile
import hr.rostanic20.gymbro.data.local.UserProfileSerializer
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

const val PROFILE_FILE = "user_profile.json"

val dataStoreModule = module {
    single {
        DataStoreFactory.create(
            serializer = UserProfileSerializer,
            corruptionHandler = ReplaceFileCorruptionHandler { UserProfile() },
            produceFile = { androidApplication().dataStoreFile(PROFILE_FILE) },
        )
    }
}
