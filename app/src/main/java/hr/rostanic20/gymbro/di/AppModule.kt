package hr.rostanic20.gymbro.di

import app.cash.sqldelight.db.SqlDriver
import hr.rostanic20.gymbro.core.DateProvider
import hr.rostanic20.gymbro.core.DefaultDispatcherProvider
import hr.rostanic20.gymbro.core.DispatcherProvider
import hr.rostanic20.gymbro.core.SystemDateProvider
import hr.rostanic20.gymbro.data.local.ProfileLocalDataSource
import hr.rostanic20.gymbro.data.local.ProfileLocalDataSourceImpl
import hr.rostanic20.gymbro.data.local.ProgramLocalDataSource
import hr.rostanic20.gymbro.data.local.ProgramLocalDataSourceImpl
import hr.rostanic20.gymbro.data.repository.ProfileRepositoryImpl
import hr.rostanic20.gymbro.data.repository.ProgramRepositoryImpl
import hr.rostanic20.gymbro.db.AppDb
import hr.rostanic20.gymbro.domain.repository.ProfileRepository
import hr.rostanic20.gymbro.domain.repository.ProgramRepository
import hr.rostanic20.gymbro.navigation.AppNavController
import hr.rostanic20.gymbro.ui.today.TodayViewModel
import hr.rostanic20.gymbro.ui.train.TrainViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    single<DateProvider> { SystemDateProvider() }
    single { AppNavController() }

    single { SqlDriverFactory.createAndroidSqlite(androidApplication()) } bind SqlDriver::class
    single { AppDb(get()) }

    singleOf(::ProgramLocalDataSourceImpl) bind ProgramLocalDataSource::class
    singleOf(::ProfileLocalDataSourceImpl) bind ProfileLocalDataSource::class
    singleOf(::ProgramRepositoryImpl) bind ProgramRepository::class
    singleOf(::ProfileRepositoryImpl) bind ProfileRepository::class

    viewModelOf(::TodayViewModel)
    viewModelOf(::TrainViewModel)
}
