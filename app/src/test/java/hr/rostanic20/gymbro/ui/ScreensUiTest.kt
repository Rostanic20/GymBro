package hr.rostanic20.gymbro.ui

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import hr.rostanic20.gymbro.domain.model.Alternative
import hr.rostanic20.gymbro.domain.nutritionTargets
import hr.rostanic20.gymbro.ui.today.TodayContent
import hr.rostanic20.gymbro.ui.today.TodayUiState
import hr.rostanic20.gymbro.ui.workout.ExerciseItem
import hr.rostanic20.gymbro.ui.workout.LoadSettings
import hr.rostanic20.gymbro.ui.workout.LoadSettingsDialog
import hr.rostanic20.gymbro.util.defaultProfile
import hr.rostanic20.gymbro.util.plannedExercise
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "en-rUS", application = Application::class)
class ScreensUiTest {

    @get:Rule
    val compose = createComposeRule()

    private val hackSquat = plannedExercise(id = 21, name = "Hack squat", startLoadKg = null).copy(
        alternatives = listOf(
            Alternative(plannedExercise(26, "Bulgarian split squat").exercise, "No cost. Weight is per hand, reps per leg."),
        ),
    )

    @Test
    fun exerciseDetailsStayHiddenUntilTapped() {
        compose.setContent { MaterialTheme { ExerciseItem(planned = hackSquat, onEditLoads = {}) } }

        compose.onNodeWithText("Start weight: find it in week one · steps of 2.5 kg").assertIsDisplayed()
        compose.onNodeWithText("Bulgarian split squat", substring = true).assertDoesNotExist()

        compose.onNodeWithText("Hack squat").performClick()

        compose.onNodeWithText("Bulgarian split squat", substring = true).assertIsDisplayed()
        compose.onNodeWithText("Edit weights").assertIsDisplayed()
    }

    @Test
    fun loadDialogRefusesAZeroStepAndSavesAValidOne() {
        var saved: LoadSettings? = null
        compose.setContent {
            MaterialTheme { LoadSettingsDialog(exercise = hackSquat.exercise, onDismiss = {}, onSave = { saved = it }) }
        }

        compose.onNodeWithText("2.5").performTextReplacement("0")
        compose.onNodeWithText("Save").assertIsNotEnabled()

        compose.onNodeWithText("0").performTextReplacement("5")
        compose.onNodeWithText("Save").assertIsEnabled().performClick()

        assertEquals(LoadSettings(startLoadKg = null, incrementKg = 5.0), saved)
    }

    @Test
    fun weekendStartOffersTheComingMonday() {
        val sunday = LocalDate.of(2026, 9, 20)
        var started = false
        compose.setContent {
            MaterialTheme {
                TodayContent(
                    state = TodayUiState(
                        date = sunday,
                        programStart = null,
                        week = null,
                        workout = null,
                        targets = defaultProfile.nutritionTargets(week = null),
                        suggestedStart = sunday.plusDays(1),
                    ),
                    onStartProgram = { started = true },
                    onOpenWorkout = {},
                )
            }
        }

        compose.onNodeWithText("September 21", substring = true).assertIsDisplayed().performClick()

        assertTrue(started)
    }
}
