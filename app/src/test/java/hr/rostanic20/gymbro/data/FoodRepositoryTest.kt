package hr.rostanic20.gymbro.data

import app.cash.sqldelight.async.coroutines.synchronous
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import hr.rostanic20.gymbro.data.local.FoodLocalDataSourceImpl
import hr.rostanic20.gymbro.data.repository.FoodRepositoryImpl
import hr.rostanic20.gymbro.db.AppDb
import hr.rostanic20.gymbro.domain.model.CanteenPlate
import hr.rostanic20.gymbro.domain.model.FoodDraft
import hr.rostanic20.gymbro.domain.model.Meal
import hr.rostanic20.gymbro.domain.model.Nutrition
import hr.rostanic20.gymbro.domain.nutrition
import hr.rostanic20.gymbro.domain.total
import hr.rostanic20.gymbro.util.TestDispatcherProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FoodRepositoryTest {

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY).also {
        AppDb.Schema.synchronous().create(it)
    }
    private val db = AppDb(driver)
    private val monday = LocalDate.of(2026, 9, 14)

    @After
    fun tearDown() {
        driver.close()
    }

    private fun TestScope.repository(): FoodRepositoryImpl {
        val dispatchers = TestDispatcherProvider(StandardTestDispatcher(testScheduler))
        return FoodRepositoryImpl(FoodLocalDataSourceImpl(db, dispatchers), dispatchers)
    }

    @Test
    fun `seeded recipes match the program's shakes and meals`() = runTest {
        val recipes = repository().recipes().first().associateBy { it.name }

        assertEquals(7, recipes.size)
        assertEquals(520.0, recipes.getValue("Shake #1").nutrition.kcal, 10.0)
        assertEquals(710.0, recipes.getValue("Shake #2").nutrition.kcal, 15.0)
        assertEquals(4, recipes.getValue("Shake #1").items.size)
    }

    @Test
    fun `logging a recipe stores one entry per ingredient`() = runTest {
        val repository = repository()
        val shake = repository.recipes().first().single { it.name == "Shake #1" }

        repository.logRecipe(monday, Meal.BREAKFAST_SHAKE, shake, nowMillis = 1_000)

        val log = repository.log(monday).first()
        assertEquals(4, log.size)
        assertTrue(log.all { it.meal == Meal.BREAKFAST_SHAKE })
        assertEquals(shake.nutrition.kcal, log.total().kcal, 0.001)
    }

    @Test
    fun `editing a food later does not rewrite what was already logged`() = runTest {
        val repository = repository()
        val whey = repository.foods().first().single { it.name == "Whey protein" }
        repository.logFood(monday, Meal.POST_GYM_SHAKE, whey, grams = 25.0, nowMillis = 1_000)

        repository.saveFood(FoodDraft(whey.id, whey.name, Nutrition(360.0, 80.0, 6.0, 4.0), "scoop", 30.0))

        assertEquals(100.0, repository.log(monday).first().single().nutrition.kcal, 0.001)
        assertEquals(360.0, repository.food(whey.id)?.per100g?.kcal)
        assertEquals(30.0, repository.food(whey.id)?.unitGrams)
    }

    @Test
    fun `repeating yesterday copies only that meal`() = runTest {
        val repository = repository()
        val recipes = repository.recipes().first().associateBy { it.name }
        val sunday = monday.minusDays(1)
        repository.logRecipe(sunday, Meal.BREAKFAST_SHAKE, recipes.getValue("Shake #1"), nowMillis = 1_000)
        repository.logRecipe(sunday, Meal.DINNER, recipes.getValue("Dinner: eggs and toast"), nowMillis = 1_000)

        val copied = repository.repeatMeal(sunday, monday, Meal.BREAKFAST_SHAKE, nowMillis = 2_000)

        assertEquals(4, copied)
        assertEquals(setOf(Meal.BREAKFAST_SHAKE), repository.log(monday).first().map { it.meal }.toSet())
        assertEquals(0, repository.repeatMeal(sunday, monday, Meal.LUNCH, nowMillis = 2_000))
    }

    @Test
    fun `a canteen estimate is logged without a food`() = runTest {
        val repository = repository()

        repository.logEstimate(monday, Meal.LUNCH, "Pasta, little meat", CanteenPlate.PASTA_LITTLE_MEAT.nutrition, 1_000)

        val entry = repository.log(monday).first().single()
        assertNull(entry.foodId)
        assertNull(entry.grams)
        assertEquals(10.0, entry.nutrition.proteinG, 0.001)
    }

    @Test
    fun `new foods can be added and favourites sort first`() = runTest {
        val repository = repository()

        val id = repository.saveFood(FoodDraft(null, " Greek yogurt ", Nutrition(97.0, 9.0, 4.0, 5.0), "", null))
        repository.setFavourite(id, favourite = true)

        val first = repository.foods().first().first()
        assertEquals("Greek yogurt", first.name)
        assertTrue(first.isFavourite)
        assertNull(first.unitName)
    }

    @Test
    fun `a deleted entry is gone`() = runTest {
        val repository = repository()
        val oats = repository.foods().first().single { it.name == "Oats" }
        repository.logFood(monday, Meal.DESK_SNACK, oats, grams = 40.0, nowMillis = 1_000)

        repository.deleteEntry(repository.log(monday).first().single().id)

        assertTrue(repository.log(monday).first().isEmpty())
    }
}
