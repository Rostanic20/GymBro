package hr.rostanic20.gymbro.di

import android.util.Log
import app.cash.sqldelight.db.SqlDriver
import hr.rostanic20.gymbro.core.BackupScheduler
import hr.rostanic20.gymbro.core.BackupStore
import hr.rostanic20.gymbro.core.DateProvider
import hr.rostanic20.gymbro.core.DefaultDispatcherProvider
import hr.rostanic20.gymbro.core.DispatcherProvider
import hr.rostanic20.gymbro.core.HealthSource
import hr.rostanic20.gymbro.core.MealReminderScheduler
import hr.rostanic20.gymbro.core.PhotoStorage
import hr.rostanic20.gymbro.core.RestTimer
import hr.rostanic20.gymbro.core.SystemDateProvider
import hr.rostanic20.gymbro.core.WallClock
import hr.rostanic20.gymbro.data.backup.AndroidBackupStore
import hr.rostanic20.gymbro.data.backup.WorkManagerBackupScheduler
import hr.rostanic20.gymbro.data.health.HealthConnectSource
import hr.rostanic20.gymbro.data.local.AndroidPhotoStorage
import hr.rostanic20.gymbro.data.local.BackupSettingsLocalDataSource
import hr.rostanic20.gymbro.data.local.BackupSettingsLocalDataSourceImpl
import hr.rostanic20.gymbro.data.local.BodyLocalDataSource
import hr.rostanic20.gymbro.data.local.BodyLocalDataSourceImpl
import hr.rostanic20.gymbro.data.local.FoodLocalDataSource
import hr.rostanic20.gymbro.data.local.FoodLocalDataSourceImpl
import hr.rostanic20.gymbro.data.local.PhotoLocalDataSource
import hr.rostanic20.gymbro.data.local.PhotoLocalDataSourceImpl
import hr.rostanic20.gymbro.data.local.ProfileLocalDataSource
import hr.rostanic20.gymbro.data.local.ProfileLocalDataSourceImpl
import hr.rostanic20.gymbro.data.local.ProgramLocalDataSource
import hr.rostanic20.gymbro.data.local.ProgramLocalDataSourceImpl
import hr.rostanic20.gymbro.data.local.SessionLocalDataSource
import hr.rostanic20.gymbro.data.local.SessionLocalDataSourceImpl
import hr.rostanic20.gymbro.data.repository.BackupSettingsRepositoryImpl
import hr.rostanic20.gymbro.data.repository.BodyRepositoryImpl
import hr.rostanic20.gymbro.data.repository.FoodRepositoryImpl
import hr.rostanic20.gymbro.data.repository.PhotoRepositoryImpl
import hr.rostanic20.gymbro.data.repository.ProfileRepositoryImpl
import hr.rostanic20.gymbro.data.repository.ProgramRepositoryImpl
import hr.rostanic20.gymbro.data.repository.SessionRepositoryImpl
import hr.rostanic20.gymbro.db.AppDb
import hr.rostanic20.gymbro.domain.repository.BackupSettingsRepository
import hr.rostanic20.gymbro.domain.repository.BodyRepository
import hr.rostanic20.gymbro.domain.repository.FoodRepository
import hr.rostanic20.gymbro.domain.repository.PhotoRepository
import hr.rostanic20.gymbro.domain.repository.ProfileRepository
import hr.rostanic20.gymbro.domain.repository.ProgramRepository
import hr.rostanic20.gymbro.domain.repository.SessionRepository
import hr.rostanic20.gymbro.timer.AlarmMealReminderScheduler
import hr.rostanic20.gymbro.timer.AlarmRestTimer
import hr.rostanic20.gymbro.ui.body.BodyViewModel
import hr.rostanic20.gymbro.ui.foods.FoodEditViewModel
import hr.rostanic20.gymbro.ui.settings.BackupViewModel
import hr.rostanic20.gymbro.ui.foods.FoodsViewModel
import hr.rostanic20.gymbro.ui.meal.MealViewModel
import hr.rostanic20.gymbro.ui.session.SessionViewModel
import hr.rostanic20.gymbro.ui.settings.SettingsViewModel
import hr.rostanic20.gymbro.ui.today.HealthViewModel
import hr.rostanic20.gymbro.ui.today.TodayViewModel
import hr.rostanic20.gymbro.ui.train.TrainViewModel
import hr.rostanic20.gymbro.ui.workout.WorkoutViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
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
    single<CoroutineScope> {
        CoroutineScope(
            SupervisorJob() + get<DispatcherProvider>().io +
                CoroutineExceptionHandler { _, throwable -> Log.e("GymBro", "Background work failed", throwable) },
        )
    }
    single<RestTimer> { AlarmRestTimer(androidApplication()) }
    single<MealReminderScheduler> { AlarmMealReminderScheduler(androidApplication()) }
    single<PhotoStorage> { AndroidPhotoStorage(androidApplication(), get()) }
    single<HealthSource> { HealthConnectSource(androidApplication(), get()) }
    single<BackupStore> { AndroidBackupStore(androidApplication(), get(), get()) }
    single<BackupScheduler> { WorkManagerBackupScheduler(androidApplication()) }

    single { SqlDriverFactory.createAndroidSqlite(androidApplication()) } bind SqlDriver::class
    single { AppDb(get()) }

    singleOf(::ProgramLocalDataSourceImpl) bind ProgramLocalDataSource::class
    singleOf(::ProfileLocalDataSourceImpl) bind ProfileLocalDataSource::class
    singleOf(::SessionLocalDataSourceImpl) bind SessionLocalDataSource::class
    singleOf(::FoodLocalDataSourceImpl) bind FoodLocalDataSource::class
    singleOf(::BodyLocalDataSourceImpl) bind BodyLocalDataSource::class
    singleOf(::PhotoLocalDataSourceImpl) bind PhotoLocalDataSource::class
    singleOf(::BackupSettingsLocalDataSourceImpl) bind BackupSettingsLocalDataSource::class
    singleOf(::ProgramRepositoryImpl) bind ProgramRepository::class
    singleOf(::ProfileRepositoryImpl) bind ProfileRepository::class
    singleOf(::SessionRepositoryImpl) bind SessionRepository::class
    singleOf(::FoodRepositoryImpl) bind FoodRepository::class
    singleOf(::BodyRepositoryImpl) bind BodyRepository::class
    singleOf(::PhotoRepositoryImpl) bind PhotoRepository::class
    singleOf(::BackupSettingsRepositoryImpl) bind BackupSettingsRepository::class

    viewModelOf(::TodayViewModel)
    viewModelOf(::HealthViewModel)
    viewModelOf(::TrainViewModel)
    viewModelOf(::BodyViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::BackupViewModel)
    viewModelOf(::FoodsViewModel)
    viewModel { (dayId: Long) -> WorkoutViewModel(dayId, get(), get(), get(), get(), get()) }
    viewModel { (sessionId: Long) -> SessionViewModel(sessionId, get(), get(), get(), get(), get()) }
    viewModel { (epochDay: Long, slot: Int) -> MealViewModel(epochDay, slot, get(), get()) }
    viewModel { params -> FoodEditViewModel(params.getOrNull(), get()) }
}
