package hr.rostanic20.gymbro.di

import android.content.Context
import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import hr.rostanic20.gymbro.db.AppDb

const val DATABASE_NAME = "gymbro.db"

object SqlDriverFactory {

    fun createAndroidSqlite(context: Context, dbName: String = DATABASE_NAME): SqlDriver =
        AndroidSqliteDriver(
            schema = AppDb.Schema.synchronous(),
            context = context,
            name = dbName,
        )
}
