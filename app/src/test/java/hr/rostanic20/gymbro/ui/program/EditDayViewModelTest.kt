package hr.rostanic20.gymbro.ui.program

import hr.rostanic20.gymbro.ui.UserMessage
import hr.rostanic20.gymbro.util.FakeProgramRepository
import hr.rostanic20.gymbro.util.MainDispatcherRule
import hr.rostanic20.gymbro.util.plannedExercise
import hr.rostanic20.gymbro.util.weekProgram
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class EditDayViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val legPress = plannedExercise(9, "Leg press").exercise
    private val program = FakeProgramRepository(weekProgram, exercises = listOf(legPress))
    private val viewModel by lazy { EditDayViewModel(dayId = 1, programRepository = program) }

    private fun TestScope.collectState() {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.state.collect {} }
    }

    @Test
    fun `saving sets and reps changes that exercise only`() = runTest {
        collectState()

        viewModel.savePrescription(position = 1, prescription = Prescription(4, 6..10))
        advanceUntilIdle()

        val day = program.editableDays().first().first { it.id == 1L }
        assertEquals(4, day.exercises.single().sets)
        assertEquals(6..10, day.exercises.single().reps)
        assertEquals(3, program.editableDays().first().first { it.id == 2L }.exercises.single().sets)
    }

    @Test
    fun `removing an exercise keeps it in the editor and drops it from the workout`() = runTest {
        collectState()

        viewModel.setHidden(position = 1, hidden = true)
        advanceUntilIdle()

        assertTrue(viewModel.state.value!!.day!!.exercises.single().isHidden)
        assertTrue(program.workoutDays().first().first { it.id == 1L }.exercises.isEmpty())

        viewModel.setHidden(position = 1, hidden = false)
        advanceUntilIdle()

        assertTrue(program.workoutDays().first().first { it.id == 1L }.exercises.isNotEmpty())
    }

    @Test
    fun `an exercise already in the day is not offered again`() = runTest {
        collectState()
        assertEquals(listOf(legPress.id), viewModel.state.value?.addable?.map { it.id })

        viewModel.addExercise(legPress.id)
        advanceUntilIdle()

        assertEquals(listOf("Barbell bench press", "Leg press"), viewModel.state.value?.day?.exercises?.map { it.exercise.name })
        assertTrue(viewModel.state.value!!.addable.isEmpty())
    }

    @Test
    fun `a failed edit is reported`() = runTest {
        program.failWrites = true

        viewModel.savePrescription(position = 1, prescription = Prescription(4, 6..10))

        assertEquals(UserMessage.SaveFailed, viewModel.messages.first())
    }
}
