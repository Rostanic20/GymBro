package hr.rostanic20.gymbro.data.local

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import hr.rostanic20.gymbro.core.DispatcherProvider
import hr.rostanic20.gymbro.data.FoodEntity
import hr.rostanic20.gymbro.data.FoodLogEntity
import hr.rostanic20.gymbro.data.RecipeRowEntity
import hr.rostanic20.gymbro.db.AppDb
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

data class FoodRow(
    val name: String,
    val kcalPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatPer100g: Double,
    val unitName: String?,
    val unitGrams: Double?,
)

data class FoodLogRow(
    val epochDay: Long,
    val mealSlot: Long,
    val foodId: Long?,
    val name: String,
    val grams: Double?,
    val kcal: Double,
    val proteinG: Double,
    val carbsG: Double,
    val fatG: Double,
    val loggedAt: Long,
)

interface FoodLocalDataSource {
    fun foods(): Flow<List<FoodEntity>>
    fun recipeRows(): Flow<List<RecipeRowEntity>>
    fun logForDate(epochDay: Long): Flow<List<FoodLogEntity>>
    suspend fun foodById(id: Long): FoodEntity?
    suspend fun logForMeal(epochDay: Long, mealSlot: Long): List<FoodLogEntity>
    suspend fun insertFood(row: FoodRow): Long
    suspend fun updateFood(id: Long, row: FoodRow)
    suspend fun setFavourite(id: Long, favourite: Boolean)
    suspend fun insertLogs(rows: List<FoodLogRow>)
    suspend fun deleteLog(id: Long)
}

class FoodLocalDataSourceImpl(
    private val db: AppDb,
    private val dispatchers: DispatcherProvider,
) : FoodLocalDataSource {

    private val query = db.foodQueries
    private val writeContext = dispatchers.io + NonCancellable

    override fun foods(): Flow<List<FoodEntity>> =
        query.selectAll().asFlow().map { it.awaitAsList() }

    override fun recipeRows(): Flow<List<RecipeRowEntity>> =
        query.selectRecipeItems().asFlow().map { it.awaitAsList() }

    override fun logForDate(epochDay: Long): Flow<List<FoodLogEntity>> =
        query.selectLogForDate(epochDay).asFlow().map { it.awaitAsList() }

    override suspend fun foodById(id: Long): FoodEntity? = withContext(dispatchers.io) {
        query.selectById(id).awaitAsOneOrNull()
    }

    override suspend fun logForMeal(epochDay: Long, mealSlot: Long): List<FoodLogEntity> =
        withContext(dispatchers.io) {
            query.selectLogForMeal(epochDay, mealSlot).awaitAsList()
        }

    override suspend fun insertFood(row: FoodRow): Long = withContext(writeContext) {
        db.transactionWithResult {
            query.insertFood(
                row.name,
                row.kcalPer100g,
                row.proteinPer100g,
                row.carbsPer100g,
                row.fatPer100g,
                row.unitName,
                row.unitGrams,
            )
            query.lastInsertRowId().awaitAsOne()
        }
    }

    override suspend fun updateFood(id: Long, row: FoodRow): Unit = withContext(writeContext) {
        query.updateFood(
            name = row.name,
            kcalPer100g = row.kcalPer100g,
            proteinPer100g = row.proteinPer100g,
            carbsPer100g = row.carbsPer100g,
            fatPer100g = row.fatPer100g,
            unitName = row.unitName,
            unitGrams = row.unitGrams,
            id = id,
        )
    }

    override suspend fun setFavourite(id: Long, favourite: Boolean): Unit = withContext(writeContext) {
        query.setFavourite(favourite = if (favourite) 1L else 0L, id = id)
    }

    override suspend fun insertLogs(rows: List<FoodLogRow>): Unit = withContext(writeContext) {
        db.transaction {
            rows.forEach { row ->
                query.insertLog(
                    row.epochDay,
                    row.mealSlot,
                    row.foodId,
                    row.name,
                    row.grams,
                    row.kcal,
                    row.proteinG,
                    row.carbsG,
                    row.fatG,
                    row.loggedAt,
                )
            }
        }
    }

    override suspend fun deleteLog(id: Long): Unit = withContext(writeContext) {
        query.deleteLog(id)
    }
}
