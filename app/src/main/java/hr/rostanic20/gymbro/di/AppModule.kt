package hr.rostanic20.gymbro.di

import app.cash.sqldelight.db.SqlDriver
import hr.rostanic20.gymbro.core.DateProvider
import hr.rostanic20.gymbro.core.DefaultDispatcherProvider
import hr.rostanic20.gymbro.core.DispatcherProvider
import hr.rostanic20.gymbro.core.RestTimer
import hr.rostanic20.gymbro.core.SystemDateProvider
import hr.rostanic20.gymbro.core.WallClock
import hr.rostanic20.gymbro.data.local.ProfileLocalDataSource
import hr.rostanic20.gymbro.data.local.ProfileLocalDataSourceImpl
import hr.rostanic20.gymbro.data.local.ProgramLocalDataSource
import hr.rostanic20.gymbro.data.local.ProgramLocalDataSourceImpl
import hr.rostanic20.gymbro.data.local.SessionLocalDataSource
import hr.rostanic20.gymbro.data.local.SessionLocalDataSourceImpl
import hr.rostanic20.gymbro.data.repository.ProfileRepositoryImpl
import hr.rostanic20.gymbro.data.repository.ProgramRepositoryImpl
import hr.rostanic20.gymbro.data.repository.SessionRepositoryImpl
import hr.rostanic20.gymbro.db.AppDb
import hr.rostanic20.gymbro.domain.repository.ProfileRepository
import hr.rostanic20.gymbro.domain.repository.ProgramRepository
import hr.rostanic20.gymbro.domain.repository.SessionRepository
import hr.rostanic20.gymbro.timer.AlarmRestTimer
import hr.rostanic20.gymbro.ui.session.SessionViewModel
import hr.rostanic20.gymbro.ui.settings.SettingsViewModel
import hr.rostanic20.gymbro.ui.today.TodayViewModel
import hr.rostanic20.gymbro.ui.train.TrainViewModel
import hr.rostanic20.gymbro.ui.workout.WorkoutViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val appModule = module {
    single<DispatcherProvider> { DefaultDispatcherProvider() }
    single<DateProvider> { SystemDateProvider() }
    single { WallClock { System.currentTimeMillis() } }
    single<RestTimer> { AlarmRestTimer(androidApplication()) }

    single { SqlDriverFactory.createAndroidSqlite(androidApplication()) } bind SqlDriver::class
    single { AppDb(get()) }

    singleOf(::ProgramLocalDataSourceImpl) bind ProgramLocalDataSource::class
    singleOf(::ProfileLocalDataSourceImpl) bind ProfileLocalDataSource::class
    singleOf(::SessionLocalDataSourceImpl) bind SessionLocalDataSource::class
    singleOf(::ProgramRepositoryImpl) bind ProgramRepository::class
    singleOf(::ProfileRepositoryImpl) bind ProfileRepository::class
    singleOf(::SessionRepositoryImpl) bind SessionRepository::class

    viewModelOf(::TodayViewModel)
    viewModelOf(::TrainViewModel)
    viewModelOf(::SettingsViewModel)
    viewModel { (dayId: Long) -> WorkoutViewModel(dayId, get(), get(), get(), get(), get()) }
    viewModel { (sessionId: Long) -> SessionViewModel(sessionId, get(), get(), get(), get(), get()) }
}
